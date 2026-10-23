package io.github.quizup.bff.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Sépare l'avatar de base gratuit (icône + fond) des styles DiceBear Premium.
 * Le BFF ne fait pas confiance au client : il relit le preset persisté avant la commande.
 */
@Component
public class AvatarEntitlementPolicy {

    private static final String BASE_STYLE = "base";
    private static final Set<String> PREMIUM_STYLES = Set.of("micah", "lorelei", "notionists");

    private final ObjectMapper objectMapper;
    private final PremiumAccessPolicy premiumAccessPolicy;

    public AvatarEntitlementPolicy(ObjectMapper objectMapper,
                                   PremiumAccessPolicy premiumAccessPolicy) {
        this.objectMapper = objectMapper;
        this.premiumAccessPolicy = premiumAccessPolicy;
    }

    public void assertCanSave(String avatarOptions) {
        if (avatarOptions == null || avatarOptions.isBlank()) {
            return;
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(avatarOptions);
        } catch (Exception exception) {
            throw new BffProblems.InvalidAvatarOptionsProblem();
        }
        if (node == null || !node.isObject()) {
            throw new BffProblems.InvalidAvatarOptionsProblem();
        }
        String style = node.path("style").asText("micah");
        if (BASE_STYLE.equals(style)) {
            return;
        }
        if (!PREMIUM_STYLES.contains(style)) {
            throw new BffProblems.InvalidAvatarOptionsProblem();
        }
        if (!premiumAccessPolicy.isPremium()) {
            throw new BffProblems.PremiumRequiredProblem();
        }
    }
}
