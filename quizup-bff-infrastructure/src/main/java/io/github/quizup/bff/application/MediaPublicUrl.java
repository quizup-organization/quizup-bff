package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.bff.infrastructure.out.media.MediaProperties;

/** Construction de l'URL publique d'un objet média (base publique + clé). */
final class MediaPublicUrl {

    private MediaPublicUrl() {
    }

    static String urlFor(MediaProperties properties, String objectKey) {
        String base = properties.publicBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BffProblems.MediaStorageUnavailableProblem("URL publique des médias non configurée");
        }
        return base.endsWith("/") ? base + objectKey : base + "/" + objectKey;
    }
}
