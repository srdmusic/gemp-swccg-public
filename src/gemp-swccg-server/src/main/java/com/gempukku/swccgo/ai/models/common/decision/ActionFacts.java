package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Icon;

import java.util.Objects;
import java.util.Set;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / ACTION FACTS (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md §"Minimal shared model".
//
// One candidate as the engine offered it, at its ORIGINAL ordinal. Candidate
// order is never sorted or rebuilt — DecisionSnapshot enforces ordinal == index.
//
// Field groups per the contract's own bullets:
//  1. original ordinal, action id, action text, card id, blueprint id,
//     testing text, selectable flag (raw candidate-array values; null = the
//     raw decision had no such array for this decision type)
//  2. typed action/card/source/destination references when resolution succeeds
//     — stable id/key strings, NOT mutable game objects (contract: fixtures
//     serialize stable ids and values, not object identity). Failed resolution
//     is an UNKNOWN FactValue with producer + provenance + reason.
//  3. rule-independent measurements: cost, power, ability, presence counts,
//     icons, and weapon-adjusted power COMPONENTS. weaponBonus is a component;
//     weapon-adjusted power = power + weaponBonus is computed downstream and
//     never stored. Deliberately ABSENT per contract: threatLevel,
//     survivabilityEstimate, economicImpact, objectiveAlignment — those are
//     policy scalars owned by domain assessments, not facts.
//
// Java 21 record: no mutators possible; the compact constructor validates and
// re-wraps a KNOWN icons set with Set.copyOf so no mutable set leaks in.
// ═══════════════════════════════════════════════════════════
public record ActionFacts(
        // ── group 1: raw candidate values at original ordinal ──
        int ordinal,
        String actionId,
        String actionText,
        String cardId,
        String blueprintId,
        String testingText,
        boolean selectable,
        // ── group 2: typed references when resolution succeeds (stable keys, no game objects) ──
        FactValue<String> resolvedAction,
        FactValue<String> resolvedCard,
        FactValue<String> resolvedSource,
        FactValue<String> resolvedDestination,
        // ── group 3: rule-independent measurements ──
        FactValue<Float> cost,
        FactValue<Float> power,
        FactValue<Float> ability,
        FactValue<Integer> friendlyPresenceCount,
        FactValue<Integer> opposingPresenceCount,
        FactValue<Set<Icon>> icons,
        FactValue<Float> weaponBonus) {

    public ActionFacts {
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be >= 0, was " + ordinal);
        }
        Objects.requireNonNull(resolvedAction, "resolvedAction");
        Objects.requireNonNull(resolvedCard, "resolvedCard");
        Objects.requireNonNull(resolvedSource, "resolvedSource");
        Objects.requireNonNull(resolvedDestination, "resolvedDestination");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(power, "power");
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(friendlyPresenceCount, "friendlyPresenceCount");
        Objects.requireNonNull(opposingPresenceCount, "opposingPresenceCount");
        Objects.requireNonNull(icons, "icons");
        Objects.requireNonNull(weaponBonus, "weaponBonus");
        // Defensive re-wrap: a KNOWN icons fact must hold an unmodifiable set.
        if (icons.isKnown()) {
            icons = FactValue.known(Set.copyOf(icons.value()), icons.producerId(), icons.provenance());
        }
    }

    /** Builder for readable construction; the compact constructor still validates everything. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int ordinal = -1;
        private String actionId;
        private String actionText;
        private String cardId;
        private String blueprintId;
        private String testingText;
        private boolean selectable;
        private FactValue<String> resolvedAction;
        private FactValue<String> resolvedCard;
        private FactValue<String> resolvedSource;
        private FactValue<String> resolvedDestination;
        private FactValue<Float> cost;
        private FactValue<Float> power;
        private FactValue<Float> ability;
        private FactValue<Integer> friendlyPresenceCount;
        private FactValue<Integer> opposingPresenceCount;
        private FactValue<Set<Icon>> icons;
        private FactValue<Float> weaponBonus;

        private Builder() {}

        public Builder ordinal(int v) { this.ordinal = v; return this; }
        public Builder actionId(String v) { this.actionId = v; return this; }
        public Builder actionText(String v) { this.actionText = v; return this; }
        public Builder cardId(String v) { this.cardId = v; return this; }
        public Builder blueprintId(String v) { this.blueprintId = v; return this; }
        public Builder testingText(String v) { this.testingText = v; return this; }
        public Builder selectable(boolean v) { this.selectable = v; return this; }
        public Builder resolvedAction(FactValue<String> v) { this.resolvedAction = v; return this; }
        public Builder resolvedCard(FactValue<String> v) { this.resolvedCard = v; return this; }
        public Builder resolvedSource(FactValue<String> v) { this.resolvedSource = v; return this; }
        public Builder resolvedDestination(FactValue<String> v) { this.resolvedDestination = v; return this; }
        public Builder cost(FactValue<Float> v) { this.cost = v; return this; }
        public Builder power(FactValue<Float> v) { this.power = v; return this; }
        public Builder ability(FactValue<Float> v) { this.ability = v; return this; }
        public Builder friendlyPresenceCount(FactValue<Integer> v) { this.friendlyPresenceCount = v; return this; }
        public Builder opposingPresenceCount(FactValue<Integer> v) { this.opposingPresenceCount = v; return this; }
        public Builder icons(FactValue<Set<Icon>> v) { this.icons = v; return this; }
        public Builder weaponBonus(FactValue<Float> v) { this.weaponBonus = v; return this; }

        public ActionFacts build() {
            return new ActionFacts(ordinal, actionId, actionText, cardId, blueprintId, testingText,
                    selectable, resolvedAction, resolvedCard, resolvedSource, resolvedDestination,
                    cost, power, ability, friendlyPresenceCount, opposingPresenceCount, icons, weaponBonus);
        }
    }
}
