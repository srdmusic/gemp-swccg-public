package com.gempukku.swccgo.common;

/** Closed engine-owned semantic identity for decision action candidates. */
public enum DecisionActionSemantic {
    UNKNOWN,
    DRAW_CARD_INTO_HAND_FROM_FORCE_PILE,
    PULL_DEPLOY_FROM_PILE,
    PULL_TAKE_INTO_HAND_FROM_PILE;

    public static final String WIRE_PARAMETER = "actionSemantic";

    /** Returns null for missing, blank, or unrecognized wire values. */
    public static DecisionActionSemantic fromWire(String wireValue) {
        if (wireValue == null || wireValue.isBlank()) {
            return null;
        }
        try {
            return valueOf(wireValue);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
