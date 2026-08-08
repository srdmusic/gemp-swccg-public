package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure ordered scoring policy for BATTLE action-text decisions. */
public final class BattleActionTextPolicy {

    private static final float ABILITY_POWER_EQUIVALENT = 2.5f;
    private static final int MINIMUM_BATTLE_RESERVE = 3;
    private static final int LEGACY_BIG_POWER_ADVANTAGE = 8;
    private static final int LEGACY_BIG_POWER_BONUS = 20;
    private static final int LEGACY_BATTLEGROUND_BONUS = 10;
    private static final int LEGACY_DANGER_PENALTY = 60;
    private static final int LEGACY_CLOSE_BATTLE_BONUS = 20;
    private static final int LEGACY_CONTESTED_WINNING_BONUS = 25;
    private static final String WEAPONS_PRODUCER = "BATTLE_ACTION_TEXT_POLICY";

    private BattleActionTextPolicy() {
    }

    public static float effectivePowerDifference(
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility) {
        return ourPower - theirPower
                + ((ourAbility - theirAbility) * ABILITY_POWER_EQUIVALENT);
    }

    /** Pure arithmetic for a resolved location in the top-level legacy fallback. */
    public static int scoreLegacyFallbackLocation(
            int initiateBattleScore,
            int favorableThreshold,
            int dangerThreshold,
            float powerAdvantage,
            boolean battleground,
            boolean contestedWinning) {
        int score = 0;

        if (powerAdvantage >= favorableThreshold) {
            score += initiateBattleScore;
            if (powerAdvantage >= LEGACY_BIG_POWER_ADVANTAGE) {
                score += LEGACY_BIG_POWER_BONUS;
            }
            if (battleground) {
                score += LEGACY_BATTLEGROUND_BONUS;
            }
        } else if (powerAdvantage <= dangerThreshold) {
            score -= LEGACY_DANGER_PENALTY;
        } else {
            score += LEGACY_CLOSE_BATTLE_BONUS;
        }

        if (contestedWinning) {
            score += LEGACY_CONTESTED_WINNING_BONUS;
        }
        return score;
    }

    /** Pure arithmetic for the top-level legacy board fallback. */
    public static int scoreLegacyFallbackBoard(
            int initiateBattleScore,
            int favorableThreshold,
            int dangerThreshold,
            float boardAdvantage) {
        int score = 0;
        if (boardAdvantage >= favorableThreshold) {
            score += initiateBattleScore;
        } else if (boardAdvantage <= dangerThreshold) {
            score -= LEGACY_DANGER_PENALTY;
        }
        return score;
    }

    public static PolicyResult scoreInitiation(
            BattleActionTextFacts.InitiationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (!facts.locationResolved()) {
            add(operations, facts.actionId(), "V25-battle-no-location",
                    TraceOutputKind.BANDED, 30.0f,
                    "V25 BATTLE: Initiate battle (no location data)");
        } else {
            addResolvedInitiation(operations, facts);
        }

        // V25 low-reserve arm RESTORED 2026-08-08 (passivity fix, m01683): a first
        // pass removed this as a V61 duplicate, but the capture harness
        // (exactActiveInsignificantRebellionMakesSafeBattleWin) proved the summed -50
        // is LOAD-BEARING margin at critical reserve — a 13v8 crush at 1-card reserve
        // must still lose to Pass (-10 vs -5). The two arms are tiered pacing that was
        // tuned to sum, not a 1:1 duplicate. Kept as-is.
        if (facts.reserveDeckSize() < MINIMUM_BATTLE_RESERVE) {
            add(operations, facts.actionId(), "V25-battle-low-reserve",
                    TraceOutputKind.VETO, -50.0f,
                    "V25 BATTLE: Low reserve (" + facts.reserveDeckSize()
                            + ") — bad destiny draws!");
        }

        return new PolicyResult("BATTLE_ACTION_TEXT_INITIATION_POLICY", operations);
    }

    public static PolicyResult scoreYouAreBeatenMode(
            BattleActionTextFacts.YouAreBeatenModeFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.battleFreeze() && facts.battlePhase()) {
            addWeapons(operations, facts.actionId(), "V144-you-are-beaten-freeze",
                    TraceOutputKind.BANDED, 500.0f,
                    "V144 YOU ARE BEATEN: Battle freeze in battle phase — strong use!");
        }
        return weaponsResult(operations);
    }

    public static PolicyResult scoreAddBattleDestiny(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-add-destiny", TraceOutputKind.BANDED,
                50.0f, "Adding battle destiny is great");
    }

    public static PolicyResult scoreRaceDestiny(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-race-destiny", TraceOutputKind.BANDED,
                50.0f, "Race destiny always high priority");
    }

    /**
     * Coarse And Rough And Irritating (V) limits both players to one battle
     * destiny. Fire it only when the opponent loses more excess draws than we
     * do, so the exact action is tactical rather than unconditional.
     */
    public static PolicyResult scoreSymmetricBattleDestinyCap(
            String actionId, int ourExpectedDraws,
            int opponentExpectedDraws) {
        Objects.requireNonNull(actionId, "actionId");
        int ourExcess = Math.max(0, ourExpectedDraws - 1);
        int opponentExcess =
                Math.max(0, opponentExpectedDraws - 1);
        if (opponentExcess <= ourExcess) {
            return oneWeapons(
                    actionId,
                    "BATTLE.COARSE.BATTLE_DESTINY_CAP_SKIP",
                    TraceOutputKind.ORDERING,
                    -50.0f,
                    "COARSE: preserve our equal or larger battle-destiny draw");
        }
        return oneWeapons(
                actionId,
                "BATTLE.COARSE.BATTLE_DESTINY_CAP",
                TraceOutputKind.ORDERING,
                500.0f,
                "COARSE: limit the opponent's larger battle-destiny advantage to one draw");
    }

    public static PolicyResult scoreHatred(BattleActionTextFacts.HatredFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.wrongTurn()) {
            addWeapons(operations, facts.actionId(), "V37.1-hatred-wrong-turn",
                    TraceOutputKind.VETO, -600.0f,
                    "V37.1 HATRED: Not our turn — save hatred for our deploy phase!");
        } else if (!facts.inquisitorOnTable()) {
            addWeapons(operations, facts.actionId(), "V35.7-hatred-no-inquisitor",
                    TraceOutputKind.VETO, -500.0f,
                    "V35.7 HATRED: No Inquisitor on table — hatred requires Inquisitor!");
        } else if (facts.sameSiteOpponent()) {
            float score = facts.deployPhase() ? 400.0f : 350.0f;
            if (facts.sameSiteJedi()) {
                score += 150.0f;
            }
            addWeapons(operations, facts.actionId(), "V35.7-hatred-useful",
                    TraceOutputKind.BANDED, score, String.format(
                            "V35.7 HATRED: Inquisitor WITH opponents%s — cancel game text! (+%.0f)",
                            facts.sameSiteJedi() ? " + JEDI" : "", score));
        } else {
            addWeapons(operations, facts.actionId(), "V35.3-hatred-no-opponent",
                    TraceOutputKind.VETO, -300.0f,
                    "V35.3 HATRED: Vader/Inquisitor not at same site as opponents — save for later!");
        }
        return weaponsResult(operations);
    }

    public static PolicyResult scoreIHaveYouNow(
            BattleActionTextFacts.IHaveYouNowFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.namedInActionText()) {
            if (facts.battlePhase()) {
                if (facts.vaderInBattle()) {
                    addWeapons(operations, facts.actionId(), "V29.9-ihyn-vader",
                            TraceOutputKind.BANDED, 300.0f,
                            "V29.9 IHYN: Vader in battle — PLAY I HAVE YOU NOW for devastating extra destiny draws!");
                } else {
                    addWeapons(operations, facts.actionId(), "V29.9-ihyn-battle",
                            TraceOutputKind.BANDED, 100.0f,
                            "V29.9 IHYN: Play I Have You Now for extra battle destiny!");
                }
            } else {
                addWeapons(operations, facts.actionId(), "V29.9-ihyn-save",
                        TraceOutputKind.VETO, -200.0f,
                        "V29.9 IHYN: Save I Have You Now for battle!");
            }
        } else if (facts.battlePhase() && facts.sourceCardMatches()) {
            addWeapons(operations, facts.actionId(), "V29.9-ihyn-source",
                    TraceOutputKind.BANDED, 200.0f,
                    "V29.9 IHYN: Play I Have You Now during battle — extra destiny draws!");
        }
        return weaponsResult(operations);
    }

    public static PolicyResult scoreFmftd(BattleActionTextFacts.FmftdFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.mode() == BattleActionTextFacts.FmftdMode.LOST && facts.battlePhase()) {
            int synergy = (facts.inquisitorInBattle() ? 1 : 0)
                    + (facts.jediInBattle() ? 1 : 0)
                    + (facts.hatredOnOpponent() ? 1 : 0);
            if (synergy >= 3) {
                addWeapons(operations, facts.actionId(), "V35-fmftd-lost-full",
                        TraceOutputKind.BANDED, 500.0f,
                        "V35 FMFTD LOST: Inquisitor + Jedi + Hatred — ADD 2 BATTLE DESTINY!");
            } else if (synergy >= 2) {
                addWeapons(operations, facts.actionId(), "V35-fmftd-lost-partial",
                        TraceOutputKind.BANDED, 350.0f,
                        "V35 FMFTD LOST: Inquisitor with Jedi or Hatred — add 1 battle destiny!");
            } else if (facts.inquisitorInBattle()) {
                addWeapons(operations, facts.actionId(), "V35-fmftd-lost-inquisitor",
                        TraceOutputKind.BANDED, 200.0f,
                        "V35 FMFTD LOST: Inquisitor in battle — add destiny!");
            } else {
                addWeapons(operations, facts.actionId(), "V35-fmftd-lost-limited",
                        TraceOutputKind.BANDED, 50.0f,
                        "V35 FMFTD LOST: No Inquisitor in battle — limited value");
            }
        } else if (facts.mode() == BattleActionTextFacts.FmftdMode.USED) {
            if (facts.deployOrMovePhase()) {
                addWeapons(operations, facts.actionId(), "V35-fmftd-used-deploy",
                        TraceOutputKind.BANDED, 350.0f,
                        "V35 FMFTD USED: Place hatred on opponent — cancel game text!");
            } else {
                addWeapons(operations, facts.actionId(), "V35-fmftd-used-other",
                        TraceOutputKind.BANDED, 150.0f,
                        "V35 FMFTD USED: Place hatred — decent timing");
            }
        } else if (facts.battlePhase()) {
            addWeapons(operations, facts.actionId(), "V35-fmftd-battle",
                    TraceOutputKind.BANDED, 250.0f,
                    "V35 FMFTD: Play during battle for extra destiny!");
        } else {
            addWeapons(operations, facts.actionId(), "V35-fmftd-save",
                    TraceOutputKind.VETO, -100.0f,
                    "V35 FMFTD: Save for battle if possible");
        }
        return weaponsResult(operations);
    }

    public static PolicyResult scoreVaderRecall(
            BattleActionTextFacts.VaderRecallFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.jediElsewhere()) {
            return oneWeapons(facts.actionId(), "V35-vader-recall-jedi", TraceOutputKind.BANDED,
                    300.0f,
                    "V35 VADER RECALL: Take Vader into hand — Jedi elsewhere to hunt! Redeploy!");
        }
        return oneWeapons(facts.actionId(), "V35-vader-recall-save", TraceOutputKind.VETO,
                -100.0f,
                "V35 VADER RECALL: Take Vader into hand — no clear target, keep him deployed");
    }

    public static PolicyResult scoreVirtualVaderRecall(
            BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(
                facts.actionId(),
                "OBJECTIVE.POST_FLIP.VIRTUAL_HUNT_VADER_RECALL_SAFE",
                TraceOutputKind.VETO,
                -100.0f,
                "VIRTUAL HUNT DOWN RECALL: another Vader remains on table;"
                        + " no tactical need to recall this one");
    }

    public static PolicyResult scoreInquisitorRecall(
            BattleActionTextFacts.InquisitorRecallFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.opponentsOnBoard()) {
            return oneWeapons(facts.actionId(), "V35.1-inquisitor-recall-block",
                    TraceOutputKind.VETO, -400.0f,
                    "V35.1 INQUISITOR RECALL BLOCK: Opponents on the board — KEEP Inquisitor to fight!");
        }
        return oneWeapons(facts.actionId(), "V35-inquisitor-recall", TraceOutputKind.BANDED,
                100.0f,
                "V35 INQUISITOR RECALL: No opponents on board — safe to reposition");
    }

    public static PolicyResult scoreStunningLeader(
            BattleActionTextFacts.StunningLeaderFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return switch (facts.mode()) {
            case OWN_INITIATED -> oneWeapons(facts.actionId(), "V37.2-stunning-leader-own",
                    TraceOutputKind.VETO, -9999.0f,
                    "V37.2 STUNNING LEADER: WE initiated — fight to WIN!");
            case DEFENDING -> {
                if (facts.theirPower() > facts.ourPower() * 1.5f) {
                    yield oneWeapons(facts.actionId(), "V37.2-stunning-leader-outmatched",
                            TraceOutputKind.BANDED, 300.0f, String.format(
                                    "V37.2 STUNNING LEADER: Outmatched %.0f vs %.0f — exclude to survive!",
                                    facts.ourPower(), facts.theirPower()));
                }
                yield oneWeapons(facts.actionId(), "V37.2-stunning-leader-close",
                        TraceOutputKind.VETO, -300.0f,
                        "V37.2 STUNNING LEADER: Close fight — battle instead!");
            }
            case OUTSIDE_BATTLE -> oneWeapons(facts.actionId(), "V37.2-stunning-leader-outside",
                    TraceOutputKind.VETO, -200.0f,
                    "V37.2 STUNNING LEADER: Not in battle — save!");
            case UNRESOLVED -> weaponsResult(List.of());
        };
    }

    public static PolicyResult scoreGenericYouAreBeaten(
            BattleActionTextFacts.GenericYouAreBeatenFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.battlePhase()) {
            return oneWeapons(facts.actionId(), "V35.4-you-are-beaten-battle",
                    TraceOutputKind.BANDED, 150.0f,
                    "V35.4 YOU ARE BEATEN: During battle — use for attrition!");
        }
        return oneWeapons(facts.actionId(), "V35.4-you-are-beaten-outside",
                TraceOutputKind.VETO, -200.0f,
                "V35.4 YOU ARE BEATEN: Not in battle — save for combat!");
    }

    public static PolicyResult scoreBattleDestinyModifier(
            BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-battle-destiny-modifier",
                TraceOutputKind.BANDED, 50.0f,
                "+1 to battle destiny - always use!");
    }

    public static PolicyResult scoreWeaponDestinyModifier(
            BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-weapon-destiny-modifier",
                TraceOutputKind.BANDED, 50.0f,
                "Boost weapon destiny - increases hit chance!");
    }

    public static PolicyResult scoreProtectDestiny(
            BattleActionTextFacts.ProtectDestinyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.turnNumber() <= 1) {
            return oneWeapons(facts.actionId(), "BATTLE-protect-destiny-early",
                    TraceOutputKind.VETO, -50.0f,
                    "SAVE for battle turn! Turn 1 rarely battles");
        }
        return switch (facts.phase()) {
            case BATTLE -> oneWeapons(facts.actionId(), "BATTLE-protect-destiny-battle",
                    TraceOutputKind.BANDED, 50.0f,
                    "Protect destiny draws - IN BATTLE NOW!");
            case ACTIVATE, CONTROL, DEPLOY -> oneWeapons(facts.actionId(),
                    "BATTLE-protect-destiny-opportunity", TraceOutputKind.BANDED,
                    30.0f, "Protect destiny draws - battle opportunity exists");
            case OTHER -> oneWeapons(facts.actionId(), "BATTLE-protect-destiny-save",
                    TraceOutputKind.VETO, -30.0f,
                    "Save destiny protection for clear battle turn");
        };
    }

    public static PolicyResult scorePreventOpponentBattleDestiny(
            BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-prevent-opponent-destiny",
                TraceOutputKind.BANDED, 50.0f,
                "Prevent opponent battle destiny - denies their draw!");
    }

    public static PolicyResult scoreKillShot(BattleActionTextFacts.KillShotFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.target() == BattleActionTextFacts.KillShotTarget.UNRESOLVED) {
            return oneWeapons(facts.actionId(), "V175-kill-shot-unresolved",
                    TraceOutputKind.BANDED, 0.0f,
                    "V175: make-lost target not found on table — unknown");
        }
        if (facts.target() == BattleActionTextFacts.KillShotTarget.OWN) {
            return oneWeapons(facts.actionId(), "V175-kill-shot-own",
                    TraceOutputKind.VETO, -100.0f,
                    "V175: target is OUR character — don't make our own lost");
        }
        float score = Math.min(900.0f,
                400.0f + facts.power() * 40.0f + facts.forfeit() * 20.0f);
        return oneWeapons(facts.actionId(), "V175-kill-shot",
                TraceOutputKind.BANDED, score, String.format(
                        "V175 KILL SHOT: make %s lost (power %.0f, forfeit %.0f) — take it!",
                        facts.targetTitle(), facts.power(), facts.forfeit()));
    }

    public static PolicyResult scoreSubstituteDestiny(
            BattleActionTextFacts.SubstituteDestinyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.readStatus() == BattleActionTextFacts.SubstituteReadStatus.READ_FAILED) {
            return oneWeapons(facts.actionId(), "V175-substitute-read-failed",
                    TraceOutputKind.BANDED, 30.0f,
                    "Substituting destiny is good");
        }
        float delta = facts.bestAbility() - facts.drawnDestiny();
        if (delta > 0.0f) {
            return oneWeapons(facts.actionId(), "V175-substitute-delta",
                    TraceOutputKind.BANDED, delta * 60.0f, String.format(
                            "V175 SUBSTITUTE DELTA: drawn %.0f -> ability %.0f (+%.0f gain)",
                            facts.drawnDestiny(), facts.bestAbility(), delta));
        }
        return oneWeapons(facts.actionId(), "V175-substitute-skip",
                TraceOutputKind.VETO, -50.0f, String.format(
                        "V175 SUBSTITUTE SKIP: drawn %.0f already >= ability %.0f — save the card",
                        facts.drawnDestiny(), facts.bestAbility()));
    }

    public static PolicyResult scoreCancelWeaponTargeting(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-cancel-weapon-targeting",
                TraceOutputKind.BANDED, 50.0f,
                "Cancel weapon targeting - protect our characters!");
    }

    public static PolicyResult scoreImmuneToAttrition(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-immune-to-attrition",
                TraceOutputKind.BANDED, 50.0f,
                "Make character immune to attrition - valuable protection!");
    }

    public static PolicyResult scoreProtectForfeit(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-protect-forfeit",
                TraceOutputKind.BANDED, 40.0f,
                "Protect forfeit value during battle");
    }

    public static PolicyResult scoreRetargetWeapon(BattleActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return oneWeapons(facts.actionId(), "BATTLE-retarget-weapon",
                TraceOutputKind.BANDED, 50.0f,
                "Re-target weapon at enemy - turn their weapon against them!");
    }

    private static void addResolvedInitiation(
            List<PolicyOperation> operations,
            BattleActionTextFacts.InitiationFacts facts) {
        float effectiveDiff = effectivePowerDifference(
                facts.ourPower(), facts.theirPower(),
                facts.ourAbility(), facts.theirAbility());
        String location = facts.locationTitle();

        if (facts.theirPower() <= 0.0f) {
            add(operations, facts.actionId(), "V25-battle-no-opponent",
                    TraceOutputKind.VETO, -100.0f,
                    "V25 BATTLE: No opponent at " + location);
        } else if (facts.theirPower() > facts.ourPower() * 2.0f
                && facts.theirPower() > 6.0f) {
            add(operations, facts.actionId(), "V25-battle-suicide",
                    TraceOutputKind.VETO, -500.0f,
                    String.format(
                            "V25 BATTLE SUICIDE: %.0f vs %.0f at %s — NEVER!",
                            facts.ourPower(), facts.theirPower(), location));
        } else if (effectiveDiff >= 8.0f) {
            add(operations, facts.actionId(), "V25-battle-crush",
                    TraceOutputKind.BANDED, 200.0f,
                    String.format(
                            "V25 BATTLE CRUSH at %s: %.0f vs %.0f — ATTACK!",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= 5.0f) {
            add(operations, facts.actionId(), "V25-battle-favorable",
                    TraceOutputKind.BANDED, 120.0f,
                    String.format(
                            "V25 BATTLE FAVORABLE at %s: %.0f vs %.0f",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= 2.0f) {
            add(operations, facts.actionId(), "V25-battle-marginal",
                    TraceOutputKind.BANDED, 60.0f,
                    String.format(
                            "V25 BATTLE MARGINAL at %s: %.0f vs %.0f",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= -2.0f) {
            add(operations, facts.actionId(), "V25-battle-even",
                    TraceOutputKind.BANDED, 20.0f,
                    String.format(
                            "V25 BATTLE EVEN at %s: %.0f vs %.0f — risky but worth trying",
                            location, facts.ourPower(), facts.theirPower()));
        } else {
            float penalty = -60.0f;
            if (effectiveDiff < -8.0f) {
                penalty = -120.0f;
            }
            if (effectiveDiff < -15.0f) {
                penalty = -250.0f;
            }
            add(operations, facts.actionId(), "V25-battle-unfavorable",
                    TraceOutputKind.VETO, penalty,
                    String.format(
                            "V25 BATTLE UNFAVORABLE at %s: %.0f vs %.0f — avoid!",
                            location, facts.ourPower(), facts.theirPower()));
        }
    }

    private static void add(
            List<PolicyOperation> operations,
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        operations.add(PolicyOperation.add(
                actionId,
                TraceRuleId.of(ruleId),
                TraceDomainId.BATTLE_INITIATION,
                outputKind,
                delta,
                reason));
    }

    private static PolicyResult weaponsResult(List<PolicyOperation> operations) {
        return new PolicyResult(WEAPONS_PRODUCER, operations);
    }

    private static PolicyResult oneWeapons(String actionId, String ruleId,
                                           TraceOutputKind outputKind,
                                           float delta, String reason) {
        List<PolicyOperation> operations = new ArrayList<>(1);
        addWeapons(operations, actionId, ruleId, outputKind, delta, reason);
        return weaponsResult(operations);
    }

    private static void addWeapons(List<PolicyOperation> operations,
                                   String actionId,
                                   String ruleId,
                                   TraceOutputKind outputKind,
                                   float delta,
                                   String reason) {
        operations.add(PolicyOperation.add(
                actionId,
                TraceRuleId.of(ruleId),
                TraceDomainId.BATTLE_WEAPONS,
                outputKind,
                delta,
                reason));
    }
}
