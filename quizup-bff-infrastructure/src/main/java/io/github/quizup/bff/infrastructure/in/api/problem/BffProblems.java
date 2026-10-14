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

    /** Paramètres invalides d'une requête d'administration média (`400`). */
    public static class InvalidMediaAdminRequestProblem extends BaseProblem {
        public InvalidMediaAdminRequestProblem(String detail) {
            super("urn:quizup:bff:media:invalidAdminRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid media admin request",
                    detail,
                    Map.of());
        }
    }

    /** Aucun objet S3 ni ligne {@code media_asset} pour cette clé (`404`). */
    public static class MediaObjectNotFoundProblem extends BaseProblem {
        public MediaObjectNotFoundProblem(String objectKey) {
            super("urn:quizup:bff:media:objectNotFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Media object not found",
                    "No stored object or asset matches key " + objectKey,
                    Map.of("objectKey", objectKey));
        }
    }

    /** Objet encore référencé par une entité métier : suppression refusée sans `force` (`409`). */
    public static class MediaObjectReferencedProblem extends BaseProblem {
        public MediaObjectReferencedProblem(String objectKey, long references) {
            super("urn:quizup:bff:media:referenced",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Media object referenced",
                    "The object " + objectKey + " is referenced by " + references
                            + " entity(ies); use force=true to delete it anyway",
                    Map.of("objectKey", objectKey, "references", references));
        }
    }

    /** Vérification des références inter-services indisponible : purge refusée (`503`). */
    public static class MediaReferenceUnavailableProblem extends BaseProblem {
        public MediaReferenceUnavailableProblem(String detail) {
            super("urn:quizup:bff:media:referenceCheckUnavailable",
                    ProblemCategory.TECHNICAL,
                    "Media reference check unavailable",
                    detail,
                    Map.of());
        }
    }

    /** Clé de titre mal formée (`400`) : attendu {@code topicId:GRADE}. */
    public static class InvalidEquippedTitleProblem extends BaseProblem {
        public InvalidEquippedTitleProblem(String titleKey) {
            super("urn:quizup:bff:profile:invalidTitle",
                    ProblemCategory.VALIDATION,
                    "Invalid title",
                    "The title key must be formatted as 'topicId:GRADE'",
                    Map.of("titleKey", String.valueOf(titleKey)));
        }
    }

    /**
     * Titre non débloqué par le joueur (`400`) : le grade demandé n'est pas atteint dans ce sujet.
     * La progression ne se contourne pas côté client.
     */
    public static class TitleNotEarnedProblem extends BaseProblem {
        public TitleNotEarnedProblem(String titleKey, String reachedGrade) {
            super("urn:quizup:bff:profile:titleNotEarned",
                    ProblemCategory.VALIDATION,
                    "Title not earned",
                    "The title " + titleKey + " has not been unlocked (reached grade: " + reachedGrade + ")",
                    Map.of("titleKey", titleKey, "reachedGrade", reachedGrade));
        }
    }
}
