package io.github.quizup.bff.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarEntitlementPolicyTest {

    private final PremiumAccessPolicy premium = mock(PremiumAccessPolicy.class);
    private final AvatarEntitlementPolicy policy =
            new AvatarEntitlementPolicy(new ObjectMapper(), premium);

    @Test
    void baseAvatarIsFree() {
        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"base\",\"backgroundColor\":\"#e4e4e7\"}"));
    }

    @Test
    void dicebearAvatarRequiresPremium() {
        when(premium.isPremium()).thenReturn(false);
        assertThatThrownBy(() -> policy.assertCanSave("{\"style\":\"micah\"}"))
                .isInstanceOf(BffProblems.PremiumRequiredProblem.class);
    }

    @Test
    void premiumAllowsDicebearAvatar() {
        when(premium.isPremium()).thenReturn(true);
        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"notionists\"}"));
    }

    @Test
    void malformedAvatarIsRejected() {
        assertThatThrownBy(() -> policy.assertCanSave("not-json"))
                .isInstanceOf(BffProblems.InvalidAvatarOptionsProblem.class);
    }
}
