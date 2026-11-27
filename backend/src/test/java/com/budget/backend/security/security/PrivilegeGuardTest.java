package com.budget.backend.security.security;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivilegeGuardTest {
    private final UUID acteurId = UUID.randomUUID();

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private PrivilegeGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PrivilegeGuard(utilisateurRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gestionnaire", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refuseDAttribuerLeRoleAdministrateurSansEtreAdministrateur() {
        connecter(role("GESTIONNAIRE", "utilisateur.creer"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> guard.verifierPeutGererRole(role("ADMIN_SYSTEME")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void refuseDAccorderUnePermissionNonDetenue() {
        connecter(role("GESTIONNAIRE", "utilisateur.creer", "role.modifier"));

        assertThrows(ResponseStatusException.class,
                () -> guard.verifierPermissionsDetenues(Set.of(permission("audit.lire"))));
    }

    @Test
    void autoriseUnRoleDontToutesLesPermissionsSontDetenues() {
        connecter(role("GESTIONNAIRE", "utilisateur.creer", "plan.lire"));

        assertDoesNotThrow(() -> guard.verifierPeutGererRole(role("LECTEUR", "plan.lire")));
    }

    @Test
    void lAdministrateurPeutToutAttribuer() {
        connecter(role("ADMIN_SYSTEME"));

        assertDoesNotThrow(() -> guard.verifierPeutGererRole(role("ADMIN_SYSTEME", "audit.lire")));
    }

    @Test
    void interditUneActionSurSonPropreCompte() {
        connecter(role("GESTIONNAIRE"));

        assertThrows(ResponseStatusException.class,
                () -> guard.interdireSurSoiMeme(acteurId, "interdit"));
        assertDoesNotThrow(() -> guard.interdireSurSoiMeme(UUID.randomUUID(), "interdit"));
    }

    private void connecter(Role role) {
        when(utilisateurRepository.findByUsername("gestionnaire")).thenReturn(Optional.of(
                Utilisateur.builder().id(acteurId).username("gestionnaire").role(role).build()));
    }

    private Role role(String code, String... permissions) {
        Set<Permission> set = new java.util.HashSet<>();
        for (String permission : permissions) {
            set.add(permission(permission));
        }
        return Role.builder().code(code).permissions(set).build();
    }

    private Permission permission(String code) {
        return Permission.builder().code(code).build();
    }
}
