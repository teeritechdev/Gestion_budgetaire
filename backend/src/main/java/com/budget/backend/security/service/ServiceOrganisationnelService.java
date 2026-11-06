package com.budget.backend.security.service;

import com.budget.backend.common.PdfUtils;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.ServiceRequestDto;
import com.budget.backend.security.dto.ServiceResponseDto;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.AffectationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceOrganisationnelService {
    private final ServiceOrganisationnelRepository serviceRepository;
    private final DirectionRepository directionRepository;
    private final AffectationRepository affectationRepository;

    public ServiceOrganisationnelService(
            ServiceOrganisationnelRepository serviceRepository,
            DirectionRepository directionRepository,
            AffectationRepository affectationRepository) {
        this.serviceRepository = serviceRepository;
        this.directionRepository = directionRepository;
        this.affectationRepository = affectationRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponseDto> lister(UUID directionId, String structureCode, Boolean actif, String recherche) {
        return serviceRepository.findAll().stream()
                .filter(service -> directionId == null || service.getDirection().getId().equals(directionId))
                .filter(service -> structureCode == null || service.getDirection().estRattacheeA(structureCode))
                .filter(service -> actif == null || service.getActif().equals(actif))
                .filter(service -> recherche == null
                        || service.getLibelle().toLowerCase().contains(recherche.toLowerCase())
                        || service.getCode().toLowerCase().contains(recherche.toLowerCase()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponseDto obtenir(UUID id) {
        return toDto(trouverService(id));
    }

    @Transactional
    public ServiceResponseDto creer(ServiceRequestDto request) {
        if (serviceRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un service avec ce code existe déjà.");
        }
        Direction direction = trouverDirection(request.directionId());
        ServiceOrganisationnel service = ServiceOrganisationnel.builder()
                .code(request.code().trim())
                .libelle(request.libelle().trim())
                .direction(direction)
                .actif(request.actif())
                .build();
        return toDto(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponseDto modifier(UUID id, ServiceRequestDto request) {
        ServiceOrganisationnel service = trouverService(id);
        if (!service.getCode().equals(request.code()) && serviceRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un service avec ce code existe déjà.");
        }
        service.setCode(request.code().trim());
        service.setLibelle(request.libelle().trim());
        service.setDirection(trouverDirection(request.directionId()));
        service.setActif(request.actif());
        return toDto(serviceRepository.save(service));
    }

    @Transactional
    public void supprimer(UUID id) {
        if (affectationRepository.existsByService_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce service ne peut pas être supprimé tant que des salariés y sont affectés.");
        }
        serviceRepository.delete(trouverService(id));
    }

    private Direction trouverDirection(UUID id) {
        return directionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Direction introuvable."));
    }

    private ServiceOrganisationnel trouverService(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable."));
    }

    private ServiceResponseDto toDto(ServiceOrganisationnel service) {
        Direction direction = service.getDirection();
        return new ServiceResponseDto(
                service.getId(),
                service.getCode(),
                service.getLibelle(),
                direction.getId(),
                direction.getCode(),
                direction.getLibelle(),
                direction.codeStructure(),
                service.getResponsableId(),
                service.getActif(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }

    // --- Exports (liste fournie : lignes affichées à l'écran) ---
    public String exporterEnCsv(List<ServiceResponseDto> services) {
        StringBuilder csv = new StringBuilder("Code;Libelle;Code_Direction;Direction;Code_Structure;Actif;Date_Creation\n");
        for (ServiceResponseDto s : services) {
            csv.append(CsvUtils.ligne(';', s.code(), s.libelle(), s.directionCode(), s.directionLibelle(),
                    s.structureCode(), Boolean.TRUE.equals(s.actif()) ? "oui" : "non", s.createdAt()));
        }
        return csv.toString();
    }

    public byte[] exporterEnPdf(List<ServiceResponseDto> services) {
        return PdfUtils.tableau("Liste des services",
                List.of("Code", "Libellé", "Direction", "Structure", "État"),
                services.stream().map(s -> java.util.Arrays.<Object>asList(s.code(), s.libelle(),
                        s.directionLibelle() + " (" + s.directionCode() + ")", s.structureCode(),
                        Boolean.TRUE.equals(s.actif()) ? "Actif" : "Inactif")).toList());
    }
}
