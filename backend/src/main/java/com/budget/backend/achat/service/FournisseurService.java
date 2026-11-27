package com.budget.backend.achat.service;

import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.dto.FournisseurRequestDto;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FournisseurService {

    @Autowired
    private FournisseurRepository repository;

    @Autowired
    private NumeroDocumentService numeroDocumentService;

    @Transactional(readOnly = true)
    public PageResponseDto<FournisseurResponseDto> filtrer(String recherche, Boolean actif, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("raisonSociale")), term),
                        cb.like(cb.lower(root.get("codeFournisseur")), term),
                        cb.like(cb.lower(root.get("ifu")), term)
                ));
            }
            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), FournisseurResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<FournisseurResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(FournisseurResponseDto::from);
    }

    @Transactional
    public FournisseurResponseDto creer(FournisseurRequestDto dto) {
        String code = dto.getCodeFournisseur();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeFournisseur();
        } else if (repository.existsByCodeFournisseur(code)) {
            throw new AchatException("Un fournisseur avec le code " + code + " existe déjà.");
        }

        if (repository.existsByIfu(dto.getIfu().trim())) {
            throw new AchatException("Un fournisseur avec l'IFU " + dto.getIfu() + " existe déjà.");
        }

        Fournisseur fournisseur = Fournisseur.builder()
                .codeFournisseur(code)
                .raisonSociale(dto.getRaisonSociale().trim())
                .ifu(dto.getIfu().trim())
                .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                .telephone(dto.getTelephone() != null ? dto.getTelephone().trim() : null)
                .adresse(dto.getAdresse())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();

        fournisseur = repository.save(fournisseur);
        return FournisseurResponseDto.from(fournisseur);
    }

    @Transactional
    public FournisseurResponseDto modifier(UUID id, FournisseurRequestDto dto) {
        Fournisseur f = repository.findById(id)
                .orElseThrow(() -> new AchatException("Fournisseur introuvable avec l'ID : " + id));

        if (!f.getIfu().equalsIgnoreCase(dto.getIfu().trim()) && repository.existsByIfu(dto.getIfu().trim())) {
            throw new AchatException("Un autre fournisseur utilise déjà l'IFU : " + dto.getIfu());
        }

        if (dto.getCodeFournisseur() != null && !dto.getCodeFournisseur().isBlank()
                && !f.getCodeFournisseur().equalsIgnoreCase(dto.getCodeFournisseur().trim())
                && repository.existsByCodeFournisseur(dto.getCodeFournisseur().trim())) {
            throw new AchatException("Un autre fournisseur utilise déjà le code : " + dto.getCodeFournisseur());
        }

        if (dto.getCodeFournisseur() != null && !dto.getCodeFournisseur().isBlank()) {
            f.setCodeFournisseur(dto.getCodeFournisseur().trim());
        }
        f.setRaisonSociale(dto.getRaisonSociale().trim());
        f.setIfu(dto.getIfu().trim());
        f.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        f.setTelephone(dto.getTelephone() != null ? dto.getTelephone().trim() : null);
        f.setAdresse(dto.getAdresse());
        if (dto.getActif() != null) {
            f.setActif(dto.getActif());
        }

        f = repository.save(f);
        return FournisseurResponseDto.from(f);
    }

    @Transactional(readOnly = true)
    public List<Fournisseur> listerTous(List<UUID> ids) {
        if (ids != null && !ids.isEmpty()) {
            return repository.findAllById(ids);
        }
        return repository.findAll(Sort.by(Sort.Direction.ASC, "raisonSociale"));
    }

    @Transactional(readOnly = true)
    public String exporterEnCsv(List<Fournisseur> fournisseurs) {
        StringBuilder csv = new StringBuilder("Code_Fournisseur;Raison_Sociale;IFU;Email;Telephone;Adresse;Actif\n");
        for (Fournisseur f : fournisseurs) {
            csv.append(CsvUtils.ligne(';',
                    f.getCodeFournisseur(),
                    f.getRaisonSociale(),
                    f.getIfu(),
                    f.getEmail() != null ? f.getEmail() : "",
                    f.getTelephone() != null ? f.getTelephone() : "",
                    f.getAdresse() != null ? f.getAdresse() : "",
                    Boolean.TRUE.equals(f.getActif()) ? "oui" : "non"));
        }
        return csv.toString();
    }
}

