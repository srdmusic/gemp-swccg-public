package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure ordered scoring policy for the shared BATTLE-2 weapons slice. */
public final class BattleWeaponsPolicy {

    private static final float FAVORABLE_CANCEL_THRESHOLD = 5.0f;

    private BattleWeaponsPolicy() {
    }

    public static PolicyResult scoreActionText(BattleWeaponsFacts.ActionTextFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        addForcePush(operations, facts.actionId(), facts.forcePushMode());
        addFire(operations, facts.actionId(), facts.fireMode());
        addThrow(operations, facts.actionId(), facts.throwMode());
        addRedraw(operations, facts.actionId(), facts.redraw());

        return new PolicyResult("BATTLE_WEAPONS_ACTION_TEXT_POLICY", operations);
    }

    public static PolicyResult scoreBattleEvaluator(
            BattleWeaponsFacts.BattleEvaluatorFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        String actionId = facts.actionId();

        if (facts.fireAction()) {
            add(operations, actionId, "BATTLE-fire-base", TraceOutputKind.BANDED,
                    40.0f, "Fire weapon");
            if (facts.characterTargetText()) {
                add(operations, actionId, "BATTLE-fire-character", TraceOutputKind.BANDED,
                        10.0f, "Target character");
            }
            if (facts.uniqueTargetText()) {
                add(operations, actionId, "BATTLE-fire-unique", TraceOutputKind.BANDED,
                        20.0f, "Target unique card");
            }
        }

        addCancelBattle(operations, actionId, facts.cancelBattle());

        if (facts.battlePhase() && facts.fireAction()) {
            add(operations, actionId, "BATTLE-fire-during-battle", TraceOutputKind.ORDERING,
                    50.0f, "Fire weapons during battle");
        }
        if (facts.battlePhase() && facts.drawDestinyAction()) {
            add(operations, actionId, "BATTLE-draw-destiny", TraceOutputKind.ORDERING,
                    30.0f, "Draw battle destiny");
        }

        return new PolicyResult("BATTLE_WEAPONS_BATTLE_EVALUATOR_POLICY", operations);
    }

    public static PolicyResult scoreTarget(BattleWeaponsFacts.TargetFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        String actionId = facts.actionId();

        if (facts.alreadyHit()) {
            add(operations, actionId, "V51-already-hit", TraceOutputKind.VETO,
                    -500.0f,
                    "V51 ALREADY HIT: Target already hit — don't waste weapon!");
        }

        BattleWeaponsFacts.DestinyAssessment destiny = facts.destiny();
        if (destiny.available()) {
            float hitMargin = destiny.expectedDestinyTotal() - destiny.defenseValue();
            if (hitMargin >= 3.0f) {
                add(operations, actionId, "V36-easy-hit", TraceOutputKind.BANDED,
                        200.0f, String.format(
                                "V36 EASY HIT: %s defense %.0f, expected destiny %.1f — HIGH hit chance!",
                                facts.targetTitle(), destiny.defenseValue(),
                                destiny.expectedDestinyTotal()));
            } else if (hitMargin >= 0.0f) {
                add(operations, actionId, "V36-marginal-hit", TraceOutputKind.BANDED,
                        50.0f, String.format(
                                "V36 MARGINAL HIT: %s defense %.0f, expected destiny %.1f — might hit",
                                facts.targetTitle(), destiny.defenseValue(),
                                destiny.expectedDestinyTotal()));
            } else {
                add(operations, actionId, "V36-likely-miss", TraceOutputKind.BANDED,
                        -150.0f, String.format(
                                "V36 LIKELY MISS: %s defense %.0f, expected destiny %.1f — probably won't hit!",
                                facts.targetTitle(), destiny.defenseValue(),
                                destiny.expectedDestinyTotal()));
            }

            if (facts.gameTextCancelerPriority()) {
                add(operations, actionId, "V36-priority-game-text-canceler",
                        TraceOutputKind.BANDED, 300.0f,
                        "V36 PRIORITY: Padme cancels Vader's game text — REMOVE HER!");
            }
            if (facts.battleDestinyAdderPriority()) {
                add(operations, actionId, "V36-priority-battle-destiny-adder",
                        TraceOutputKind.BANDED, 100.0f,
                        "V36 PRIORITY: Battle destiny adder — dangerous!");
            }
            if (facts.jediOrPadawanPriority()) {
                add(operations, actionId, "V36-hunt-jedi-padawan",
                        TraceOutputKind.BANDED, 80.0f,
                        "V36 HUNT: Jedi/Padawan target — Hunt Down bonus!");
            }
        }

        // This must remain the final target contribution, even for contradictory test facts.
        if (facts.ownTargetWithHarmfulEffect()) {
            add(operations, actionId, "V38.3-self-target", TraceOutputKind.VETO,
                    -9999.0f,
                    "V38.3 SELF-TARGET: NEVER target own card with harmful effect!");
        }

        return new PolicyResult("BATTLE_WEAPONS_TARGET_POLICY", operations);
    }

    private static void addForcePush(List<PolicyOperation> operations,
                                     String actionId,
                                     BattleWeaponsFacts.ForcePushMode mode) {
        switch (mode) {
            case NONE -> {
            }
            case BATTLE_EXCLUSION -> add(operations, actionId,
                    "V29-force-push-battle", TraceOutputKind.BANDED, 80.0f,
                    "V29 FORCE PUSH: Battle exclusion — remove threat! Good use.");
            case FORCE_PILE_EXCHANGE -> add(operations, actionId,
                    "V67u-force-push-exchange", TraceOutputKind.VETO, -500.0f,
                    "V67u FORCE PUSH BLOCK: Exchange w/ Force Pile is WASTE — those cards come to hand on draw anyway. NEVER play during draw phase!");
        }
    }

    private static void addFire(List<PolicyOperation> operations,
                                String actionId,
                                BattleWeaponsFacts.FireMode mode) {
        switch (mode) {
            case NONE -> {
            }
            case VALID_TARGET_IN_BATTLE -> add(operations, actionId,
                    "V29.12-fire-battle", TraceOutputKind.ORDERING, 300.0f,
                    "V29.12 FIRE WEAPON: Fire FIRST in battle — hit target before throwing!");
            case VALID_TARGET_OUTSIDE_BATTLE -> add(operations, actionId,
                    "BATTLE-fire-valid-target", TraceOutputKind.BANDED, 50.0f,
                    "Firing weapons at valid targets");
            case NO_VALID_TARGET -> add(operations, actionId,
                    "BATTLE-fire-no-valid-target", TraceOutputKind.VETO, -30.0f,
                    "All targets already HIT - save weapon");
        }
    }

    private static void addThrow(List<PolicyOperation> operations,
                                 String actionId,
                                 BattleWeaponsFacts.ThrowMode mode) {
        switch (mode) {
            case NONE -> {
            }
            case IN_BATTLE -> add(operations, actionId,
                    "V29.12-throw-battle", TraceOutputKind.ORDERING, 200.0f,
                    "V29.12 LIGHTSABER THROW: Add destiny to attrition — do AFTER firing!");
            case OUTSIDE_BATTLE -> add(operations, actionId,
                    "V29.10-throw-outside-battle", TraceOutputKind.ORDERING, 150.0f,
                    "V29.10 LIGHTSABER THROW: Throw lightsaber to add destiny to attrition!");
        }
    }

    private static void addRedraw(List<PolicyOperation> operations,
                                  String actionId,
                                  BattleWeaponsFacts.RedrawFacts facts) {
        switch (facts.mode()) {
            case NONE -> {
            }
            case KNOWN_DESTINY -> {
                if (facts.currentDestiny() >= 3.0f) {
                    add(operations, actionId, "V37-redraw-keep", TraceOutputKind.VETO,
                            -300.0f, String.format(
                                    "V37 DON'T REDRAW: Current destiny %.0f is GOOD (avg %.1f) — keep it!",
                                    facts.currentDestiny(), facts.averageReserveDestiny()));
                } else {
                    add(operations, actionId, "V37-redraw-low", TraceOutputKind.ORDERING,
                            100.0f, String.format(
                                    "V37 REDRAW: Current destiny %.0f is LOW (avg %.1f) — try for better!",
                                    facts.currentDestiny(), facts.averageReserveDestiny()));
                }
            }
            case UNKNOWN_DESTINY -> {
                if (facts.averageReserveDestiny() >= 3.5d) {
                    add(operations, actionId, "V37-redraw-good-average",
                            TraceOutputKind.ORDERING, 30.0f,
                            "Redraw destiny — good average in reserve");
                } else {
                    add(operations, actionId, "V37-redraw-low-average",
                            TraceOutputKind.VETO, -50.0f,
                            "Redraw destiny — risky, low average in reserve");
                }
            }
            case READ_FAILED -> add(operations, actionId, "V37-redraw-read-failed",
                    TraceOutputKind.ORDERING, 30.0f, "Redraw destiny");
        }
    }

    private static void addCancelBattle(List<PolicyOperation> operations,
                                        String actionId,
                                        BattleWeaponsFacts.CancelBattleFacts facts) {
        switch (facts.mode()) {
            case NONE -> {
            }
            case OWN_INITIATED -> add(operations, actionId,
                    "BATTLE-cancel-own-battle", TraceOutputKind.VETO, -150.0f,
                    "DO NOT cancel our own battle! Waste of interrupt.");
            case OPPONENT_INITIATED_WITH_POWER -> {
                float powerDiff = facts.ourPower() - facts.theirPower();
                if (powerDiff < -FAVORABLE_CANCEL_THRESHOLD) {
                    add(operations, actionId, "BATTLE-cancel-losing",
                            TraceOutputKind.BANDED, 60.0f,
                            String.format("Cancel losing battle (%.0f vs %.0f)",
                                    facts.ourPower(), facts.theirPower()));
                } else if (powerDiff < 0.0f) {
                    add(operations, actionId, "BATTLE-cancel-unfavorable",
                            TraceOutputKind.BANDED, 20.0f,
                            String.format("Cancel unfavorable battle (%.0f vs %.0f)",
                                    facts.ourPower(), facts.theirPower()));
                } else {
                    add(operations, actionId, "BATTLE-cancel-not-losing",
                            TraceOutputKind.BANDED, -60.0f,
                            String.format("Don't cancel - we're not losing (%.0f vs %.0f)",
                                    facts.ourPower(), facts.theirPower()));
                }
            }
        }
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId,
                            String ruleId,
                            TraceOutputKind outputKind,
                            float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.BATTLE_WEAPONS, outputKind, delta, reason));
    }
}
