package com.budget.backend.security.service;

import com.budget.backend.common.PdfUtils;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.DirectionRequestDto;
import com.budget.backend.security.dto.DirectionResponseDto;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DirectionService {
    private final DirectionRepository directionRepository;
    private final StructureRepository structureRepository;
    private final ServiceOrganisationnelRepository serviceRepository;
    private final AffectationRepository affectationRepository;

    public DirectionService(
            DirectionRepository directionRepository,
            StructureRepository structureRepository,
            ServiceOrganisationnelRepository serviceRepository,
            AffectationRepository affectationRepository) {
        this.directionRepository = directionRepository;
        this.structureRepository = structureRepository;
        this.serviceRepository = serviceRepository;
        this.affectationRepository = affectationRepository;
    }

    @Transactional(readOnly = true)
    public List<DirectionResponseDto> lister(String structureCode, Boolean actif, String recherche) {
        return directionRepository.findAll().stream()
                .filter(direction -> structureCode == null || direction.estRattacheeA(structureCode))
                .filter(direction -> actif == null || direction.getActif().equals(actif))
                .filter(direction -> recherche == null
                        || direction.getLibelle().toLowerCase().contains(recherche.toLowerCase())
                        || direction.getCode().toLowerCase().contains(recherche.toLowerCase()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DirectionResponseDto obtenir(UUID id) {
        return toDto(trouverDirection(id));
    }

    @Transactional
    public DirectionResponseDto creer(DirectionRequestDto request) {
        if (directionRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une direction avec ce code existe déjà.");
        }
        Direction direction = Direction.builder()
                .code(request.code().trim())
                .libelle(request.libelle().trim())
                .structure(structureDeRattachement(request))
                .actif(request.actif())
                .build();
        return toDto(directionRepository.save(direction));
    }

    @Transactional
    public DirectionResponseDto modifier(UUID id, DirectionRequestDto request) {
        Direction direction = trouverDirection(id);
        if (!direction.getCode().equals(request.code()) && directionRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une direction avec ce code existe déjà.");
        }
        Structure structure = structureDeRattachement(request);

        direction.setCode(request.code().trim());
        direction.setLibelle(request.libelle().trim());
        direction.setStructure(structure);
        direction.setActif(request.actif());
        return toDto(directionRepository.save(direction));
    }

    @Transactional
    public void supprimer(UUID id) {
        Direction direction = trouverDirection(id);
        if (serviceRepository.existsByDirection_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette direction ne peut pas être supprimée tant que des services y sont rattachés.");
        }
        if (affectationRepository.existsByDirection_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette direction ne peut pas être supprimée tant que des salariés y sont affectés.");
        }
        directionRepository.delete(direction);
    }

    private Direction trouverDirection(UUID id) {
        return directionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Direction introuvable."));
    }

    /** Une direction est rattachée à une seule structure, qui doit exister. */
    private Structure structureDeRattachement(DirectionRequestDto request) {
        String code = request.structureCode() == null ? "" : request.structureCode().trim();
        if (code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rattachez la direction à une structure.");
        }
        return structureRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Structure introuvable : " + code + "."));
    }

    private DirectionResponseDto toDto(Direction direction) {
        Structure structure = direction.getStructure();
        return new DirectionResponseDto(
                direction.getId(),
                direction.getCode(),
                direction.getLibelle(),
                structure == null ? null : structure.getCode(),
                structure == null ? null : structure.getNom(),
                direction.getResponsableId(),
                direction.getActif(),
                direction.getCreatedAt(),
                direction.getUpdatedAt());
    }

    // --- Exports (liste fournie : lignes affichées à l'écran) ---
    public String exporterEnCsv(List<DirectionResponseDto> directions) {
        StringBuilder csv = new StringBuilder("Code;Libelle;Code_Structure;Structure;Actif;Date_Creation\n");
        for (DirectionResponseDto d : directions) {
            csv.append(CsvUtils.ligne(';', d.code(), d.libelle(), d.structureCode(), d.structureNom(),
                    Boolean.TRUE.equals(d.actif()) ? "oui" : "non", d.createdAt()));
        }
        return csv.toString();
    }

    public byte[] exporterEnPdf(List<DirectionResponseDto> directions) {
        return PdfUtils.tableau("Liste des directions",
                List.of("Code", "Libellé", "Structure", "État"),
                directions.stream().map(d -> java.util.Arrays.<Object>asList(d.code(), d.libelle(),
                        d.structureNom() + " (" + d.structureCode() + ")",
                        Boolean.TRUE.equals(d.actif()) ? "Active" : "Inactive")).toList());
    }
}
