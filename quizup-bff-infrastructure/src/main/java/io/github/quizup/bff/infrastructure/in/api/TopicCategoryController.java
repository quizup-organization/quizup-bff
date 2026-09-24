package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.response.TopicCategoryResponse;
import io.github.quizup.theme.domain.model.TopicCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Ressource {@code /api/topic-categories} — catalogue des catégories de sujets (métadonnées d'énumération).
 */
@RestController
@RequestMapping("/api")
public class TopicCategoryController {

    @GetMapping("/topic-categories")
    public ResponseEntity<List<TopicCategoryResponse>> categories() {
        return ResponseEntity.ok(
                Arrays.stream(TopicCategory.values())
                        .map(category -> new TopicCategoryResponse(category.name(), category.label()))
                        .toList()
        );
    }
}
