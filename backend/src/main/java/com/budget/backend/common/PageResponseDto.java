package com.budget.backend.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Réponse paginée commune (paramètres de requête du PDF : page, size, sort). Même forme que le JSON de Spring Data,
 * mais écrite explicitement car la sérialisation directe de PageImpl n'est pas garantie stable.
 */
@Data
@NoArgsConstructor
public class PageResponseDto<T> {
    private List<T> content;
    private int number;          // numéro de la page, à partir de 0
    private int size;            // taille de page demandée
    private long totalElements;  // nombre total d'éléments, toutes pages confondues
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <E, T> PageResponseDto<T> from(Page<E> page, Function<E, T> conversion) {
        PageResponseDto<T> dto = new PageResponseDto<>();
        dto.setContent(page.getContent().stream().map(conversion).toList());
        dto.setNumber(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalElements(page.getTotalElements());
        dto.setTotalPages(page.getTotalPages());
        dto.setFirst(page.isFirst());
        dto.setLast(page.isLast());
        return dto;
    }
}
