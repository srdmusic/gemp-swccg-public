package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.CardCategory;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleForfeitPolicyTest {

    private static final String ACTION_ID = "card-1";

    @Test
    public void flipGateFormationProtectionRequiresAnUnprotectedAlternative() {
        PolicyResult actor = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                    true);
        PolicyResult buddy = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_BUDDY,
                    true);
        PolicyResult flipBackBlocker = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                    true);
        PolicyResult terminalActor = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .TERMINAL_OBJECTIVE_ACTOR,
                    true);
        PolicyResult pendingTrigger = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_PENDING_TRIGGER_CONTROL_SOURCE,
                    true);
        PolicyResult surplus = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                    true);
        PolicyResult unavoidable = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    ACTION_ID,
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                    false);

        assertOperations(actor,
                op("BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                    -9999.0f,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD: preserve the last required actor while another legal loss exists",
                    TraceOutputKind.VETO));
        assertOperations(buddy,
                op("BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                    -9999.0f,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD: preserve the required actor's last buddy while another legal loss exists",
                    TraceOutputKind.VETO));
        assertOperations(flipBackBlocker,
                op("BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                    -9999.0f,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD: preserve the sole flip-back blocker while another legal loss exists",
                    TraceOutputKind.VETO));
        assertOperations(terminalActor,
                op("BATTLE.OBJECTIVE.TERMINAL_ACTOR_HOLD",
                    -9999.0f,
                    "BATTLE.OBJECTIVE.TERMINAL_ACTOR_HOLD: do not forfeit the actor whose loss would place the objective out of play while another legal loss exists",
                    TraceOutputKind.VETO));
        assertOperations(pendingTrigger,
                op("BATTLE.OBJECTIVE.PENDING_TRIGGER_HOLD",
                    -9999.0f,
                    "BATTLE.OBJECTIVE.PENDING_TRIGGER_HOLD: preserve the sole control source until the mandatory objective trigger resolves",
                    TraceOutputKind.VETO));
        assertOperations(surplus);
        assertOperations(unavoidable);
    }

    @Test
    public void optionalNoDamageEmitsMinusFiveHundredThenContinuesToLegalPass() {
        BattleForfeitFacts.CandidateFacts candidate = new CandidateBuilder()
                .hit().dead().build();
        BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateOptional(
                decision(5, 0, candidate), candidate,
                BattleForfeitFacts.ObjectiveFlags.none());

        assertEquals(BattleForfeitPolicy.Route.OPTIONAL_FORFEIT,
                evaluation.route());
        assertTrue(evaluation.passLegal());
        assertEquals(BattleForfeitPolicy.AdapterStep.CONTINUE_CANDIDATE,
                evaluation.adapterStep());
        assertOperations(evaluation.beforeRoute(),
                op("V29.13-forfeit", -500.0f,
                        "V29.13 IMMUNE/NO DAMAGE - never forfeit voluntarily!",
                        TraceOutputKind.VETO));
        assertOperations(evaluation.afterRoute());
        float legacyScore = 50.0f + evaluation.beforeRoute().operations().get(0).delta();
        assertBits(-450.0f, legacyScore);
        assertTrue(legacyScore < -100.0f);
    }

    @Test
    public void optionalDamageKeepsV159ThenZeroForfeitThenObjectiveOrder() {
        BattleForfeitFacts.CandidateFacts candidate = new CandidateBuilder()
                .forfeit(5.0f).hit().dead().build();
        BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateOptional(
                decision(0, 6, candidate), candidate,
                new BattleForfeitFacts.ObjectiveFlags(true, true));

        assertTrue(evaluation.passLegal());
        assertEquals(BattleForfeitPolicy.AdapterStep.CONTINUE_CANDIDATE,
                evaluation.adapterStep());
        assertOperations(evaluation.beforeRoute(),
                op("V159-hit", 3000.0f,
                        "V159 FORFEIT (attr=0 dmg=6 fv=5 hit=true)"),
                op("V22.4-zero-forfeit", -80.0f,
                        "Optional forfeit but zero forfeit value",
                        TraceOutputKind.BANDED),
                op("V21-optional-required", -9999.0f,
                        "OBJECTIVE CRITICAL - don't voluntarily forfeit",
                        TraceOutputKind.VETO));
        assertOperations(evaluation.afterRoute());
    }

    @Test
    public void standaloneResidualKeepsV48ThenV139ThenRequiredObjectiveAdditiveOrder() {
        PolicyResult shipWithCrew = BattleForfeitPolicy.scoreStandaloneShipWithCrew(
                ACTION_ID, "Executor", 3);
        PolicyResult residual = BattleForfeitPolicy.scoreStandaloneResidual(
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, false, false, 7.0f, 5.0f, true, 5.0f, 4.0f,
                        new BattleForfeitFacts.ObjectiveFlags(true, true)));

        assertOperations(shipWithCrew,
                op("V48-ship-with-crew", -9999.0f,
                        "V48 SHIP WITH CREW: Executor has 3 crew aboard \u2014 forfeit crew first, not the ship!",
                        TraceOutputKind.VETO));
        assertOperations(residual,
                op("V139-forfeit-value", 30.0f, "Forfeit value 7"),
                op("V139-high-power", -100.0f,
                        "V139 High power - prefer keeping for battle"),
                op("V139-valuable-unique", -300.0f,
                        "V139 VALUABLE UNIQUE - never forfeit unless forced"),
                op("V21-forfeit-required", -9999.0f,
                        "OBJECTIVE CRITICAL - NEVER FORFEIT!", TraceOutputKind.VETO));
        assertBits(-20318.0f, 50.0f + shipWithCrew.operations().get(0).delta()
                + residual.operations().stream().map(PolicyOperation::delta)
                .reduce(0.0f, Float::sum));
    }

    @Test
    public void standalonePriorityKeepsDeadCardThenPilotOrderAndExactAdvantages() {
        BattleForfeitPolicy.StandaloneResidualFacts both =
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, true, true, 5.0f, 2.0f, false, null, null,
                        BattleForfeitFacts.ObjectiveFlags.none());
        PolicyResult priority = BattleForfeitPolicy.scoreStandalonePriority(both);
        PolicyResult residual = BattleForfeitPolicy.scoreStandaloneResidual(both);
        PolicyResult aliveResidual = BattleForfeitPolicy.scoreStandaloneResidual(
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, false, false, 5.0f, 2.0f, false, null, null,
                        BattleForfeitFacts.ObjectiveFlags.none()));

        assertOperations(priority,
                op("BATTLE-forfeit-dead-card", 140.0f,
                        "☠️ DEAD CARD (persona on table) - forfeit!",
                        TraceOutputKind.ORDERING),
                op("BATTLE-forfeit-pilot-on-ship", 50.0f,
                        "PILOT ON SHIP - forfeit first!",
                        TraceOutputKind.ORDERING));
        assertOperations(residual,
                op("V139-forfeit-value", 50.0f, "Forfeit value 5"),
                op("V139-low-power", 50.0f,
                        "Low power - cheap loss, forfeit first"));
        float aliveScore = aliveResidual.operations().stream()
                .map(PolicyOperation::delta).reduce(0.0f, Float::sum);
        float deadPilotScore = priority.operations().stream()
                .map(PolicyOperation::delta).reduce(0.0f, Float::sum)
                + residual.operations().stream()
                .map(PolicyOperation::delta).reduce(0.0f, Float::sum);
        assertBits(190.0f, deadPilotScore - aliveScore);
    }

    @Test
    public void standaloneResidualPinsV139BoundsAndV21PullableFirstMatch() {
        PolicyResult bounded = BattleForfeitPolicy.scoreStandaloneResidual(
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, false, false, 13.0f, 2.0f, true, 4.0f, 4.0f,
                        new BattleForfeitFacts.ObjectiveFlags(false, true)));
        assertOperations(bounded,
                op("V139-forfeit-value", 0.0f, "Forfeit value 13"),
                op("V139-low-power", 50.0f,
                        "Low power - cheap loss, forfeit first"),
                op("V139-generic-unique", -100.0f,
                        "V139 Unique - avoid forfeiting"),
                op("V21-forfeit-pullable", -9999.0f,
                        "OBJECTIVE PULLABLE - NEVER FORFEIT!", TraceOutputKind.VETO));

        PolicyResult middlePower = BattleForfeitPolicy.scoreStandaloneResidual(
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, false, false, null, 3.0f, false, null, null,
                        BattleForfeitFacts.ObjectiveFlags.none()));
        assertOperations(middlePower);

        PolicyResult objectiveOnly = BattleForfeitPolicy.scoreStandaloneResidual(
                new BattleForfeitPolicy.StandaloneResidualFacts(
                        ACTION_ID, false, false, null, null, false, null, null,
                        new BattleForfeitFacts.ObjectiveFlags(true, false)));
        assertOperations(objectiveOnly,
                op("V21-forfeit-required", -9999.0f,
                        "OBJECTIVE CRITICAL - NEVER FORFEIT!", TraceOutputKind.VETO));
    }

    @Test
    public void attachedWeaponRunsV154AndContinuesBeforeEveryOtherArm() {
        BattleForfeitFacts.CandidateFacts hitHostWeapon = new CandidateBuilder()
                .category(CardCategory.WEAPON).attachedHostHit().forceLoss().build();
        BattleForfeitPolicy.Evaluation hitHost = BattleForfeitPolicy.evaluateCombined(
                decision(0, 2, hitHostWeapon), hitHostWeapon);

        assertFalse(hitHost.passLegal());
        assertEquals(BattleForfeitPolicy.AdapterStep.CONTINUE_CANDIDATE,
                hitHost.adapterStep());
        assertOperations(hitHost.beforeRoute(),
                op("V154-hit-host", 2200.0f,
                        "V154 WEAPON-LOSS: strip weapon first for extra coverage"
                                + " (host is HIT \u2014 lost anyway) \u2014 before hit chars"));
        assertOperations(hitHost.afterRoute());

        BattleForfeitFacts.CandidateFacts looseWeapon = new CandidateBuilder()
                .category(CardCategory.WEAPON).build();
        BattleForfeitPolicy.Evaluation loose = BattleForfeitPolicy.evaluateCombined(
                decision(0, 2, looseWeapon), looseWeapon);
        assertOperations(loose.beforeRoute(),
                op("V154-weapon", 2000.0f,
                        "V154 WEAPON-LOSS: strip weapon first for extra coverage"
                                + " \u2014 before hit chars"));
        assertOperations(loose.afterRoute());
    }

    @Test
    public void v118StaysBeforeForceLossAndV223StaysAfterIt() {
        BattleForfeitFacts.CandidateFacts candidate = new CandidateBuilder()
                .category(CardCategory.EFFECT).forceLoss().build();
        BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateCombined(
                decision(0, 2, candidate), candidate);

        assertFalse(evaluation.passLegal());
        assertEquals(BattleForfeitPolicy.AdapterStep.APPLY_FORCE_LOSS_THEN_AFTER_ROUTE,
                evaluation.adapterStep());
        assertOperations(evaluation.beforeRoute(),
                op("V118-force-loss", 200.0f,
                        "V118 SMALL DAMAGE: only 2 battle damage \u2014 lose from reserves instead of forfeiting a character",
                        TraceOutputKind.BANDED));
        assertOperations(evaluation.afterRoute(),
                op("V22.3-damage-1-5", -40.0f,
                        "V22.3: FORFEIT CHARACTERS FIRST - they cover multiple damage points per card! (2 damage left)"));
    }

    @Test
    public void smallDamageCharacterProtectionRemainsMandatoryAndBeforeV159() {
        BattleForfeitFacts.CandidateFacts candidate = new CandidateBuilder()
                .forfeit(5.0f).build();
        BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateCombined(
                decision(0, 2, candidate), candidate);

        assertFalse(evaluation.passLegal());
        assertEquals(BattleForfeitPolicy.AdapterStep.APPLY_FORFEIT_AFTER_ROUTE,
                evaluation.adapterStep());
        assertOperations(evaluation.beforeRoute(),
                op("V118-save-character", -500.0f,
                        "V118 SAVE CHARACTER: only 2 battle damage \u2014 characters worth more than that, lose from reserves!",
                        TraceOutputKind.BANDED));
        assertOperations(evaluation.afterRoute(),
                op("V159-pure-small", -3000.0f,
                        "V159 FORFEIT (attr=0 dmg=2 fv=5 hit=false)"));
        assertBits(-3450.0f, 50.0f
                + evaluation.beforeRoute().operations().get(0).delta()
                + evaluation.afterRoute().operations().get(0).delta());
    }

    @Test
    public void forceLossTailKeepsV150AndEveryV223DamageTier() {
        BattleForfeitFacts.CandidateFacts candidate = new CandidateBuilder()
                .category(CardCategory.EFFECT).forceLoss().build();
        BattleForfeitPolicy.Evaluation attrition = BattleForfeitPolicy.evaluateCombined(
                decision(3, 8, candidate), candidate);
        assertOperations(attrition.beforeRoute());
        assertOperations(attrition.afterRoute(),
                op("V150", -500.0f,
                        "V150 CANNOT satisfy attrition with Force loss \u2014 forfeit covers attrition+damage together, don't waste pile!"));

        assertDamageTier(candidate, 3, "V22.3-damage-1-5", -40.0f);
        assertDamageTier(candidate, 6, "V22.3-damage-6-10", -80.0f);
        assertDamageTier(candidate, 11, "V22.3-damage-11-plus", -120.0f);
    }

    @Test
    public void v223HitPrecedesDeadAndUsesCandidateSetFactsOnly() {
        BattleForfeitFacts.CandidateFacts forceLoss = new CandidateBuilder()
                .category(CardCategory.EFFECT).forceLoss().build();
        BattleForfeitFacts.CandidateFacts hit = new CandidateBuilder()
                .actionId("hit").hit().build();
        BattleForfeitFacts.CandidateFacts dead = new CandidateBuilder()
                .actionId("dead").dead().build();

        BattleForfeitPolicy.Evaluation withBoth = BattleForfeitPolicy.evaluateCombined(
                decision(0, 12, hit, dead), forceLoss);
        assertOperations(withBoth.afterRoute(),
                op("V22.3-hit", -80.0f,
                        "V22.3: Have hit cards to forfeit first - much more efficient!"));

        BattleForfeitPolicy.Evaluation deadOnly = BattleForfeitPolicy.evaluateCombined(
                decision(0, 12, dead), forceLoss);
        assertOperations(deadOnly.afterRoute(),
                op("V22.3-dead", -80.0f,
                        "V22.3: Have dead cards to forfeit - they satisfy multiple damage!"));
    }

    @Test
    public void v159ScoresHitBeforeDeadOnMandatoryRoute() {
        BattleForfeitFacts.CandidateFacts both = new CandidateBuilder()
                .forfeit(6.0f).hit().dead().build();
        BattleForfeitPolicy.Evaluation hitFirst = BattleForfeitPolicy.evaluateCombined(
                decision(2, 5, both), both);
        assertOperations(hitFirst.afterRoute(),
                op("V159-hit", 1500.0f,
                        "V159 FORFEIT (attr=2 dmg=5 fv=6 hit=true)"));

        BattleForfeitFacts.CandidateFacts dead = new CandidateBuilder()
                .forfeit(6.0f).dead().build();
        BattleForfeitPolicy.Evaluation deadResult = BattleForfeitPolicy.evaluateCombined(
                decision(2, 5, dead), dead);
        assertOperations(deadResult.afterRoute(),
                op("V159-dead", 1200.0f,
                        "V159 FORFEIT (attr=2 dmg=5 fv=6 hit=false)"));
    }

    @Test
    public void attritionScoringKeepsReleaseValvesAndV178Tiebreaker() {
        BattleForfeitFacts.CandidateFacts armedCheap = new CandidateBuilder()
                .forfeit(2.0f).armed().build();
        assertAfterRoute(decision(2, 4, armedCheap), armedCheap,
                op("V159-attrition+V178", 1890.0f,
                        "V159 FORFEIT (attr=2 dmg=4 fv=2 hit=false)"));

        BattleForfeitFacts.CandidateFacts capital = new CandidateBuilder()
                .forfeit(3.0f).capitalShip().build();
        assertAfterRoute(decision(5, 2, capital), capital,
                op("V159-centerpiece-release", -1000.0f,
                        "V159 FORFEIT (attr=5 dmg=2 fv=3 hit=false)"));

        BattleForfeitFacts.CandidateFacts gameWinner = new CandidateBuilder()
                .forfeit(7.0f).powerAbility(6.0f, 4.0f).build();
        assertAfterRoute(decision(2, 2, gameWinner), gameWinner,
                op("V159-game-winner-release", -1500.0f,
                        "V159 FORFEIT (attr=2 dmg=2 fv=7 hit=false)"));
    }

    @Test
    public void pureDamageKeepsCoverageMathAndV178RawFloat() {
        BattleForfeitFacts.CandidateFacts efficient = new CandidateBuilder()
                .forfeit(7.0f).armed().build();
        assertAfterRoute(decision(0, 11, efficient), efficient,
                op("V159-pure-damage+V178", 2250.0f,
                        "V159 FORFEIT (attr=0 dmg=11 fv=7 hit=false)"));

        BattleForfeitFacts.CandidateFacts lowCoverage = new CandidateBuilder()
                .forfeit(2.0f).build();
        assertAfterRoute(decision(0, 11, lowCoverage), lowCoverage,
                op("V159-pure-low-coverage", -800.0f,
                        "V159 FORFEIT (attr=0 dmg=11 fv=2 hit=false)"));
    }

    @Test
    public void immunityKeepsV161LargeSoloCautiousAndExactPrecedence() {
        BattleForfeitFacts.CandidateFacts large = new CandidateBuilder()
                .forfeit(7.0f).armed().immunity(4.0f, 10.0f).build();
        assertAfterRoute(decision(4, 10, large), large,
                op("V161-damage-cover", 2060.0f,
                        "V159 FORFEIT (attr=4 dmg=10 fv=7 hit=false)",
                        TraceOutputKind.BANDED));

        BattleForfeitFacts.CandidateFacts solo = new CandidateBuilder()
                .forfeit(7.0f).armed().immunity(4.0f, 0.0f)
                .soloPower(1, 7.0f, 2.0f).build();
        assertAfterRoute(decision(4, 2, solo), solo,
                op("V161-solo-gap+V178", 690.0f,
                        "V159 FORFEIT (attr=4 dmg=2 fv=7 hit=false)",
                        TraceOutputKind.BANDED));

        BattleForfeitFacts.CandidateFacts grouped = new CandidateBuilder()
                .forfeit(7.0f).armed().immunity(4.0f, 0.0f)
                .soloPower(2, 7.0f, 2.0f).build();
        assertAfterRoute(decision(4, 2, grouped), grouped,
                op("V159-immune-cautious", -580.0f,
                        "V159 FORFEIT (attr=4 dmg=2 fv=7 hit=false)"));

        BattleForfeitFacts.CandidateFacts exactMismatch = new CandidateBuilder()
                .forfeit(5.0f).immunity(4.0f, 10.0f).build();
        assertAfterRoute(decision(3, 2, exactMismatch), exactMismatch,
                op("V159-attrition", 2300.0f,
                        "V159 FORFEIT (attr=3 dmg=2 fv=5 hit=false)"));

        BattleForfeitFacts.CandidateFacts noCoverage = new CandidateBuilder()
                .forfeit(7.0f).immunity(4.0f, 0.0f).build();
        assertAfterRoute(decision(4, 0, noCoverage), noCoverage,
                op("V159-immune-no-coverage", -2500.0f,
                        "V159 FORFEIT (attr=4 dmg=0 fv=7 hit=false)"));
    }

    private static void assertDamageTier(
            BattleForfeitFacts.CandidateFacts candidate, int damage,
            String ruleArmId, float delta) {
        BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateCombined(
                decision(0, damage, candidate), candidate);
        assertOperations(evaluation.afterRoute(),
                op(ruleArmId, delta,
                        "V22.3: FORFEIT CHARACTERS FIRST - they cover "
                                + "multiple damage points per card! ("
                                + damage + " damage left)"));
    }

    private static void assertAfterRoute(
            BattleForfeitFacts.DecisionFacts decision,
            BattleForfeitFacts.CandidateFacts candidate,
            Expected... expected) {
        BattleForfeitPolicy.Evaluation evaluation =
                BattleForfeitPolicy.evaluateCombined(decision, candidate);
        assertOperations(evaluation.beforeRoute());
        assertOperations(evaluation.afterRoute(), expected);
    }

    private static BattleForfeitFacts.DecisionFacts decision(
            int attrition, int damage,
            BattleForfeitFacts.CandidateFacts... candidates) {
        BattleForfeitFacts.CandidateSetFacts candidateSet = candidates.length == 0
                ? BattleForfeitFacts.CandidateSetFacts.empty()
                : BattleForfeitFacts.readCandidateSet(List.of(candidates));
        return new BattleForfeitFacts.DecisionFacts(attrition, damage, candidateSet);
    }

    private static Expected op(String ruleArmId, float delta, String reason) {
        return op(ruleArmId, delta, reason, TraceOutputKind.ORDERING);
    }

    private static Expected op(String ruleArmId, float delta, String reason,
                               TraceOutputKind outputKind) {
        return new Expected(ruleArmId, delta, reason, outputKind);
    }

    private static void assertOperations(PolicyResult result, Expected... expected) {
        assertEquals("BATTLE_FORFEIT_POLICY", result.producerId());
        assertEquals(expected.length, result.operations().size());
        Set<String> ruleArmIds = new HashSet<>();
        for (int index = 0; index < expected.length; index++) {
            PolicyOperation operation = result.operations().get(index);
            Expected expectedOperation = expected[index];
            assertTrue(ruleArmIds.add(operation.ruleArmId().id()));
            assertEquals(expectedOperation.ruleArmId(), operation.ruleArmId().id());
            assertBits(expectedOperation.delta(), operation.delta());
            assertEquals(expectedOperation.reason(), operation.reason());
            assertEquals(expectedOperation.outputKind(), operation.outputKind());
            assertEquals(TraceDomainId.BATTLE_FORFEIT, operation.domainId());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertEquals(ACTION_ID, operation.actionId());
        }
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private record Expected(String ruleArmId, float delta, String reason,
                            TraceOutputKind outputKind) {
    }

    private static final class CandidateBuilder {
        private String actionId = ACTION_ID;
        private boolean blueprintPresent = true;
        private CardCategory category = CardCategory.CHARACTER;
        private float forfeit = 5.0f;
        private boolean hit;
        private boolean dead;
        private boolean forceLoss;
        private boolean attachedHostHit;
        private boolean armed;
        private BattleForfeitFacts.ImmunityFacts immunity =
                BattleForfeitFacts.ImmunityFacts.none();
        private BattleForfeitFacts.SoloPowerFacts soloPower =
                BattleForfeitFacts.SoloPowerFacts.unavailable();
        private Float power = 3.0f;
        private Float ability = 3.0f;
        private boolean capitalShip;
        private boolean priorityCard;

        private CandidateBuilder actionId(String value) {
            actionId = value;
            return this;
        }

        private CandidateBuilder category(CardCategory value) {
            category = value;
            return this;
        }

        private CandidateBuilder forfeit(float value) {
            forfeit = value;
            return this;
        }

        private CandidateBuilder hit() {
            hit = true;
            return this;
        }

        private CandidateBuilder dead() {
            dead = true;
            return this;
        }

        private CandidateBuilder forceLoss() {
            forceLoss = true;
            return this;
        }

        private CandidateBuilder attachedHostHit() {
            attachedHostHit = true;
            return this;
        }

        private CandidateBuilder armed() {
            armed = true;
            return this;
        }

        private CandidateBuilder immunity(float exact, float lessThan) {
            immunity = new BattleForfeitFacts.ImmunityFacts(exact, lessThan);
            return this;
        }

        private CandidateBuilder soloPower(int friendlyCharacters,
                                           float opponentPower,
                                           float friendlyPower) {
            soloPower = new BattleForfeitFacts.SoloPowerFacts(true,
                    friendlyCharacters, opponentPower, friendlyPower);
            return this;
        }

        private CandidateBuilder powerAbility(float powerValue,
                                              float abilityValue) {
            power = powerValue;
            ability = abilityValue;
            return this;
        }

        private CandidateBuilder capitalShip() {
            capitalShip = true;
            return this;
        }

        private BattleForfeitFacts.CandidateFacts build() {
            return new BattleForfeitFacts.CandidateFacts(
                    actionId, blueprintPresent, category, forfeit,
                    hit, dead, forceLoss, attachedHostHit, armed,
                    immunity, soloPower, power, ability,
                    capitalShip, priorityCard);
        }
    }
}
