package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * Catalogue d'une catégorie de sujet (code + libellé d'affichage).
 *
 * <p>Le champ est exposé sous le nom {@code category} (et non {@code code}) : c'est le contrat
 * consommé par les clients web/mobile, qui s'appuient sur {@code category} pour construire les
 * filtres de recherche ({@code category = <code>}).</p>
 */
public record TopicCategoryResponse(
        String category,
        String label
) implements Serializable {
}
