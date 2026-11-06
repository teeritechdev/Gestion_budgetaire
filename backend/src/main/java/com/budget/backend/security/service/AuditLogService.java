package com.budget.backend.security.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.AuditLogDetailDto;
import com.budget.backend.security.entity.AuditLog;
import com.budget.backend.security.repository.AuditLogRepository;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // --- Lecture ---
    /** Du plus récent au plus ancien. */
    private static final Comparator<AuditLog> PLUS_RECENT_D_ABORD = Comparator.comparing(
            AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    public List<AuditLog> listerTousLesLogs() {
        return auditLogRepository.findAll().stream().sorted(PLUS_RECENT_D_ABORD).toList();
    }

    public Optional<AuditLog> trouverParId(UUID id) {
        return auditLogRepository.findById(id);
    }

    /** Vue détaillée (même lecture des valeurs que le PDF). */
    @Transactional(readOnly = true)
    public Optional<AuditLogDetailDto> detail(UUID id) {
        return auditLogRepository.findById(id).map(log -> {
            Map<UUID, AuditLogPdfGenerator.Auteur> auteurs = auteurs(List.of(log));
            AuditLogPdfGenerator.Auteur auteur = log.getUtilisateurId() == null ? null : auteurs.get(log.getUtilisateurId());
            Map<String, String> avant = AuditLogPdfGenerator.valeursLisibles(log.getAnciennesValeurs());
            Map<String, String> apres = AuditLogPdfGenerator.valeursLisibles(log.getNouvellesValeurs());
            Set<String> noms = new LinkedHashSet<>(avant.keySet());
            noms.addAll(apres.keySet());
            List<AuditLogDetailDto.Champ> champs = noms.stream().map(nom -> {
                String ancienne = avant.get(nom);
                String nouvelle = apres.get(nom);
                return new AuditLogDetailDto.Champ(nom, ancienne, nouvelle,
                        ancienne != null && nouvelle != null && !ancienne.equals(nouvelle));
            }).toList();
            return new AuditLogDetailDto(log.getId(), log.getCreatedAt(), log.getTypeAction(), log.getNomTable(),
                    log.getEnregistrementId(), log.getUtilisateurId(),
                    AuditLogPdfGenerator.nomAuteur(log, auteurs),
                    auteur == null ? null : auteur.username(),
                    auteur == null ? null : auteur.matricule(),
                    auteur == null ? null : auteur.role(),
                    log.getAdresseIp(), log.getUserAgent(),
                    AuditLogPdfGenerator.navigateur(log.getUserAgent()),
                    AuditLogPdfGenerator.requete(log.getNouvellesValeurs(), log.getAnciennesValeurs()),
                    champs, log.getAnciennesValeurs(), log.getNouvellesValeurs());
        });
    }

    // --- Filtrage multicritères avancé ---
    public List<AuditLog> filtrerAuditLogs(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findAll().stream()
                .filter(log -> utilisateurId == null || (log.getUtilisateurId() != null && log.getUtilisateurId().equals(utilisateurId)))
                .filter(log -> typeAction == null || log.getTypeAction().equalsIgnoreCase(typeAction))
                .filter(log -> nomTable == null || log.getNomTable().equalsIgnoreCase(nomTable))
                .filter(log -> startDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(startDate)))
                .filter(log -> endDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(endDate)))
                .sorted(PLUS_RECENT_D_ABORD)
                .collect(Collectors.toList());
    }

    // --- Export CSV ---
    public String exporterEnCsv(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        return exporterEnCsv(filtrerAuditLogs(utilisateurId, typeAction, nomTable, startDate, endDate));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<AuditLog> logs) {
        StringBuilder csv = new StringBuilder("ID;UtilisateurID;TypeAction;NomTable;EnregistrementID;AdresseIP;Date\n");
        for (AuditLog log : logs) {
            csv.append(CsvUtils.ligne(';', log.getId(), log.getUtilisateurId() != null ? log.getUtilisateurId() : "SYSTEM",
                    log.getTypeAction(), log.getNomTable(), log.getEnregistrementId(), log.getAdresseIp(),
                    log.getCreatedAt()));
        }
        return csv.toString();
    }

    // --- Export PDF ---
    public byte[] exporterEnPdf(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        return exporterEnPdf(filtrerAuditLogs(utilisateurId, typeAction, nomTable, startDate, endDate));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    @Transactional(readOnly = true)
    public byte[] exporterEnPdf(List<AuditLog> logs) {
        return AuditLogPdfGenerator.generer(logs, auteurs(logs));
    }

    /** Auteurs des actions (nom, identifiant, matricule, rôle), chargés en une seule requête. */
    private Map<UUID, AuditLogPdfGenerator.Auteur> auteurs(List<AuditLog> logs) {
        Set<UUID> ids = logs.stream().map(AuditLog::getUtilisateurId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, AuditLogPdfGenerator.Auteur> auteurs = new HashMap<>();
        for (Utilisateur utilisateur : utilisateurRepository.findAllById(ids)) {
            Salarie salarie = utilisateur.getSalarie();
            auteurs.put(utilisateur.getId(), new AuditLogPdfGenerator.Auteur(
                    salarie == null ? utilisateur.getUsername() : salarie.getPrenom() + " " + salarie.getNom(),
                    utilisateur.getUsername(),
                    salarie == null ? null : salarie.getMatricule(),
                    utilisateur.getRole() == null ? null : utilisateur.getRole().getLibelle()));
        }
        return auteurs;
    }
}