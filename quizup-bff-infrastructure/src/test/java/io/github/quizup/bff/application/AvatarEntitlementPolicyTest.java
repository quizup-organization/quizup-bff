package io.github.quizup.bff.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.PlayerRewards;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarEntitlementPolicyTest {

    private final AvatarEntitlementPolicy policy =
            new AvatarEntitlementPolicy(new ObjectMapper());

    @Test
    void baseAvatarIsFree() {
        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"base\",\"backgroundColor\":\"#e4e4e7\"}", false, false));
    }

    @Test
    void micahRequiresRewardOrPremium() {
        assertThatThrownBy(() -> policy.assertCanSave("{\"style\":\"micah\"}", false, false))
                .isInstanceOf(BffProblems.MicahLockedProblem.class);

        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"micah\"}", false, true));
        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"micah\"}", true, false));
    }

    @Test
    void premiumOnlyStylesStillRequirePremium() {
        assertThatThrownBy(() -> policy.assertCanSave("{\"style\":\"notionists\"}", false, true))
                .isInstanceOf(BffProblems.PremiumRequiredProblem.class);

        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"notionists\"}", true, true));
    }

    @Test
    void malformedAvatarIsRejected() {
        assertThatThrownBy(() -> policy.assertCanSave("not-json", false, false))
                .isInstanceOf(BffProblems.InvalidAvatarOptionsProblem.class);
    }

    @Test
    void rewardEntityIsNotNeededForThePolicyItself() {
        var rewards = new PlayerRewards("u1", Set.of(RewardCode.MICAH_AVATAR));
        assertThatNoException().isThrownBy(() ->
                policy.assertCanSave("{\"style\":\"micah\"}", false, rewards.has(RewardCode.MICAH_AVATAR)));
    }
}
