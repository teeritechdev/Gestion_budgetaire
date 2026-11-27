package com.budget.backend.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public final class PageUtils {

    /** Taille de page maximale acceptée (une taille supérieure est ramenée à cette valeur). */
    public static final int TAILLE_MAX = 200;

    private PageUtils() {
    }

    /** Plafonne la taille de page ; le tri demandé par le client est conservé. */
    public static Pageable limiter(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20);
        }
        if (pageable.getPageSize() <= TAILLE_MAX) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), TAILLE_MAX, pageable.getSort());
    }

    /** Vrai si l'erreur vient d'un critère de tri qui ne correspond à aucun champ (ex. sort=nimporte). */
    public static boolean estTriInvalide(Throwable erreur) {
        for (Throwable t = erreur; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.data.mapping.PropertyReferenceException) {
                return true;
            }
        }
        return false;
    }

    /** Message lisible pour un tri invalide. */
    public static String messageTriInvalide(Throwable erreur) {
        for (Throwable t = erreur; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.data.mapping.PropertyReferenceException p) {
                return "Critère de tri invalide : '" + p.getPropertyName() + "' n'est pas un champ de "
                        + p.getType().getType().getSimpleName() + ".";
            }
        }
        return "Critère de tri invalide.";
    }
}
