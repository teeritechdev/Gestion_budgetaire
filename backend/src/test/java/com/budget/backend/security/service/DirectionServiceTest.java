package com.budget.backend.security.service;

import com.budget.backend.security.dto.DirectionRequestDto;
import com.budget.backend.security.dto.DirectionResponseDto;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.StructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectionServiceTest {

    @Mock
    private DirectionRepository directionRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private ServiceOrganisationnelRepository serviceRepository;

    @InjectMocks
    private DirectionService service;

    @Test
    void uneDirectionCspEstTransversaleSansStructure() {
        when(directionRepository.save(any(Direction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DirectionResponseDto cree = service.creer(new DirectionRequestDto("DIR-SI", "Systèmes d'information", null, true, true));

        assertTrue(cree.estCsp());
        assertNull(cree.structureCode());
        verify(structureRepository, never()).findById(any());
    }

    @Test
    void refuseDeRattacherUneDirectionCspAUneStructure() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.creer(new DirectionRequestDto("DIR-SI", "Systèmes d'information", "UAB_VIE", true, true)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(directionRepository, never()).save(any());
    }

    @Test
    void exigeUneStructurePourUneDirectionClassique() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.creer(new DirectionRequestDto("DIR-FIN", "Direction financière", " ", true, false)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rattacheUneDirectionClassiqueASaStructure() {
        when(structureRepository.findById("UAB_VIE"))
                .thenReturn(Optional.of(Structure.builder().code("UAB_VIE").nom("UAB Vie").build()));
        when(directionRepository.save(any(Direction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DirectionResponseDto cree = service.creer(new DirectionRequestDto("DIR-FIN", "Direction financière", "UAB_VIE", true, null));

        assertEquals("UAB_VIE", cree.structureCode());
        assertEquals(Boolean.FALSE, cree.estCsp());
    }
}
