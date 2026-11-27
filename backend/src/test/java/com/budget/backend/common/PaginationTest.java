package com.budget.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.*;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {

    @Test
    void pageResponseReprendLesInformationsDeLaPageEtConvertitLeContenu() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(2, 3), 10);

        PageResponseDto<String> dto = PageResponseDto.from(page, i -> "n" + i);

        assertEquals(List.of("n1", "n2", "n3"), dto.getContent());
        assertEquals(2, dto.getNumber());
        assertEquals(3, dto.getSize());
        assertEquals(10, dto.getTotalElements());
        assertEquals(4, dto.getTotalPages());
        assertFalse(dto.isFirst());
        assertFalse(dto.isLast());
    }

    @Test
    void premiereEtDernierePage() {
        assertTrue(PageResponseDto.from(new PageImpl<>(List.of(1), PageRequest.of(0, 1), 3), i -> i).isFirst());
        assertTrue(PageResponseDto.from(new PageImpl<>(List.of(3), PageRequest.of(2, 1), 3), i -> i).isLast());
        PageResponseDto<Integer> vide = PageResponseDto.from(new PageImpl<>(List.<Integer>of(), PageRequest.of(0, 20), 0), i -> i);
        assertEquals(0, vide.getTotalPages());
        assertTrue(vide.getContent().isEmpty());
    }

    @Test
    void tailleDePagePlafonneeEtTriConserve() {
        Pageable demande = PageRequest.of(3, 100000, Sort.by(Sort.Direction.DESC, "createdAt"));

        Pageable limite = PageUtils.limiter(demande);

        assertEquals(PageUtils.TAILLE_MAX, limite.getPageSize());
        assertEquals(3, limite.getPageNumber());
        assertEquals(Sort.Direction.DESC, limite.getSort().getOrderFor("createdAt").getDirection());
        // une taille raisonnable est renvoyée telle quelle
        Pageable raisonnable = PageRequest.of(0, 50);
        assertSame(raisonnable, PageUtils.limiter(raisonnable));
        assertEquals(50, PageUtils.limiter(PageRequest.of(0, 50)).getPageSize());
    }

    @Test
    void sansPaginationOnRetrouveLaValeurParDefaut() {
        assertEquals(20, PageUtils.limiter(null).getPageSize());
        assertEquals(20, PageUtils.limiter(Pageable.unpaged()).getPageSize());
    }

    @Test
    void detectionDUnTriInvalideMemeEnveloppe() {
        PropertyReferenceException cause = new PropertyReferenceException("nimporte", TypeInformation.of(String.class), List.of());
        assertTrue(PageUtils.estTriInvalide(cause));
        assertTrue(PageUtils.estTriInvalide(new InvalidDataAccessApiUsageException("x", cause)));
        assertTrue(PageUtils.messageTriInvalide(new InvalidDataAccessApiUsageException("x", cause)).contains("nimporte"));
        assertFalse(PageUtils.estTriInvalide(new InvalidDataAccessApiUsageException("autre problème")));
        assertFalse(PageUtils.estTriInvalide(new RuntimeException("boom")));
    }
}
