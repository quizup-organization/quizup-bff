package io.github.quizup.bff.infrastructure.in.api.problem;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

/**
 * Problèmes de façade propres au BFF (validation de requête, ressource absente).
 */
public final class BffProblems {

    private BffProblems() {
    }

    public static class InvalidGameRequestProblem extends BaseProblem {
        public InvalidGameRequestProblem(String detail) {
            super("urn:quizup:bff:game:invalidRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid game request",
                    detail,
                    Map.of());
        }
    }

    public static class InvalidGameListRequestProblem extends BaseProblem {
        public InvalidGameListRequestProblem(String detail) {
            super("urn:quizup:bff:game:invalidListRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid game list request",
                    detail,
                    Map.of());
        }
    }

    public static class InvalidTopicListRequestProblem extends BaseProblem {
        public InvalidTopicListRequestProblem(String detail) {
            super("urn:quizup:bff:topic:invalidListRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid topic list request",
                    detail,
                    Map.of());
        }
    }

    public static class InvalidQuestionRequestProblem extends BaseProblem {
        public InvalidQuestionRequestProblem(String detail) {
            super("urn:quizup:bff:question:invalidRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid question request",
                    detail,
                    Map.of());
        }
    }

    public static class PushDisabledProblem extends BaseProblem {
        public PushDisabledProblem() {
            super("urn:quizup:bff:push:disabled",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Web Push disabled",
                    "Web Push is not configured on this server",
                    Map.of());
        }
    }

    public static class NotTopicOwnerProblem extends BaseProblem {
        public NotTopicOwnerProblem(String topicId) {
            super("urn:quizup:bff:topic:notOwner",
                    ProblemCategory.PERMISSION,
                    "Topic management not allowed",
                    "Only the creator of the topic can manage it",
                    Map.of("topicId", topicId));
        }
    }

    /** Le challenger n'a pas joué la partie d'origine de la revanche. */
    public static class RematchNotAllowedProblem extends BaseProblem {
        public RematchNotAllowedProblem(String gameId, String playerId) {
            super("urn:quizup:bff:challenge:rematchNotAllowed",
                    ProblemCategory.PERMISSION,
                    "Rematch not allowed",
                    "The player " + playerId + " did not play the game " + gameId,
                    Map.of("gameId", gameId, "playerId", playerId));
        }
    }

    /** Partie d'origine inéligible (bot) ou adversaire incohérent. */
    public static class InvalidRematchProblem extends BaseProblem {
        public InvalidRematchProblem(String detail) {
            super("urn:quizup:bff:challenge:invalidRematch",
                    ProblemCategory.VALIDATION,
                    "Invalid rematch",
                    detail,
                    Map.of());
        }
    }

    /** Aucun fichier fourni (part manquante ou vide). */
    public static class MediaFileMissingProblem extends BaseProblem {
        public MediaFileMissingProblem() {
            super("urn:quizup:bff:media:fileMissing",
                    ProblemCategory.VALIDATION,
                    "Missing media file",
                    "The multipart part 'file' is required and must not be empty",
                    Map.of());
        }
    }

    /** Format non supporté : seuls JPEG et PNG sont acceptés (détection par contenu). */
    public static class MediaUnsupportedFormatProblem extends BaseProblem {
        public MediaUnsupportedFormatProblem() {
            super("urn:quizup:bff:media:unsupportedFormat",
                    ProblemCategory.VALIDATION,
                    "Unsupported media format",
                    "Only JPEG and PNG images are supported",
                    Map.of());
        }
    }

    /** Contenu indécodable ou dimensions/pixels hors bornes. */
    public static class MediaInvalidImageProblem extends BaseProblem {
        public MediaInvalidImageProblem(String detail) {
            super("urn:quizup:bff:media:invalidImage",
                    ProblemCategory.VALIDATION,
                    "Invalid media image",
                    detail,
                    Map.of());
        }
    }

    /** Fichier au-delà de {@code quizup.media.max-file-size} (mappé 413). */
    public static class MediaTooLargeProblem extends BaseProblem {
        public MediaTooLargeProblem(String detail) {
            super("urn:quizup:bff:media:tooLarge",
                    ProblemCategory.VALIDATION,
                    "Media file too large",
                    detail,
                    Map.of());
        }
    }

    /** Quota d'images de l'utilisateur atteint (mappé 429). */
    public static class MediaQuotaExceededProblem extends BaseProblem {
        public MediaQuotaExceededProblem(String detail) {
            super("urn:quizup:bff:media:quotaExceeded",
                    ProblemCategory.VALIDATION,
                    "Media quota exceeded",
                    detail,
                    Map.of());
        }
    }

    /** Stockage objet injoignable ou non configuré (mappé 503). */
    public static class MediaStorageUnavailableProblem extends BaseProblem {
        public MediaStorageUnavailableProblem(String detail) {
            super("urn:quizup:bff:media:storageUnavailable",
                    ProblemCategory.TECHNICAL,
                    "Media storage unavailable",
                    detail,
                    Map.of());
        }
    }

    /** Image inconnue du catalogue média (`404`). */
    public static class MediaNotFoundProblem extends BaseProblem {
        public MediaNotFoundProblem(String mediaId) {
            super("urn:quizup:bff:media:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Media not found",
                    "No media with id " + mediaId,
                    Map.of("mediaId", mediaId));
        }
    }

    /** Suppression d'une image dont l'utilisateur n'est pas l'auteur (`403`). */
    public static class MediaNotOwnerProblem extends BaseProblem {
        public MediaNotOwnerProblem(String mediaId) {
            super("urn:quizup:bff:media:notOwner",
                    ProblemCategory.PERMISSION,
                    "Media deletion not allowed",
                    "Only the uploader can delete the media " + mediaId,
                    Map.of("mediaId", mediaId));
        }
    }
}
