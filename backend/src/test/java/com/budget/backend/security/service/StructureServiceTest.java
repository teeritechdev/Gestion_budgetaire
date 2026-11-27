package com.budget.backend.security.service;

import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructureServiceTest {
    @Mock
    private StructureRepository entiteRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private StructureService entiteService;

    @Test
    void trimsAndNormalizesEntityCodeBeforeSaving() {
        Structure submitted = Structure.builder()
                .code("  entite_test ")
                .nom("  Entité de test  ")
                .build();
        when(entiteRepository.existsByCode("ENTITE_TEST")).thenReturn(false);
        when(entiteRepository.save(any(Structure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Structure created = entiteService.creerStructure(submitted, UUID.randomUUID().toString());

        assertEquals("ENTITE_TEST", created.getCode());
        assertEquals("Entité de test", created.getNom());
        verify(entiteRepository).save(submitted);
        verify(auditService).logAction(any(), eq("INSERTION"), eq("structures"), eq("ENTITE_TEST"),
                isNull(), anyMap());
    }

    @Test
    void rejectsMissingRequiredEntityFieldsWithoutSaving() {
        Structure submitted = Structure.builder().code("ENTITE_TEST").nom(" ").build();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> entiteService.creerStructure(submitted, null));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(entiteRepository);
    }
}
