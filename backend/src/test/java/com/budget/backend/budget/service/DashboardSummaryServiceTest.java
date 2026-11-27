package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.repository.AuditLogRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private StructureRepository entiteRepository;
    @Mock
    private DirectionRepository directionRepository;
    @Mock
    private BudgetAllocationStructureRepository allocationRepository;
    @Mock
    private DemandeRevisionBudgetaireRepository revisionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private DashboardSummaryService service;

    @Test
    void afficheLesIndicateursDeToutesLesStructuresPourUnUtilisateurAuthentifie() {
        UUID userId = UUID.randomUUID();
        Structure entity = Structure.builder().code("ENT-A").nom("Entité A").build();
        Structure otherEntity = Structure.builder().code("ENT-B").nom("Entité B").build();
        Utilisateur user = Utilisateur.builder()
                .id(userId)
                .username("agent@example.com")
                .actif(true)
                .role(Role.builder().code("AGENT").permissions(Set.of(
                        Permission.builder().code("execution.lire").build(),
                        Permission.builder().code("revision.lire").build(),
                        Permission.builder().code("utilisateur.lire").build(),
                        Permission.builder().code("structure.lire").build(),
                        Permission.builder().code("entite.lire").build()
                )).build())
                .build();
        Utilisateur colleague = Utilisateur.builder().actif(true).build();
        BudgetAllocationStructure allocation = BudgetAllocationStructure.builder()
                .montantVote(new BigDecimal("100"))
                .montantModifie(new BigDecimal("20"))
                .montantAlloue(new BigDecimal("120"))
                .montantEngage(new BigDecimal("30"))
                .montantLiquide(new BigDecimal("10"))
                .build();
        when(utilisateurRepository.findByUsername("agent@example.com")).thenReturn(Optional.of(user));
        when(utilisateurRepository.findAll()).thenReturn(List.of(user, colleague));
        when(entiteRepository.findAll()).thenReturn(List.of(entity, otherEntity));
        when(directionRepository.count()).thenReturn(3L);
        when(allocationRepository.findByStructure_CodeIn(List.of("ENT-A", "ENT-B")))
                .thenReturn(List.of(allocation));
        when(revisionRepository.countByStatut("EN_ATTENTE")).thenReturn(2L);
        when(roleRepository.count()).thenReturn(3L);
        when(permissionRepository.count()).thenReturn(12L);
        when(auditLogRepository.count()).thenReturn(20L);

        var summary = service.getSummary("agent@example.com");

        assertTrue(summary.vueGlobale());
        assertEquals(3, summary.nombreStructures());
        assertEquals(2, summary.nombreEntites());
        assertEquals(2, summary.nombreUtilisateurs());
        assertEquals(new BigDecimal("120"), summary.budgetAlloue());
        assertEquals(new BigDecimal("30"), summary.montantEngage());
        assertEquals(2, summary.revisionsEnAttente());
        assertEquals(3, summary.nombreRoles());
        assertEquals(12, summary.nombrePermissions());
        assertEquals(20, summary.nombreJournauxAudit());
        verify(allocationRepository).findByStructure_CodeIn(List.of("ENT-A", "ENT-B"));
    }

    @Test
    void donneUneVueGlobaleUniquementAuRoleAdministrateurSysteme() {
        Utilisateur admin = Utilisateur.builder()
                .id(UUID.randomUUID())
                .username("admin@example.com")
                .role(Role.builder().code("ADMIN_SYSTEME").build())
                .build();
        when(utilisateurRepository.findByUsername("admin@example.com")).thenReturn(Optional.of(admin));
        when(utilisateurRepository.findAll()).thenReturn(List.of());
        Structure orgStructure = Structure.builder().code("ORG").nom("Organisation").build();
        when(entiteRepository.findAll()).thenReturn(List.of(orgStructure));
        when(directionRepository.count()).thenReturn(1L);
        when(allocationRepository.findByStructure_CodeIn(List.of("ORG"))).thenReturn(List.of());
        when(revisionRepository.countByStatut("EN_ATTENTE")).thenReturn(4L);

        var summary = service.getSummary("admin@example.com");

        assertTrue(summary.vueGlobale());
        assertEquals("Toutes les structures", summary.structureLibelle());
        assertEquals(4, summary.revisionsEnAttente());
        verify(entiteRepository).findAll();
    }

    @Test
    void lesIndicateursDuTableauDeBordNeSontPasLimitesParLesPermissionsDeStructure() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = Utilisateur.builder()
                .id(userId)
                .username("agent@example.com")
                .actif(true)
                .role(Role.builder().code("AGENT").build())
                .build();
        when(utilisateurRepository.findByUsername("agent@example.com")).thenReturn(Optional.of(user));
        when(utilisateurRepository.findAll()).thenReturn(List.of(user));
        Structure orgStructure = Structure.builder().code("ORG").nom("Organisation").build();
        when(entiteRepository.findAll()).thenReturn(List.of(orgStructure));
        when(directionRepository.count()).thenReturn(1L);
        when(allocationRepository.findByStructure_CodeIn(List.of("ORG"))).thenReturn(List.of());
        when(revisionRepository.countByStatut("EN_ATTENTE")).thenReturn(0L);
        when(roleRepository.count()).thenReturn(1L);
        when(permissionRepository.count()).thenReturn(1L);
        when(auditLogRepository.count()).thenReturn(0L);

        var summary = service.getSummary("agent@example.com");

        assertEquals(1, summary.nombreUtilisateurs());
        assertEquals(1, summary.nombreStructures());
        assertEquals(BigDecimal.ZERO, summary.budgetAlloue());
        assertEquals(0, summary.revisionsEnAttente());
        assertEquals(1, summary.nombreRoles());
        verify(utilisateurRepository).findAll();
        verify(allocationRepository).findByStructure_CodeIn(List.of("ORG"));
        verify(revisionRepository).countByStatut("EN_ATTENTE");
    }
}
