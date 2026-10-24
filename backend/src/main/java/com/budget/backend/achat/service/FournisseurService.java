package com.budget.backend.achat.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.dto.FournisseurRequestDto;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FournisseurService {

    @Autowired
    private FournisseurRepository repository;

    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private AuditHelper auditHelper;
    @Autowired
    private CurrentUserService currentUserService;

    public PageResponseDto<FournisseurResponseDto> filtrer(String raisonSociale, String ifu, Boolean actif, String query, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, q, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (raisonSociale != null && !raisonSociale.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("raisonSociale")), "%" + raisonSociale.toLowerCase() + "%"));
            }
            if (ifu != null && !ifu.isEmpty()) {
                predicates.add(cb.equal(root.get("ifu"), ifu));
            }
            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            }
            if (query != null && !query.isEmpty()) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("raisonSociale")), like),
                        cb.like(cb.lower(root.get("codeFournisseur")), like),
                        cb.like(cb.lower(root.get("ifu")), like)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), FournisseurResponseDto::from);
    }

    public Optional<FournisseurResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(FournisseurResponseDto::from);
    }

    @Transactional
    public FournisseurResponseDto creer(FournisseurRequestDto dto) {
        if (repository.existsByIfu(dto.getIfu())) {
            throw new AchatException("Un fournisseur avec cet IFU existe déjà : " + dto.getIfu());
        }
        String code = dto.getCodeFournisseur();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeFournisseur();
        } else if (repository.existsByCodeFournisseur(code)) {
            throw new AchatException("Un fournisseur avec ce code existe déjà : " + code);
        }
        Fournisseur f = Fournisseur.builder()
                .codeFournisseur(code)
                .raisonSociale(dto.getRaisonSociale())
                .ifu(dto.getIfu())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
        f = repository.saveAndFlush(f);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.INSERTION, "fournisseurs", f.getId(), null,
                AuditHelper.valeurs("codeFournisseur", f.getCodeFournisseur(), "raisonSociale", f.getRaisonSociale(),
                        "ifu", f.getIfu(), "actif", f.getActif()));
        return FournisseurResponseDto.from(f);
    }

    @Transactional
    public FournisseurResponseDto modifier(UUID id, FournisseurRequestDto dto) {
        Fournisseur f = repository.findById(id)
                .orElseThrow(() -> new AchatException("Fournisseur introuvable avec l'ID : " + id));
        if (!f.getIfu().equals(dto.getIfu()) && repository.existsByIfu(dto.getIfu())) {
            throw new AchatException("Un fournisseur avec cet IFU existe déjà : " + dto.getIfu());
        }
        java.util.Map<String, Object> avant = AuditHelper.valeurs("raisonSociale", f.getRaisonSociale(), "ifu", f.getIfu(),
                "email", f.getEmail(), "actif", f.getActif());
        if (dto.getCodeFournisseur() != null && !dto.getCodeFournisseur().isBlank()
                && !f.getCodeFournisseur().equals(dto.getCodeFournisseur())) {
            if (repository.existsByCodeFournisseur(dto.getCodeFournisseur())) {
                throw new AchatException("Un fournisseur avec ce code existe déjà : " + dto.getCodeFournisseur());
            }
            f.setCodeFournisseur(dto.getCodeFournisseur());
        }
        f.setRaisonSociale(dto.getRaisonSociale());
        f.setIfu(dto.getIfu());
        f.setEmail(dto.getEmail());
        f.setTelephone(dto.getTelephone());
        f.setAdresse(dto.getAdresse());
        if (dto.getActif() != null) {
            f.setActif(dto.getActif());
        }
        f = repository.saveAndFlush(f);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "fournisseurs", f.getId(), avant,
                AuditHelper.valeurs("raisonSociale", f.getRaisonSociale(), "ifu", f.getIfu(),
                        "email", f.getEmail(), "actif", f.getActif()));
        return FournisseurResponseDto.from(f);
    }
}
