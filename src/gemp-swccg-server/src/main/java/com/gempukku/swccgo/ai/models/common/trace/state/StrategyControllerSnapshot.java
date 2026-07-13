package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Snapshot Boundary"): the one immutable, canonical view of every retained
 * StrategyController instance field, captured by the mirrored controller's pure
 * package-local traceSnapshot() seam before and after each of the six observed owner
 * boundaries. Exact before/after equality defines CHANGED vs NO_OP.
 *
 * This holds the COMPLETE retained legacy state, not a claim that every field currently
 * affects a decision. hasShieldsToPlay, offeredConcedeThisGame, contestedLocations,
 * dangerousLocations, turnsWithFocus, and lastDecisionReason are currently inert or
 * effectively write-only; they remain here because lifecycle calls preserve or clear
 * them and future source drift must be visible.
 *
 * Package-independent by construction: phase and focus arrive as their exact lowercase
 * GamePhase/StrategyFocus .getValue() strings (the enums are bot-package-local, so this
 * common record never depends on either bot package or on .name() vs .getValue()). The
 * confidence rides as raw float bits so equality is exact and NaN-blind. Both location
 * lists preserve their ArrayList order; cardsSeenInReserve is copied from its HashSet
 * into a sorted immutable list so no snapshot equality depends on hash iteration. Every
 * collection is defensively copied and frozen.
 *
 * side is the one nullable component (a controller before setSide has none). No numeric
 * bound is imposed: forceDeficit is legitimately negative when generation exceeds target.
 */
public record StrategyControllerSnapshot(
    Side side,
    boolean underBattleOrderRules,
    boolean hasShieldsToPlay,
    boolean offeredConcedeThisGame,
    String phase,
    int turnNumber,
    int forceGeneration,
    int forceGenerationTarget,
    int forceDeficit,
    String focus,
    int focusConfidenceBits,
    int turnsWithFocus,
    int focusDeployments,
    List<Integer> contestedLocations,
    List<Integer> dangerousLocations,
    int reserveChecksThisTurn,
    List<String> cardsSeenInReserve,
    int lastReserveCheckTurn,
    int battlesWon,
    int battlesLost,
    String lastDecisionReason) {

    public StrategyControllerSnapshot {
        // side is intentionally nullable (a controller before setSide has no side)
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(lastDecisionReason, "lastDecisionReason");
        contestedLocations = orderedIntCopy(contestedLocations, "contestedLocations");
        dangerousLocations = orderedIntCopy(dangerousLocations, "dangerousLocations");
        cardsSeenInReserve = sortedStringCopy(cardsSeenInReserve, "cardsSeenInReserve");
    }

    /** Defensive immutable copy that preserves the source list order exactly. */
    private static List<Integer> orderedIntCopy(List<Integer> values, String name) {
        List<Integer> copy = new ArrayList<>(Objects.requireNonNull(values, name));
        for (Integer value : copy) {
            Objects.requireNonNull(value, name + " element");
        }
        return Collections.unmodifiableList(copy);
    }

    /** Defensive immutable copy sorted into a stable order (the source is a HashSet). */
    private static List<String> sortedStringCopy(List<String> values, String name) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(values, name));
        for (String value : copy) {
            Objects.requireNonNull(value, name + " element");
        }
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }
}
