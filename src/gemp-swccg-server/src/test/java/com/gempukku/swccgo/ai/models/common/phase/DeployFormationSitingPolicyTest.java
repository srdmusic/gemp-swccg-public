package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.phase.DeployFormationSitingPolicy.CharacterFormationFacts;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeployFormationSitingPolicyTest {

    private static final String ACTION_ID = "deploy-site-42";
    private static final String DESTINATION = "Cloud City: Guest Quarters";

    @Test
    public void v67bnUsesInclusiveFourToFiveDeficitAndRequiresCommitment() {
        assertRuleAbsent(evaluate(formation(0, 6.0f, 10.0f, false)), "V67bn");
        assertRuleAbsent(evaluate(formation(1, 6.0f, 9.99f, false)), "V67bn");
        assertDelta(evaluate(formation(1, 6.0f, 10.0f, false)), "V67bn", 800.0f);
        assertDelta(evaluate(formation(3, 6.0f, 11.0f, false)), "V67bn", 800.0f);
        assertRuleAbsent(evaluate(formation(1, 6.0f, 11.01f, false)), "V67bn");
        assertRuleAbsent(evaluate(formation(1, 6.0f, 10.0f, true)), "V67bn");

        assertOperation(operation(evaluate(formation(2, 6.0f, 10.0f, false)), "V67bn"),
                "V67bn", TraceDomainId.DEPLOY_SITING, 800.0f,
                "V67bn REINFORCE OUTGUNNED (Braveheart): 2 friendly char(s) at Cloud City: Guest Quarters (our 6 vs opp 10, deficit 4) \u2014 NO ESCAPE, DEPLOY HERE to minimize overflow!");
    }

    @Test
    public void v295BuddyPreservesEveryLiveTopologyBranch() {
        assertBuddy(character(true, true, true, 0, 0, false, 0.0f), 40.0f,
                "V29.5 BUDDY: Own location \u2014 home field advantage");
        assertBuddy(character(true, false, true, 0, 0, true, 0.0f), -20.0f,
                "V29.6 BUDDY: Opponent's location but empty table \u2014 must deploy somewhere!");
        assertBuddy(character(true, false, true, 0, 0, false, 0.0f), -150.0f,
                "V29.5 BUDDY: Opponent's location, deploying ALONE \u2014 risky!");
        assertBuddy(character(true, false, true, 0, 2, false, 0.0f), -100.0f,
                "V29.5 BUDDY: Opponent's location with enemies, NO friendlies \u2014 dangerous!");
        assertBuddy(character(true, false, true, 1, 2, false, 0.0f), 10.0f,
                "V29.5 BUDDY: Opponent's location but friendlies present");
        assertRuleAbsent(evaluate(character(true, false, false, 0, 0, false, 0.0f)),
                "V29.5-buddy");
    }

    @Test
    public void v113UsesAbilityThreeInclusiveAndRequiresSoloDeployment() {
        assertRuleAbsent(evaluate(character(true, true, false, 0, 0, false, 2.99f)), "V113");
        assertDelta(evaluate(character(true, true, false, 0, 0, false, 3.0f)), "V113", -300.0f);
        assertRuleAbsent(evaluate(character(true, true, false, 1, 0, false, 8.0f)), "V113");
        assertRuleAbsent(evaluate(character(true, true, false, 0, 0, true, 8.0f)), "V113");
        assertRuleAbsent(evaluate(character(false, true, false, 0, 0, false, 8.0f)), "V113");

        assertOperation(operation(evaluate(character(true, true, false, 0, 0, false, 3.0f)), "V113"),
                "V113", TraceDomainId.SOLO_FORMATION, -300.0f,
                "V113 SOLO VULNERABILITY: Darth Vader (ability 3) alone at Cloud City: Guest Quarters \u2014 opponent can overwhelm next turn!");
    }

    @Test
    public void liveOperationsRetainLegacySourceOrder() {
        PolicyResult result = evaluate(new CharacterFormationFacts(true, DESTINATION,
                false, true, 1, 1, false, "Darth Vader", 3.0f,
                6.0f, 10.0f, false));

        assertEquals("DEPLOY_FORMATION_SITING_POLICY", result.producerId());
        assertEquals(2, result.operations().size());
        assertEquals("V67bn", result.operations().get(0).ruleArmId().id());
        assertEquals("V29.5-buddy", result.operations().get(1).ruleArmId().id());

        PolicyResult solo = evaluate(character(true, false, true,
                0, 1, false, 3.0f));
        assertEquals(2, solo.operations().size());
        assertEquals("V29.5-buddy", solo.operations().get(0).ruleArmId().id());
        assertEquals("V113", solo.operations().get(1).ruleArmId().id());
    }

    private static PolicyResult evaluate(CharacterFormationFacts facts) {
        return DeployFormationSitingPolicy.evaluate(ACTION_ID, facts);
    }

    private static CharacterFormationFacts formation(int friendlies,
                                                      float ourPower,
                                                      float opponentPower,
                                                      boolean canEscape) {
        return new CharacterFormationFacts(true, DESTINATION,
                false, true, friendlies, 1, false,
                "Darth Vader", 6.0f, ourPower, opponentPower, canEscape);
    }

    private static CharacterFormationFacts character(boolean eligible,
                                                      boolean ourLocation,
                                                      boolean opponentLocation,
                                                      int friendlies,
                                                      int opponents,
                                                      boolean emptyTable,
                                                      float ability) {
        return new CharacterFormationFacts(eligible, DESTINATION,
                ourLocation, opponentLocation, friendlies, opponents,
                emptyTable, "Darth Vader", ability,
                0.0f, 0.0f, false);
    }

    private static void assertBuddy(CharacterFormationFacts facts,
                                    float expected,
                                    String expectedReason) {
        assertOperation(operation(evaluate(facts), "V29.5-buddy"),
                "V29.5-buddy", TraceDomainId.SOLO_FORMATION,
                expected, expectedReason);
    }

    private static void assertDelta(PolicyResult result, String ruleId,
                                    float expected) {
        assertEquals(expected, operation(result, ruleId).delta(), 0.0f);
    }

    private static PolicyOperation operation(PolicyResult result,
                                             String ruleId) {
        List<PolicyOperation> matching = result.operations().stream()
                .filter(candidate -> ruleId.equals(candidate.ruleArmId().id()))
                .toList();
        assertEquals("operation count for " + ruleId, 1, matching.size());
        return matching.get(0);
    }

    private static void assertRuleAbsent(PolicyResult result, String ruleId) {
        assertEquals(0, result.operations().stream()
                .filter(candidate -> ruleId.equals(candidate.ruleArmId().id()))
                .count());
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId,
                                        TraceDomainId domain,
                                        float delta,
                                        String reason) {
        assertEquals(ACTION_ID, operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domain, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }
}
