package com.budget.backend.security.service;

import com.budget.backend.security.entity.JetonReinitialisation;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.JetonReinitialisationRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.security.LoginAttemptService;
import com.budget.backend.security.security.PolitiqueMotDePasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MotDePasseServiceTest {

    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final JetonReinitialisationRepository jetonRepository = mock(JetonReinitialisationRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final LoginAttemptService tentatives = new LoginAttemptService();
    private final AuditService audit = mock(AuditService.class);
    private MotDePasseService service;
    private Utilisateur awa;

    @BeforeEach
    void preparer() {
        service = new MotDePasseService(utilisateurRepository, jetonRepository, encoder, tentatives, audit);
        Salarie salarie = Salarie.builder().matricule("EMP-001").numeroCnib("B12345678")
                .dateNaissance(LocalDate.of(1990, 3, 12)).build();
        awa = Utilisateur.builder().id(UUID.randomUUID()).username("awa@uab.bf").salarie(salarie).actif(true)
                .motDePasseHash(encoder.encode("Ancien!2026")).doitChangerMotDePasse(false).build();
        when(utilisateurRepository.findByUsernameIgnoreCase("awa@uab.bf")).thenReturn(Optional.of(awa));
        when(utilisateurRepository.findByUsername("awa@uab.bf")).thenReturn(Optional.of(awa));
        when(utilisateurRepository.findById(awa.getId())).thenReturn(Optional.of(awa));
    }

    @Test
    void politiqueListeLesReglesNonRespectees() {
        assertTrue(PolitiqueMotDePasse.manquements("Solide!2026", "awa@uab.bf").isEmpty());
        // longueur, majuscule, chiffre, caractère spécial (la minuscule est présente)
        assertEquals(4, PolitiqueMotDePasse.manquements("abc", "x").size());
        assertTrue(PolitiqueMotDePasse.manquements("Awa@uab!2026", "awa@uab.bf").contains("ne pas contenir votre identifiant"));
    }

    @Test
    void delivreUnJetonQuandLIdentiteCorrespond() {
        String jeton = service.verifierIdentite("awa@uab.bf", "emp-001", "b 1234 5678", LocalDate.of(1990, 3, 12), "10.0.0.1");

        assertNotNull(jeton);
        ArgumentCaptor<JetonReinitialisation> capture = ArgumentCaptor.forClass(JetonReinitialisation.class);
        verify(jetonRepository).save(capture.capture());
        assertNotEquals(jeton, capture.getValue().getEmpreinte(), "le jeton ne doit jamais être stocké en clair");
        assertTrue(capture.getValue().getExpireLe().isAfter(Instant.now()));
        verify(jetonRepository).annulerJetonsActifs(eq(awa.getId()), any());
    }

    @Test
    void refuseSansDireQuelleInformationEstFausse() {
        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.verifierIdentite("awa@uab.bf", "EMP-001", "B12345678", LocalDate.of(1991, 1, 1), "10.0.0.1"));
        ResponseStatusException inconnu = assertThrows(ResponseStatusException.class,
                () -> service.verifierIdentite("personne@uab.bf", "X", "B00000", LocalDate.of(1990, 1, 1), "10.0.0.1"));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        assertEquals(erreur.getReason(), inconnu.getReason());
        verify(jetonRepository, never()).save(any());
    }

    @Test
    void bloqueApresTropDeTentatives() {
        for (int i = 0; i < 5; i++) {
            assertThrows(ResponseStatusException.class,
                    () -> service.verifierIdentite("awa@uab.bf", "EMP-001", "B99999999", LocalDate.of(1990, 3, 12), "10.0.0.2"));
        }
        ResponseStatusException blocage = assertThrows(ResponseStatusException.class,
                () -> service.verifierIdentite("awa@uab.bf", "EMP-001", "B12345678", LocalDate.of(1990, 3, 12), "10.0.0.2"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocage.getStatusCode());
    }

    @Test
    void reinitialiseAvecUnJetonValideEtFermeLesSessions() {
        JetonReinitialisation jeton = JetonReinitialisation.builder().utilisateurId(awa.getId())
                .expireLe(Instant.now().plusSeconds(600)).build();
        when(jetonRepository.findByEmpreinte(anyString())).thenReturn(Optional.of(jeton));

        service.reinitialiser("jeton-recu", "Nouveau!2026");

        assertTrue(encoder.matches("Nouveau!2026", awa.getMotDePasseHash()));
        assertNotNull(jeton.getUtiliseLe(), "le jeton est à usage unique");
        assertNotNull(awa.getMotDePasseModifieLe());
        assertFalse(awa.getDoitChangerMotDePasse());
    }

    @Test
    void refuseUnJetonDejaUtiliseOuExpire() {
        JetonReinitialisation utilise = JetonReinitialisation.builder().utilisateurId(awa.getId())
                .expireLe(Instant.now().plusSeconds(600)).utiliseLe(Instant.now()).build();
        when(jetonRepository.findByEmpreinte(anyString())).thenReturn(Optional.of(utilise));

        assertThrows(ResponseStatusException.class, () -> service.reinitialiser("jeton-recu", "Nouveau!2026"));
        assertTrue(encoder.matches("Ancien!2026", awa.getMotDePasseHash()));
    }

    @Test
    void changeLeMotDePasseUniquementAvecLActuelCorrect() {
        ResponseStatusException erreur = assertThrows(ResponseStatusException.class,
                () -> service.changer("awa@uab.bf", "Faux!2026", "Nouveau!2026"));
        assertEquals("Le mot de passe actuel est incorrect.", erreur.getReason());

        awa.setDoitChangerMotDePasse(true);
        service.changer("awa@uab.bf", "Ancien!2026", "Nouveau!2026");

        assertTrue(encoder.matches("Nouveau!2026", awa.getMotDePasseHash()));
        assertFalse(awa.getDoitChangerMotDePasse(), "un mot de passe temporaire n'est plus exigé après changement");
    }
}
