package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Zone;

import java.util.Objects;
import java.util.Set;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / ACTION FACTS (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md §"Minimal shared model".
// Gate deltas applied: Handoffs/CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md items 1, 3-6.
//
// One candidate as the engine offered it, at its ORIGINAL ordinal. Candidate
// order is never sorted or rebuilt — DecisionSnapshot enforces ordinal == index.
//
// Field groups per the contract's own bullets:
//  1. original ordinal, action id, action text, card id, blueprint id,
//     testing text, selectable flag. The id/text fields are the RAW candidate-
//     array values (null = the raw decision had no such array for this decision
//     type; nonblank when present). selectable is FactValue-wrapped: an absent
//     selectable array is UNKNOWN, never a fabricated false (gate item 3).
//  2. typed action/card/source/destination references when resolution succeeds.
//     Four DISTINCT value types (ActionRef / CardRef / SourceRef /
//     DestinationRef, gate item 1), each carrying its own stable-id semantics,
//     so they are not cross-assignable and never mutable game objects
//     (contract: fixtures serialize stable ids and values, not object
//     identity). Failed resolution is an UNKNOWN FactValue with
//     producer + provenance + reason.
//  3. rule-independent measurements: cost, base power, ability, non-undercover
//     character counts, icons, and the weapon-bonus power COMPONENT.
//     basePower + weaponBonus is the fact boundary (gate item 5): weapon-
//     adjusted power is computed downstream and never stored. If component
//     resolution fails, the builder MUST emit UNKNOWN; converting an exception
//     into a known zero is forbidden. Deliberately ABSENT per contract:
//     threatLevel, survivabilityEstimate, economicImpact, objectiveAlignment.
//     Those are policy scalars owned by domain assessments, not facts.
//
// Java 21 record: no mutators possible; the compact constructor validates and
// re-wraps a KNOWN icons set with Set.copyOf so no mutable set leaks in.
// ═══════════════════════════════════════════════════════════
public record ActionFacts(
        // ── group 1: raw candidate values at original ordinal ──
        int ordinal,
        String actionId,        // nullable; nonblank when present (raw decision actionId array entry)
        String actionText,      // nullable (raw decision actionText array entry)
        String cardId,          // nullable; nonblank when present (raw entry, MAY be a temp id, see resolvedCard)
        String blueprintId,     // nullable; nonblank when present
        String testingText,     // nullable (card title from GEMP)
        FactValue<Boolean> selectable,
        // ── group 2: typed references when resolution succeeds (distinct stable-id types) ──
        FactValue<ActionRef> resolvedAction,
        FactValue<CardRef> resolvedCard,
        FactValue<SourceRef> resolvedSource,
        FactValue<DestinationRef> resolvedDestination,
        // ── group 3: rule-independent measurements ──
        FactValue<Float> cost,
        /** Printed/base power of the resolved card, WITHOUT weapon adjustment. */
        FactValue<Float> basePower,
        FactValue<Float> ability,
        /** Count of friendly non-undercover CHARACTER cards at the candidate
         *  destination. Exact mirror of the runtime measurement
         *  (FormationSafety.countFriendlyNonUndercoverCharacters). This is a raw
         *  card count, NOT the engine's "presence" rule concept. */
        FactValue<Integer> friendlyNonUndercoverCharacterCount,
        /** Same measurement for opposing non-undercover characters. */
        FactValue<Integer> opposingNonUndercoverCharacterCount,
        FactValue<Set<Icon>> icons,
        /** Weapon-bonus power COMPONENT (FormationSafety.weaponBonusOf heuristic:
         *  lightsaber +5, other weapon +3, permanent weapon icon). Kept separate
         *  from basePower; failed resolution is UNKNOWN, never a known 0. */
        FactValue<Float> weaponBonus) {

    public ActionFacts {
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be >= 0, was " + ordinal);
        }
        requireNonBlankWhenPresent(actionId, "actionId");
        requireNonBlankWhenPresent(cardId, "cardId");
        requireNonBlankWhenPresent(blueprintId, "blueprintId");
        Objects.requireNonNull(selectable, "selectable");
        Objects.requireNonNull(resolvedAction, "resolvedAction");
        Objects.requireNonNull(resolvedCard, "resolvedCard");
        Objects.requireNonNull(resolvedSource, "resolvedSource");
        Objects.requireNonNull(resolvedDestination, "resolvedDestination");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(basePower, "basePower");
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(friendlyNonUndercoverCharacterCount, "friendlyNonUndercoverCharacterCount");
        Objects.requireNonNull(opposingNonUndercoverCharacterCount, "opposingNonUndercoverCharacterCount");
        Objects.requireNonNull(icons, "icons");
        Objects.requireNonNull(weaponBonus, "weaponBonus");
        // Count range validation (gate item 6: negative counts rejected).
        requireNonNegativeWhenKnown(friendlyNonUndercoverCharacterCount, "friendlyNonUndercoverCharacterCount");
        requireNonNegativeWhenKnown(opposingNonUndercoverCharacterCount, "opposingNonUndercoverCharacterCount");
        // Defensive re-wrap: a KNOWN icons fact must hold an unmodifiable set.
        if (icons.isKnown()) {
            icons = FactValue.known(Set.copyOf(icons.value()), icons.producerId(), icons.provenance());
        }
    }

    private static void requireNonBlankWhenPresent(String s, String name) {
        if (s != null && s.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank when present");
        }
    }

    private static void requireNonNegativeWhenKnown(FactValue<Integer> fact, String name) {
        if (fact.isKnown() && fact.value() < 0) {
            throw new IllegalArgumentException(name + " must be >= 0 when known, was " + fact.value());
        }
    }

    // ═══ Distinct typed references (gate item 1) ═══
    // Four separate value types so an action id can never be passed where a card
    // id is expected (and vice versa). Compile-time non-assignability is the
    // guarantee; each type also validates its own id form (gate item 6:
    // "nonblank or their native numeric/type form").

    /** Engine-issued action id for one candidate of THIS decision (raw decision
     *  actionId array entry). Decision-scoped, nonblank string form. */
    public record ActionRef(String actionId) {
        public ActionRef {
            Objects.requireNonNull(actionId, "actionId");
            if (actionId.isBlank()) {
                throw new IllegalArgumentException("actionId must be nonblank");
            }
        }
    }

    /** Engine PhysicalCard id in its native int form. Always positive:
     *  GameState.nextCardId() starts at 1. A resolved CardRef means resolution
     *  reached a real PhysicalCard; raw temp ids (see ActionFacts.cardId) never
     *  become a CardRef. */
    public record CardRef(int cardId) {
        public CardRef {
            if (cardId <= 0) {
                throw new IllegalArgumentException("cardId must be positive (engine card ids start at 1), was " + cardId);
            }
        }
    }

    /** Where the candidate card currently is / plays from: either an engine
     *  Zone (deploy from hand, reserve deck, ...) or an on-table card (move
     *  origin location, stacked-on card). Sealed: exactly these two forms. */
    public sealed interface SourceRef {
        record FromZone(Zone zone) implements SourceRef {
            public FromZone {
                Objects.requireNonNull(zone, "zone");
            }
        }
        record FromCard(CardRef card) implements SourceRef {
            public FromCard {
                Objects.requireNonNull(card, "card");
            }
        }
    }

    /** Where the candidate would end up: an on-table card (target location or
     *  attach target) or an engine Zone (side of table, return to hand).
     *  Sealed: exactly these two forms. */
    public sealed interface DestinationRef {
        record ToZone(Zone zone) implements DestinationRef {
            public ToZone {
                Objects.requireNonNull(zone, "zone");
            }
        }
        record ToCard(CardRef card) implements DestinationRef {
            public ToCard {
                Objects.requireNonNull(card, "card");
            }
        }
    }

    /** Builder for readable construction; the compact constructor still validates everything.
     *  NO defaults for any FactValue field: an unset fact fails construction instead of
     *  fabricating a value (gate item 3). */
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
        private FactValue<Boolean> selectable;
        private FactValue<ActionRef> resolvedAction;
        private FactValue<CardRef> resolvedCard;
        private FactValue<SourceRef> resolvedSource;
        private FactValue<DestinationRef> resolvedDestination;
        private FactValue<Float> cost;
        private FactValue<Float> basePower;
        private FactValue<Float> ability;
        private FactValue<Integer> friendlyNonUndercoverCharacterCount;
        private FactValue<Integer> opposingNonUndercoverCharacterCount;
        private FactValue<Set<Icon>> icons;
        private FactValue<Float> weaponBonus;

        private Builder() {}

        public Builder ordinal(int v) { this.ordinal = v; return this; }
        public Builder actionId(String v) { this.actionId = v; return this; }
        public Builder actionText(String v) { this.actionText = v; return this; }
        public Builder cardId(String v) { this.cardId = v; return this; }
        public Builder blueprintId(String v) { this.blueprintId = v; return this; }
        public Builder testingText(String v) { this.testingText = v; return this; }
        public Builder selectable(FactValue<Boolean> v) { this.selectable = v; return this; }
        public Builder resolvedAction(FactValue<ActionRef> v) { this.resolvedAction = v; return this; }
        public Builder resolvedCard(FactValue<CardRef> v) { this.resolvedCard = v; return this; }
        public Builder resolvedSource(FactValue<SourceRef> v) { this.resolvedSource = v; return this; }
        public Builder resolvedDestination(FactValue<DestinationRef> v) { this.resolvedDestination = v; return this; }
        public Builder cost(FactValue<Float> v) { this.cost = v; return this; }
        public Builder basePower(FactValue<Float> v) { this.basePower = v; return this; }
        public Builder ability(FactValue<Float> v) { this.ability = v; return this; }
        public Builder friendlyNonUndercoverCharacterCount(FactValue<Integer> v) { this.friendlyNonUndercoverCharacterCount = v; return this; }
        public Builder opposingNonUndercoverCharacterCount(FactValue<Integer> v) { this.opposingNonUndercoverCharacterCount = v; return this; }
        public Builder icons(FactValue<Set<Icon>> v) { this.icons = v; return this; }
        public Builder weaponBonus(FactValue<Float> v) { this.weaponBonus = v; return this; }

        public ActionFacts build() {
            return new ActionFacts(ordinal, actionId, actionText, cardId, blueprintId, testingText,
                    selectable, resolvedAction, resolvedCard, resolvedSource, resolvedDestination,
                    cost, basePower, ability, friendlyNonUndercoverCharacterCount,
                    opposingNonUndercoverCharacterCount, icons, weaponBonus);
        }
    }
}
