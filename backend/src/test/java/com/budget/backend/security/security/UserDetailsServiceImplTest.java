package com.budget.backend.security.security;

import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void authenticatesByUsername() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("agent.uab")
                .motDePasseHash("encoded-password")
                .actif(true)
                .salarie(Salarie.builder().actif(true).build())
                .role(Role.builder().code("AGENT").permissions(Set.of()).build())
                .build();
        when(utilisateurRepository.findByUsername("agent.uab")).thenReturn(Optional.of(utilisateur));

        UserDetails details = userDetailsService.loadUserByUsername("agent.uab");

        assertEquals("agent.uab", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        verify(utilisateurRepository).findByUsername("agent.uab");
    }

    @Test
    void refusesAnAccountWhoseEmployeeRecordIsInactive() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("agent.uab")
                .motDePasseHash("encoded-password")
                .actif(true)
                .salarie(Salarie.builder().actif(false).build())
                .role(Role.builder().code("AGENT").permissions(Set.of()).build())
                .build();
        when(utilisateurRepository.findByUsername("agent.uab")).thenReturn(Optional.of(utilisateur));

        assertThrows(DisabledException.class, () -> userDetailsService.loadUserByUsername("agent.uab"));
    }
}
