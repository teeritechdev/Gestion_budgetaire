package com.budget.backend.security.service;

import java.util.Map;
import com.budget.backend.common.PdfUtils;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.SalarieRequestDto;
import com.budget.backend.security.dto.SalarieResponseDto;
import com.budget.backend.security.dto.ServiceResponseDto;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SalarieService {
    private final SalarieRepository salarieRepository;
    private final ServiceOrganisationnelRepository serviceRepository;
    private final UtilisateurRepository utilisateurRepository;

    public SalarieService(
            SalarieRepository salarieRepository,
            ServiceOrganisationnelRepository serviceRepository,
            UtilisateurRepository utilisateurRepository) {
        this.salarieRepository = salarieRepository;
        this.serviceRepository = serviceRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public List<SalarieResponseDto> lister(UUID serviceId, Boolean actif, String recherche) {
        String terme = recherche == null ? null : recherche.toLowerCase();
        return salarieRepository.findAll().stream()
                .filter(salarie -> serviceId == null || salarie.getService().getId().equals(serviceId))
                .filter(salarie -> actif == null || salarie.getActif().equals(actif))
                .filter(salarie -> terme == null
                        || salarie.getMatricule().toLowerCase().contains(terme)
                        || salarie.getNom().toLowerCase().contains(terme)
                        || salarie.getPrenom().toLowerCase().contains(terme)
                        || salarie.getEmail().toLowerCase().contains(terme))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalarieResponseDto obtenir(UUID id) {
        return toDto(trouverSalarie(id));
    }

    @Transactional
    public SalarieResponseDto creer(SalarieRequestDto request) {
        verifierUnicite(request, null);
        Salarie salarie = new Salarie();
        appliquer(salarie, request);
        return toDto(salarieRepository.save(salarie));
    }

    @Transactional
    public SalarieResponseDto modifier(UUID id, SalarieRequestDto request) {
        Salarie salarie = trouverSalarie(id);
        verifierUnicite(request, id);
        appliquer(salarie, request);
        return toDto(salarieRepository.save(salarie));
    }

    @Transactional
    public void supprimer(UUID id) {
        Salarie salarie = trouverSalarie(id);
        if (utilisateurRepository.existsBySalarie_Id(id) || salarieRepository.existsByManager_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce salarié ne peut pas être supprimé tant qu'un compte utilisateur ou un collaborateur y est rattaché.");
        }
        salarieRepository.delete(salarie);
    }

    private void verifierUnicite(SalarieRequestDto request, UUID id) {
        boolean matriculeExiste = id == null
                ? salarieRepository.existsByMatricule(request.matricule().trim())
                : salarieRepository.existsByMatriculeAndIdNot(request.matricule().trim(), id);
        boolean emailExiste = id == null
                ? salarieRepository.existsByEmail(request.email().trim().toLowerCase())
                : salarieRepository.existsByEmailAndIdNot(request.email().trim().toLowerCase(), id);
        if (matriculeExiste || emailExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un salarié avec ce matricule ou cette adresse e-mail existe déjà.");
        }
    }

    private void appliquer(Salarie salarie, SalarieRequestDto request) {
        if (request.managerId() != null && request.managerId().equals(salarie.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un salarié ne peut pas être son propre manager.");
        }
        salarie.setMatricule(request.matricule().trim());
        salarie.setNom(request.nom().trim());
        salarie.setPrenom(request.prenom().trim());
        salarie.setSexe(request.sexe());
        salarie.setTelephone(request.telephone());
        salarie.setEmail(request.email().trim().toLowerCase());
        salarie.setPoste(request.poste());
        salarie.setDateEmbauche(request.dateEmbauche());
        salarie.setService(serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable.")));
        salarie.setManager(request.managerId() == null ? null : trouverSalarie(request.managerId()));
        salarie.setActif(request.actif());
    }

    private Salarie trouverSalarie(UUID id) {
        return salarieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarié introuvable."));
    }

    private SalarieResponseDto toDto(Salarie salarie) {
        return new SalarieResponseDto(salarie.getId(), salarie.getMatricule(), salarie.getNom(),
                salarie.getPrenom(), salarie.getSexe(), salarie.getTelephone(), salarie.getEmail(),
                salarie.getPoste(), salarie.getDateEmbauche(), toServiceDto(salarie.getService()),
                salarie.getManager() == null ? null : salarie.getManager().getId(),
                salarie.getActif(), salarie.getCreatedAt(), salarie.getUpdatedAt());
    }

    private ServiceResponseDto toServiceDto(ServiceOrganisationnel service) {
        return new ServiceResponseDto(service.getId(), service.getCode(), service.getLibelle(),
                service.getDirection().getId(), service.getDirection().getCode(),
                service.getDirection().getLibelle(), service.getDirection().codeStructure(),
                service.getResponsableId(), service.getActif(), service.getCreatedAt(), service.getUpdatedAt());
    }

    // --- Exports (liste fournie : lignes affichées à l'écran) ---
    public String exporterEnCsv(List<SalarieResponseDto> salaries) {
        Map<UUID, String> matricules = matriculesParId();
        StringBuilder csv = new StringBuilder(
                "Matricule;Nom;Prenom;Sexe;Telephone;Email;Poste;Date_Embauche;Code_Service;Matricule_Manager;Actif\n");
        for (SalarieResponseDto s : salaries) {
            csv.append(CsvUtils.ligne(';', s.matricule(), s.nom(), s.prenom(), s.sexe(), s.telephone(), s.email(),
                    s.poste(), s.dateEmbauche(), s.service() == null ? null : s.service().code(),
                    s.managerId() == null ? null : matricules.get(s.managerId()),
                    Boolean.TRUE.equals(s.actif()) ? "oui" : "non"));
        }
        return csv.toString();
    }

    public byte[] exporterEnPdf(List<SalarieResponseDto> salaries) {
        Map<UUID, String> matricules = matriculesParId();
        return PdfUtils.tableau("Liste des salariés",
                List.of("Matricule", "Nom et prénom", "E-mail", "Poste", "Service", "Embauche", "Manager", "Statut"),
                salaries.stream().map(s -> java.util.Arrays.<Object>asList(s.matricule(), s.nom() + " " + s.prenom(),
                        s.email(), s.poste(), s.service() == null ? "" : s.service().libelle(), s.dateEmbauche(),
                        s.managerId() == null ? "" : matricules.getOrDefault(s.managerId(), ""),
                        Boolean.TRUE.equals(s.actif()) ? "Actif" : "Inactif")).toList());
    }

    private Map<UUID, String> matriculesParId() {
        Map<UUID, String> matricules = new java.util.HashMap<>();
        salarieRepository.findAll().forEach(salarie -> matricules.put(salarie.getId(), salarie.getMatricule()));
        return matricules;
    }
}
