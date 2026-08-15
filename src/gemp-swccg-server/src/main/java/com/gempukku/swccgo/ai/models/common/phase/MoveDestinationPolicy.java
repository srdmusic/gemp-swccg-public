package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared MOVE destination and landed-ship safety analysis.
 * Adapters retain engine reads, action mutation, ladder, exception-log, and action-log ownership.
 */
public final class MoveDestinationPolicy {
    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record RetreatMode(boolean active, String originTitle) {
    }

    public enum PowerAwareDisposition {
        NONE,
        SUICIDE,
        SAFE_DRAIN,
        FAVORABLE
    }

    public record PowerAwareDestination(
            PowerAwareDisposition disposition,
            Contribution contribution,
            float projectedOurPower) {
    }

    public enum ContestDisposition {
        NONE,
        CONTEST,
        SPY_ONLY
    }

    public record SpyAwareContest(
            ContestDisposition disposition,
            Contribution contribution) {
    }

    public enum DrainThreatDisposition {
        NONE,
        SPY_NEUTRALIZED,
        TOO_DANGEROUS,
        ACTIVE
    }

    public enum WrongDirectionDisposition {
        NONE,
        RETREAT_EXEMPT,
        JOIN_GROUP_EXEMPT,
        TERMINAL_ESCAPE_EXEMPT,
        VETO
    }

    public record WrongDirectionEvaluation(
            WrongDirectionDisposition disposition,
            Contribution contribution) {
    }

    public record LandedShipEscape(
            Contribution contribution, boolean takeOff,
            boolean disembark, boolean moveAboard,
            boolean landedShipFound) {
    }

    public record CompanionVeto(boolean hardVeto, String reason) {
        private static CompanionVeto none() {
            return new CompanionVeto(false, null);
        }
    }

    public record DestinationContest(
            PhysicalCard destination,
            Contribution contestContribution,
            float opponentPowerAtDestination,
            float ourPowerAtDestination,
            boolean destinationWasUncontested,
            boolean moverArmed,
            boolean jediAtDestination,
            Contribution battlegroundAdvanceContribution,
            boolean wrongDirectionVeto,
            String wrongDirectionReason,
            String opponentUncontestedLocation,
            boolean castleVeto,
            String castleVetoReason) {
    }

    public record IconScoring(
            Contribution opponentIcons,
            Contribution ownIcons,
            Contribution noIcons) {
    }

    private MoveDestinationPolicy() {
    }

    public static Contribution missingSourceLocation() {
        return new Contribution(true, "Card not at a location", -10.0f);
    }

    public static CompanionVeto terminalObjectiveExposure(
            boolean terminalExposure) {
        if (!terminalExposure) return CompanionVeto.none();
        return new CompanionVeto(
                true,
                "Do not move the objective's terminal actor into the exact conjunction that can place the objective out of play");
    }

    public static Contribution objectivePostFlipPayoffStart(
            boolean hasSafePayoffHop,
            String actorTitle) {
        if (!hasSafePayoffHop) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_START: "
                        + (actorTitle != null
                            ? actorTitle : "named actor")
                        + " has a safe move to activate a back-side objective payoff",
                300.0f);
    }

    public static Contribution objectiveNabooDuelFrontRouteStart(
            boolean hasSafeTargetRoute,
            String actorTitle) {
        if (!hasSafeTargetRoute) return Contribution.none();
        return new Contribution(
                true,
                "NABOO DUEL FRONT ROUTE: "
                        + (actorTitle != null
                            ? actorTitle : "typed duelist")
                        + " has a safe move to the legal interior Theed Palace target",
                300.0f);
    }

    public static Contribution objectiveNabooDuelFrontRouteDestination(
            boolean advancesTargetRoute,
            String actorTitle,
            String destinationTitle) {
        if (!advancesTargetRoute) return Contribution.none();
        return new Contribution(
                true,
                "NABOO DUEL FRONT ROUTE: "
                        + (actorTitle != null
                            ? actorTitle : "typed duelist")
                        + " reaches the legal target-loss pairing at "
                        + (destinationTitle != null
                            ? destinationTitle : "this location"),
                300.0f);
    }

    public static Contribution objectiveNabooDuelFrontRouteRetention(
            boolean currentlyPaired,
            boolean remainsPaired,
            String actorTitle,
            String destinationTitle) {
        if (!currentlyPaired || remainsPaired) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                "NABOO DUEL FRONT ROUTE: "
                        + (actorTitle != null
                            ? actorTitle : "typed duelist")
                        + " would abandon the legal target-loss pairing at "
                        + (destinationTitle != null
                            ? destinationTitle : "this destination"),
                -1600.0f);
    }

    public static Contribution objectivePostFlipPayoffDestination(
            ObjectiveAnalyzer.ObjectivePostFlipPayoffRole role,
            String actorTitle,
            String destinationTitle) {
        if (role == null
                || role
                    == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE) {
            return Contribution.none();
        }
        boolean primary = role
                == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY;
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.POST_FLIP_"
                        + (primary ? "PRIMARY" : "SECONDARY")
                        + "_PAYOFF: "
                        + (actorTitle != null
                            ? actorTitle : "named actor")
                        + " activates a back-side objective payoff at "
                        + (destinationTitle != null
                            ? destinationTitle : "this location"),
                300.0f);
    }

    public static Contribution objectivePostFlipPayoffRetention(
            ObjectiveAnalyzer.ObjectivePostFlipPayoffRole currentRole,
            ObjectiveAnalyzer.ObjectivePostFlipPayoffRole destinationRole,
            String actorTitle,
            String destinationTitle) {
        int currentRank = payoffRank(currentRole);
        if (currentRank == 0
                || payoffRank(destinationRole) >= currentRank) {
            return Contribution.none();
        }
        boolean leavingPrimary = currentRole
                == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY;
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_HOLD: "
                        + (actorTitle != null
                            ? actorTitle : "named actor")
                        + " would abandon an active "
                        + (leavingPrimary ? "primary" : "secondary")
                        + " back-side payoff at "
                        + (destinationTitle != null
                            ? destinationTitle : "this destination"),
                leavingPrimary ? -1600.0f : -900.0f);
    }

    public static Contribution objectiveTerminalActorEscapeStart(
            boolean hasSafeEscape,
            String actorTitle) {
        if (!hasSafeEscape) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.TERMINAL_ACTOR_ESCAPE_START: "
                        + (actorTitle != null
                            ? actorTitle : "terminal actor")
                        + " has a safe move out of the exact objective-loss conjunction",
                300.0f);
    }

    public static Contribution objectiveTerminalActorEscapeDestination(
            boolean escapesTerminalExposure,
            String actorTitle,
            String destinationTitle) {
        if (!escapesTerminalExposure) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.TERMINAL_ACTOR_ESCAPE_DESTINATION: "
                        + (actorTitle != null
                            ? actorTitle : "terminal actor")
                        + " escapes the objective-loss conjunction via "
                        + (destinationTitle != null
                            ? destinationTitle : "this destination"),
                300.0f);
    }

    public static Contribution objectiveFirstOrderDrainPairStart(
            boolean hasSafePairHop,
            String actorTitle) {
        if (!hasSafePairHop) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR_START: "
                        + (actorTitle != null
                            ? actorTitle : "First Order character")
                        + " has a safe move that completes the objective's +1 drain pair",
                300.0f);
    }

    public static Contribution objectiveFirstOrderDrainPairDestination(
            boolean completesPair,
            String actorTitle,
            String destinationTitle) {
        if (!completesPair) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR: "
                        + (actorTitle != null
                            ? actorTitle : "First Order character")
                        + " completes the objective's +1 drain pair at "
                        + (destinationTitle != null
                            ? destinationTitle : "this battleground"),
                300.0f);
    }

    public static Contribution objectiveFirstOrderDrainPairHold(
            boolean breaksExactPair,
            String actorTitle,
            String destinationTitle) {
        if (!breaksExactPair) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR_HOLD: "
                        + (actorTitle != null
                            ? actorTitle : "First Order character")
                        + " would break the objective's active two-character drain pair"
                        + (destinationTitle != null
                            ? " by moving to " + destinationTitle : ""),
                -900.0f);
    }

    private static int payoffRank(
            ObjectiveAnalyzer.ObjectivePostFlipPayoffRole role) {
        if (role
                == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY) {
            return 2;
        }
        return role
                == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.SECONDARY
                ? 1 : 0;
    }

    public static Contribution objectiveActorRouteStart(
            boolean hasSafeAdvancingHop,
            String actorTitle) {
        if (!hasSafeAdvancingHop) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.ACTOR_ROUTE_START: "
                        + (actorTitle != null ? actorTitle : "typed actor")
                        + " has a safe move toward the flip gate",
                300.0f);
    }

    public static Contribution objectiveActorRouteDestination(
            boolean advancesRoute,
            String actorTitle,
            String destinationTitle) {
        if (!advancesRoute) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION: "
                        + (actorTitle != null ? actorTitle : "typed actor")
                        + " advances toward the flip gate via "
                        + (destinationTitle != null
                                ? destinationTitle : "this site"),
                300.0f);
    }

    public static Contribution objectiveRequiredCardEnablerStart(
            boolean hasSafeAdvancingHop,
            String actorTitle) {
        if (!hasSafeAdvancingHop) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_START: "
                        + (actorTitle != null
                            ? actorTitle : "required actor")
                        + " has a safe move toward a required-card deploy condition",
                300.0f);
    }

    public static Contribution objectiveRequiredCardEnablerDestination(
            boolean advancesRoute,
            String actorTitle,
            String destinationTitle) {
        if (!advancesRoute) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_DESTINATION: "
                        + (actorTitle != null
                            ? actorTitle : "required actor")
                        + " advances a required-card deploy condition at "
                        + (destinationTitle != null
                            ? destinationTitle : "this site"),
                300.0f);
    }

    public static Contribution objectiveActorLocationStart(
            boolean hasSafeAdvancingHop,
            String actorTitle) {
        if (!hasSafeAdvancingHop) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.ACTOR_LOCATION_START: "
                        + (actorTitle != null
                                ? actorTitle : "typed actor")
                        + " has a safe legal move to a live qualifying location",
                300.0f);
    }

    public static Contribution objectiveActorLocationDestination(
            boolean advancesLocation,
            String actorTitle,
            String destinationTitle) {
        if (!advancesLocation) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION: "
                        + (actorTitle != null
                                ? actorTitle : "typed actor")
                        + " advances the objective at "
                        + (destinationTitle != null
                                ? destinationTitle : "this location"),
                300.0f);
    }

    public static Contribution objectiveBlockerChaseStart(
            boolean hasSafeChase,
            String actorTitle) {
        if (!hasSafeChase) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.BLOCKER_CHASE_START: "
                        + (actorTitle != null
                                ? actorTitle : "typed actor")
                        + " has a safe legal move toward a flip blocker",
                300.0f);
    }

    public static Contribution objectiveBlockerChaseDestination(
            boolean chasesBlocker,
            String actorTitle,
            String destinationTitle) {
        if (!chasesBlocker) return Contribution.none();
        return new Contribution(
                true,
                "MOVE.OBJECTIVE.BLOCKER_CHASE_DESTINATION: "
                        + (actorTitle != null
                                ? actorTitle : "typed actor")
                        + " chases the blocker at "
                        + (destinationTitle != null
                                ? destinationTitle : "this location"),
                300.0f);
    }

    public static IconScoring icons(int ownIcons, int opponentIcons) {
        Contribution opponent = opponentIcons > 0
                ? new Contribution(
                        true,
                        opponentIcons + " opponent icons = force drain potential!",
                        opponentIcons * 15.0f)
                : Contribution.none();
        Contribution own = ownIcons > 0
                ? new Contribution(
                        true,
                        ownIcons + " of our icons = force generation",
                        ownIcons * 7.5f)
                : Contribution.none();
        Contribution none = ownIcons + opponentIcons == 0
                ? new Contribution(
                        true,
                        "No icons at location - low value",
                        -10.0f)
                : Contribution.none();
        return new IconScoring(opponent, own, none);
    }

    public static Contribution power(
            float ownPower,
            float opponentPower,
            int ownIcons,
            int opponentIcons) {
        if (ownPower >= opponentPower && opponentPower > 0.0f) {
            return new Contribution(
                    true, "We have power advantage here", 10.0f);
        }
        if (opponentPower - ownPower <= 2.0f
                && opponentPower > 0.0f) {
            return new Contribution(
                    true, "Can help reinforce here", 10.0f);
        }
        if (opponentPower == 0.0f) {
            if (opponentIcons > 0) {
                return new Contribution(
                        true,
                        "Unoccupied with opponent icons - force drain!",
                        20.0f);
            }
            if (ownIcons > 0) {
                return new Contribution(
                        true,
                        "Unoccupied with our icons - control",
                        10.0f);
            }
            return new Contribution(
                    true,
                    "Unoccupied but no icons - low priority",
                    0.0f);
        }
        return new Contribution(
                true,
                "Enemy too strong (" + (int) opponentPower + " power)",
                -5.0f * opponentPower);
    }

    public static Contribution battleground(
            Boolean engineBattleground,
            boolean titleContainsBattleground) {
        if (engineBattleground != null) {
            return engineBattleground
                    ? new Contribution(
                            true,
                            "V29.7 Move to battleground — force drains!",
                            40.0f)
                    : new Contribution(
                            true,
                            "V29.7 Non-battleground destination",
                            0.0f);
        }
        if (titleContainsBattleground) {
            return new Contribution(
                    true, "Battleground location", 15.0f);
        }
        return Contribution.none();
    }

    public static Contribution evazanCombo(
            boolean movingEvazan,
            boolean movingWeaponCharacter,
            boolean comboPartnerAtDestination) {
        if ((movingEvazan || movingWeaponCharacter)
                && comboPartnerAtDestination) {
            return new Contribution(
                    true,
                    "V24.3 EVAZAN COMBO: Move here — combo partner at this site for weapon kill combo!",
                    200.0f);
        }
        return Contribution.none();
    }

    public static PhysicalCard resolveDestination(
            GameState gameState,
            PhysicalCard source,
            String actionLower) {
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location == null || location == source) {
                continue;
            }
            String locationName = location.getTitle() != null
                    ? location.getTitle().toLowerCase(Locale.ROOT) : "";
            if (!locationName.isEmpty()
                    && actionLower.contains(locationName)) {
                return location;
            }
        }
        return null;
    }

    public static Contribution battlegroundRetreat(
            String sourceTitle,
            String destinationTitle,
            boolean sourceBattleground,
            boolean destinationBattleground) {
        if (!sourceBattleground || destinationBattleground) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V37 NO RETREAT: Moving from battleground %s"
                                + " to non-battleground %s"
                                + " — lose drain and battle ability!",
                        sourceTitle,
                        destinationTitle),
                -800.0f);
    }

    public static RetreatMode retreatMode(
            String originTitle, float opponentPowerExcess) {
        return new RetreatMode(opponentPowerExcess > 0.0f, originTitle);
    }

    public static Contribution safeRetreatDestination(
            RetreatMode retreatMode,
            String destinationTitle,
            float opponentPowerAtDestination) {
        if (retreatMode == null || !retreatMode.active()
                || opponentPowerAtDestination != 0.0f) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V169 RETREAT: %s is safe (no opponent power) — get the endangered character out of %s!",
                        destinationTitle,
                        retreatMode.originTitle()),
                600.0f);
    }

    public static boolean retreatExemptsWrongDirection(
            RetreatMode retreatMode) {
        return retreatMode != null && retreatMode.active();
    }

    public static Contribution retreatToDrain(
            String sourceTitle,
            float opponentPowerAtSource,
            float ourPowerAtSource,
            boolean sourceBattleground,
            String destinationTitle,
            float opponentPowerAtDestination,
            boolean destinationBattleground,
            int friendlyDrainIcons) {
        if (!sourceBattleground
                || opponentPowerAtSource <= 0.0f
                || opponentPowerAtSource <= ourPowerAtSource
                || destinationBattleground
                || opponentPowerAtDestination != 0.0f
                || friendlyDrainIcons <= 0) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V67au RETREAT-TO-DRAIN: %s is over-contested (their %.0f vs our %.0f) — move to safe adjacent %s (no opp, %d friendly icons) and drain there!",
                        sourceTitle,
                        opponentPowerAtSource,
                        ourPowerAtSource,
                        destinationTitle,
                        friendlyDrainIcons),
                400.0f);
    }

    public static PowerAwareDestination powerAwareHiddenPathDestination(
            boolean enabled,
            String destinationTitle,
            float opponentPowerAtDestination,
            float ourPowerAtDestination) {
        float projectedOurPower = ourPowerAtDestination + 6.0f;
        if (!enabled || destinationTitle == null
                || destinationTitle.toLowerCase(Locale.ROOT)
                        .contains("mapuzo")) {
            return new PowerAwareDestination(
                    PowerAwareDisposition.NONE,
                    Contribution.none(), projectedOurPower);
        }

        if (opponentPowerAtDestination >= 7.0f
                && projectedOurPower
                        < opponentPowerAtDestination + 2.0f) {
            float deathPenalty = -1500.0f;
            if (opponentPowerAtDestination >= 9.0f) {
                deathPenalty = -1800.0f;
            }
            if (opponentPowerAtDestination >= 12.0f) {
                deathPenalty = -2500.0f;
            }
            return new PowerAwareDestination(
                    PowerAwareDisposition.SUICIDE,
                    new Contribution(
                            true,
                            "V64 SUICIDE MOVE: " + destinationTitle
                                    + " has enemy power "
                                    + (int) opponentPowerAtDestination
                                    + " — solo Jedi will DIE on their next turn!",
                            deathPenalty),
                    projectedOurPower);
        }
        if (opponentPowerAtDestination == 0.0f) {
            return new PowerAwareDestination(
                    PowerAwareDisposition.SAFE_DRAIN,
                    new Contribution(
                            true,
                            "V64 SAFE DRAIN: " + destinationTitle
                                    + " is empty — Jedi can drain without opposition!",
                            150.0f),
                    projectedOurPower);
        }
        if (projectedOurPower
                >= opponentPowerAtDestination + 3.0f) {
            return new PowerAwareDestination(
                    PowerAwareDisposition.FAVORABLE,
                    new Contribution(
                            true,
                            "V64 FAVORABLE: " + destinationTitle
                                    + " — Jedi arrival gives us power advantage",
                            80.0f),
                    projectedOurPower);
        }
        return new PowerAwareDestination(
                PowerAwareDisposition.NONE,
                Contribution.none(), projectedOurPower);
    }

    public static Contribution hiddenPathPreFlipSuicide(
            boolean hiddenPathPreFlip,
            String destinationTitle,
            float nonSpyOpponentPower,
            float ourPowerAtDestination) {
        if (!hiddenPathPreFlip || nonSpyOpponentPower < 5.0f
                || ourPowerAtDestination != 0.0f) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V67aa HIDDEN PATH SUICIDE BLOCK: %s has opp power %.0f — pre-flip Jedi survivors are power 3, this is SUICIDE!",
                        destinationTitle, nonSpyOpponentPower),
                -9999.0f);
    }

    public static SpyAwareContest spyAwareContest(
            String destinationTitle,
            float nonSpyOpponentPower,
            int opponentSpies,
            float ourPowerAtDestination,
            boolean jediAtDestination) {
        if (nonSpyOpponentPower > 0.0f) {
            float contestBonus = 300.0f;
            if (ourPowerAtDestination == 0.0f) {
                contestBonus += 200.0f;
            }
            if (jediAtDestination) {
                contestBonus += 200.0f;
            }
            return new SpyAwareContest(
                    ContestDisposition.CONTEST,
                    new Contribution(
                            true,
                            String.format(
                                    "V41 CONTEST DEST: Opponents (power %.0f) at %s%s — go fight!",
                                    nonSpyOpponentPower,
                                    destinationTitle,
                                    jediAtDestination ? " [JEDI!]" : ""),
                            contestBonus));
        }
        if (opponentSpies > 0) {
            return new SpyAwareContest(
                    ContestDisposition.SPY_ONLY,
                    new Contribution(
                            true,
                            "V67f SPY-ONLY: " + destinationTitle
                                    + " has only opponent spy ("
                                    + opponentSpies
                                    + ") — drain blocked, prefer draining elsewhere",
                            -1500.0f));
        }
        return new SpyAwareContest(
                ContestDisposition.NONE, Contribution.none());
    }

    public static DrainThreatDisposition drainThreat(
            float opponentPower,
            float ourPower,
            boolean ourUndercoverSpyPresent) {
        if (opponentPower <= 0.0f || ourPower != 0.0f) {
            return DrainThreatDisposition.NONE;
        }
        if (ourUndercoverSpyPresent) {
            return DrainThreatDisposition.SPY_NEUTRALIZED;
        }
        if (opponentPower >= 7.0f) {
            return DrainThreatDisposition.TOO_DANGEROUS;
        }
        return DrainThreatDisposition.ACTIVE;
    }

    public static WrongDirectionEvaluation wrongDirection(
            boolean opponentsElsewhere,
            String destinationTitle,
            String opponentLocation,
            boolean retreatExempt,
            boolean joinGroupExempt,
            boolean terminalEscapeExempt) {
        if (!opponentsElsewhere) {
            return new WrongDirectionEvaluation(
                    WrongDirectionDisposition.NONE,
                    Contribution.none());
        }
        if (retreatExempt) {
            return new WrongDirectionEvaluation(
                    WrongDirectionDisposition.RETREAT_EXEMPT,
                    Contribution.none());
        }
        if (joinGroupExempt) {
            return new WrongDirectionEvaluation(
                    WrongDirectionDisposition.JOIN_GROUP_EXEMPT,
                    Contribution.none());
        }
        if (terminalEscapeExempt) {
            return new WrongDirectionEvaluation(
                    WrongDirectionDisposition.TERMINAL_ESCAPE_EXEMPT,
                    Contribution.none());
        }
        return new WrongDirectionEvaluation(
                WrongDirectionDisposition.VETO,
                new Contribution(
                        true,
                        String.format(
                                "V41 WRONG DIRECTION: %s is empty — opponents draining at %s! Go there instead!",
                                destinationTitle, opponentLocation),
                        -9999.0f));
    }

    public static boolean isCastleDestination(String destinationTitle) {
        String titleLower = destinationTitle != null
                ? destinationTitle.toLowerCase(Locale.ROOT) : "";
        return titleLower.contains("mustafar")
                && titleLower.contains("castle");
    }

    public static Contribution castleRetreat(
            String destinationTitle, boolean opponentsPresent) {
        if (!opponentsPresent
                || !isCastleDestination(destinationTitle)) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                "V41 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!",
                -9999.0f);
    }

    public static boolean isSelfMoveToFriend(String gameText) {
        if (gameText == null) {
            return false;
        }
        String lower = gameText.toLowerCase(Locale.ROOT);
        return lower.contains("may move to same site as")
                || lower.contains("moves to same site as");
    }

    public static CompanionVeto companionVeto(
            String moverTitle,
            String destinationTitle,
            boolean selfMoveToFriend,
            int friendlyCharactersAtDestination) {
        if (!selfMoveToFriend || friendlyCharactersAtDestination != 0) {
            return CompanionVeto.none();
        }
        return new CompanionVeto(
                true,
                String.format(
                        "V135 SELF-MOVE-TO-FRIEND ALONE: '%s' would land alone at %s"
                                + " — no friendly characters there",
                        moverTitle,
                        destinationTitle));
    }

    public static LandedShipEscape landedShipEscape(
            GameState gameState, SwccgGame game, PhysicalCard location,
            String playerId, Supplier<String> actionTextSupplier) {
        boolean currentIsSystem = false;
        try {
            currentIsSystem = location.getBlueprint().getCardSubtype()
                    == CardSubtype.SYSTEM;
        } catch (Exception e) {
            // Preserve V91's fail-open classification as a site.
        }

        if (currentIsSystem) {
            return new LandedShipEscape(
                    Contribution.none(), false, false,
                    false, false);
        }

        String actionText = actionTextSupplier.get();
        String actionLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";
        boolean takeOff = actionLower.contains("take off");
        boolean disembark = actionLower.contains("disembark");
        boolean moveAboard = actionLower.contains("embark") && !disembark;
        if (!takeOff && !disembark) {
            return new LandedShipEscape(
                    Contribution.none(), takeOff, disembark,
                    moveAboard, false);
        }

        boolean landedShipFound = false;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) {
                continue;
            }
            if (!playerId.equals(card.getOwner())) {
                continue;
            }
            if (card.getBlueprint() == null) {
                continue;
            }
            if (card.getBlueprint().getCardCategory()
                    != CardCategory.STARSHIP) {
                continue;
            }
            PhysicalCard cardLocation = null;
            try {
                cardLocation = game.getModifiersQuerying()
                        .getLocationThatCardIsAt(gameState, card);
            } catch (Exception e) {
                // Preserve V91's per-ship location failure.
            }
            if (cardLocation == location) {
                landedShipFound = true;
                break;
            }
        }

        if (!landedShipFound) {
            return new LandedShipEscape(
                    Contribution.none(), takeOff, disembark,
                    moveAboard, false);
        }

        float bonus = takeOff ? 800.0f : 600.0f;
        return new LandedShipEscape(
                new Contribution(
                        true,
                        String.format(
                                "V91 ESCAPE LANDED SHIP: %s at site %s — %s to restore ship power / use character on ground",
                                takeOff ? "Take off" : "Disembark",
                                location.getTitle(),
                                takeOff ? "lift to system"
                                        : "drop pilot to ground"),
                        bonus),
                takeOff, disembark, moveAboard, true);
    }

    public static DestinationContest destinationContest(
            GameState gameState, SwccgGame game,
            PhysicalCard source, PhysicalCard cardToMove,
            String playerId, String opponentId,
            String actionLower, Predicate<String> jediDetector,
            Consumer<PhysicalCard> uncontestedDestinationObserver) {
        PhysicalCard destination = resolveDestination(
                gameState, source, actionLower);

        if (destination == null) {
            return noneDestination();
        }

        float opponentPowerAtDestination = 0;
        try {
            opponentPowerAtDestination = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, destination, opponentId,
                            false, false);
        } catch (Exception e) {
            // Preserve V34's fail-open read as empty.
        }

        if (opponentPowerAtDestination > 0) {
            return contestDestination(
                    gameState, game, cardToMove,
                    playerId, destination, opponentPowerAtDestination,
                    jediDetector, uncontestedDestinationObserver);
        }

        return emptyDestination(
                gameState, game, source,
                playerId, opponentId, destination,
                opponentPowerAtDestination);
    }

    private static DestinationContest contestDestination(
            GameState gameState, SwccgGame game,
            PhysicalCard cardToMove,
            String playerId, PhysicalCard destination,
            float opponentPowerAtDestination,
            Predicate<String> jediDetector,
            Consumer<PhysicalCard> uncontestedDestinationObserver) {
        float ourPowerAtDestination = 0;
        try {
            ourPowerAtDestination = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, destination, playerId,
                            false, false);
        } catch (Exception e) {
            // Preserve V36's fail-open read as uncontested.
        }

        boolean destinationWasUncontested = ourPowerAtDestination == 0;
        float contestBonus = 250.0f;
        if (destinationWasUncontested) {
            contestBonus += 150.0f;
            uncontestedDestinationObserver.accept(destination);
        }

        boolean moverArmed = false;
        if (cardToMove != null) {
            try {
                List<PhysicalCard> attachments =
                        gameState.getAttachedCards(cardToMove);
                if (attachments != null) {
                    for (PhysicalCard attachment : attachments) {
                        if (attachment != null
                                && attachment.getBlueprint() != null
                                && attachment.getBlueprint().getCardCategory()
                                        == CardCategory.WEAPON) {
                            moverArmed = true;
                            contestBonus += 100.0f;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Preserve V34's fail-open weapon read.
            }
        }

        boolean jediAtDestination = false;
        try {
            for (PhysicalCard card :
                    gameState.getCardsAtLocation(destination)) {
                if (card == null || playerId.equals(card.getOwner())) {
                    continue;
                }
                String cardTitle = card.getTitle() != null
                        ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                if (jediDetector.test(cardTitle)) {
                    jediAtDestination = true;
                    break;
                }
            }
        } catch (Exception e) {
            // Preserve V35's fail-open destination scan.
        }

        if (jediAtDestination && cardToMove != null
                && cardToMove.getTitle() != null
                && cardToMove.getTitle().toLowerCase(Locale.ROOT)
                        .contains("vader")) {
            contestBonus += 150.0f;
        }

        Contribution contest = new Contribution(
                true,
                String.format(
                        "V34 CONTEST: Moving to %s where opponents have power %.0f%s — block their drain and fight!",
                        destination.getTitle(), opponentPowerAtDestination,
                        jediAtDestination ? " [JEDI!]" : ""),
                contestBonus);
        return new DestinationContest(
                destination, contest, opponentPowerAtDestination,
                ourPowerAtDestination, destinationWasUncontested,
                moverArmed, jediAtDestination,
                Contribution.none(), false, null, null,
                false, null);
    }

    private static DestinationContest emptyDestination(
            GameState gameState, SwccgGame game,
            PhysicalCard source,
            String playerId, String opponentId,
            PhysicalCard destination,
            float opponentPowerAtDestination) {
        boolean opponentsUncontested = false;
        String opponentUncontestedLocation = null;
        float opponentUncontestedPower = 0;
        try {
            for (PhysicalCard otherLocation :
                    gameState.getLocationsInOrder()) {
                if (otherLocation == null || otherLocation == source
                        || otherLocation == destination) {
                    continue;
                }
                float opponentPower = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, otherLocation, opponentId,
                                false, false);
                float ourPower = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, otherLocation, playerId,
                                false, false);
                if (opponentPower > 0 && ourPower == 0) {
                    opponentsUncontested = true;
                    if (opponentPower > opponentUncontestedPower) {
                        opponentUncontestedPower = opponentPower;
                        opponentUncontestedLocation =
                                otherLocation.getTitle();
                    }
                }
            }
        } catch (Exception e) {
            // Preserve the partial V38.3 scan result.
        }

        Contribution battlegroundAdvance = Contribution.none();
        boolean wrongDirectionVeto = false;
        String wrongDirectionReason = null;
        if (opponentsUncontested) {
            boolean currentNonBattleground = false;
            boolean destinationBattleground = false;
            try {
                currentNonBattleground = !game.getModifiersQuerying()
                        .isBattleground(gameState, source, null);
                destinationBattleground = game.getModifiersQuerying()
                        .isBattleground(gameState, destination, null);
            } catch (Exception e) {
                // Preserve V111's partial fail-open reads.
            }

            if (currentNonBattleground && destinationBattleground) {
                battlegroundAdvance = new Contribution(
                        true,
                        String.format(
                                "V111 BG ADVANCE: Moving from non-battleground %s to battleground %s — establish drain position!",
                                source.getTitle(), destination.getTitle()),
                        400.0f);
            } else {
                wrongDirectionVeto = true;
                wrongDirectionReason = String.format(
                        "V38.3 WRONG DIRECTION: Moving to empty %s while opponents at %s",
                        destination.getTitle(),
                        opponentUncontestedLocation);
            }
        }

        boolean castleVeto = false;
        String castleVetoReason = null;
        String destinationTitle = destination.getTitle() != null
                ? destination.getTitle().toLowerCase(Locale.ROOT) : "";
        if (destinationTitle.contains("mustafar")
                && destinationTitle.contains("castle")) {
            boolean anyOpponentsOnBoard = false;
            try {
                for (PhysicalCard otherLocation :
                        gameState.getLocationsInOrder()) {
                    if (otherLocation == null) {
                        continue;
                    }
                    float opponentPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(
                                    gameState, otherLocation, opponentId,
                                    false, false);
                    if (opponentPower > 0) {
                        anyOpponentsOnBoard = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // Preserve the partial Castle scan result.
            }
            if (anyOpponentsOnBoard) {
                castleVeto = true;
                castleVetoReason = "V38.3 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!";
            }
        }

        return new DestinationContest(
                destination, Contribution.none(),
                opponentPowerAtDestination, 0.0f, false,
                false, false, battlegroundAdvance,
                wrongDirectionVeto, wrongDirectionReason,
                opponentUncontestedLocation,
                castleVeto, castleVetoReason);
    }

    private static DestinationContest noneDestination() {
        return new DestinationContest(
                null, Contribution.none(), 0.0f, 0.0f,
                false, false, false, Contribution.none(),
                false, null, null, false, null);
    }
}
