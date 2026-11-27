package com.budget.backend.security.service;

import com.budget.backend.security.dto.AffectationRequestDto;
import com.budget.backend.security.dto.AffectationResponseDto;
import com.budget.backend.security.entity.Affectation;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.NiveauAffectation;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.StructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffectationServiceTest {

    @Mock private AffectationRepository affectationRepository;
    @Mock private SalarieRepository salarieRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private DirectionRepository directionRepository;
    @Mock private ServiceOrganisationnelRepository serviceRepository;

    @InjectMocks
    private AffectationService service;

    private final Salarie salarie = Salarie.builder().id(UUID.randomUUID()).matricule("EMP-1").nom("Ouédraogo").prenom("Awa").build();
    private final Structure structure = Structure.builder().code("UAB_VIE").nom("UAB Vie").build();
    private final Direction direction = Direction.builder().id(UUID.randomUUID()).code("DIR-FIN").libelle("Direction financière").structure(structure).build();
    private final ServiceOrganisationnel compta = ServiceOrganisationnel.builder().id(UUID.randomUUID()).code("SVC-COMPTA").libelle("Comptabilité").direction(direction).build();
    private final List<Affectation> existantes = new ArrayList<>();

    @BeforeEach
    void preparer() {
        lenient().when(salarieRepository.findById(salarie.getId())).thenReturn(Optional.of(salarie));
        lenient().when(serviceRepository.findById(compta.getId())).thenReturn(Optional.of(compta));
        lenient().when(directionRepository.findById(direction.getId())).thenReturn(Optional.of(direction));
        lenient().when(affectationRepository.findBySalarie_Id(salarie.getId())).thenReturn(existantes);
        lenient().when(affectationRepository.save(any(Affectation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AffectationRequestDto demande(NiveauAffectation niveau, String cible, LocalDate debut, LocalDate fin) {
        return new AffectationRequestDto(salarie.getId(), niveau, cible, "Comptable", debut, fin, null);
    }

    @Test
    void calculeLeCheminEtLeStatut() {
        AffectationResponseDto cree = service.creer(demande(NiveauAffectation.SERVICE, compta.getId().toString(),
                LocalDate.now().minusDays(1), null));

        assertEquals("UAB_VIE › DIR-FIN › SVC-COMPTA", cree.chemin());
        assertEquals("UAB_VIE", cree.structureCode());
        assertEquals("EN_COURS", cree.statut());
    }

    @Test
    void leDossierAfficheLAffectationEnCoursLaPlusRecente() {
        existantes.add(Affectation.builder().id(UUID.randomUUID()).salarie(salarie).niveau(NiveauAffectation.SERVICE)
                .service(compta).dateDebut(LocalDate.now().minusYears(2)).build());
        existantes.add(Affectation.builder().id(UUID.randomUUID()).salarie(salarie).niveau(NiveauAffectation.DIRECTION)
                .direction(direction).dateDebut(LocalDate.now().minusMonths(1)).build());
        existantes.add(Affectation.builder().id(UUID.randomUUID()).salarie(salarie).niveau(NiveauAffectation.STRUCTURE)
                .structure(structure).dateDebut(LocalDate.now().plusMonths(1)).build());

        assertEquals("DIR-FIN", service.resumeActuelle(salarie).cibleCode());
    }

    @Test
    void refuseUneAffectationQuiChevaucheLaMemeEntite() {
        existantes.add(Affectation.builder().id(UUID.randomUUID()).salarie(salarie).niveau(NiveauAffectation.SERVICE)
                .service(compta).dateDebut(LocalDate.of(2026, 1, 1)).build());

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class, () -> service.creer(
                demande(NiveauAffectation.SERVICE, compta.getId().toString(), LocalDate.of(2026, 6, 1), null)));

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        verify(affectationRepository, never()).save(any());
    }

    @Test
    void accepteLaMemeEntiteSurUnePeriodeDisjointe() {
        existantes.add(Affectation.builder().id(UUID.randomUUID()).salarie(salarie).niveau(NiveauAffectation.SERVICE)
                .service(compta).dateDebut(LocalDate.of(2024, 1, 1)).dateFin(LocalDate.of(2024, 12, 31)).build());

        AffectationResponseDto cree = service.creer(
                demande(NiveauAffectation.SERVICE, compta.getId().toString(), LocalDate.of(2026, 1, 1), null));

        assertEquals("EN_COURS", cree.statut());
    }

    @Test
    void refuseUneDateDeFinAvantLeDebut() {
        ResponseStatusException erreur = assertThrows(ResponseStatusException.class, () -> service.creer(
                demande(NiveauAffectation.SERVICE, compta.getId().toString(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1))));

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
    }

    @Test
    void refuseUneEntiteInconnue() {
        UUID inconnu = UUID.randomUUID();
        when(directionRepository.findById(inconnu)).thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(ResponseStatusException.class, () -> service.creer(
                demande(NiveauAffectation.DIRECTION, inconnu.toString(), LocalDate.now(), null)));

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
    }
}
