package com.gempukku.swccgo.common;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE/CONTROL OPTION 2 / DECISION ORIGIN SEAM (2026-07-13) ═══
// Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_PACKET_2026-07-13.md §1.
//
// The engine-owned identity of WHY a decision was created. The ACTIVATE/CONTROL
// values name the five exact decision creation sites that phase stamps. The PULL
// values name the standard deploy/take child, destination, and failed-verify sites.
// (PlayersPlayPhaseActionsInOrderGameProcess top-level + zero-activation confirm;
// AbstractSwccgCardBlueprint.getCardPilePhaseActions amount + opponent allowance +
// interruption acknowledgement). This is stamped as the single wire parameter
// "decisionOrigin" (name() serialized), so a route resolver never has to infer the
// origin from prompt text.
//
// Each origin declares its REQUIRED wire type. It is held as the AwaitingDecisionType
// name STRING, not the enum, because AwaitingDecisionType lives in gemp-swccg-logic
// and gemp-swccg-common must not depend on logic (dependency runs logic -> common).
// The live resolver validates the stamped decision's real wire type against this
// name; a resolver-side fixture asserts every name parses back to a real
// AwaitingDecisionType, so a rename cannot drift silently.
//
// This enum is a CLOSED set. New values require one exact engine construction site
// and one matching route fixture; prompt text is never an origin substitute.
// ═══════════════════════════════════════════════════════════
public enum DecisionOrigin {
    PHASE_ACTION("CARD_ACTION_CHOICE"),
    ACTIVATE_AMOUNT("INTEGER"),
    ACTIVATE_ALLOWANCE("INTEGER"),
    ACTIVATE_ZERO_CONFIRM("MULTIPLE_CHOICE"),
    ACTIVATE_INTERRUPTION_ACK("MULTIPLE_CHOICE"),
    PULL_DEPLOY_CHILD("ARBITRARY_CARDS"),
    PULL_TAKE_CHILD("ARBITRARY_CARDS"),
    PULL_DESTINATION("CARD_SELECTION"),
    PULL_FAILED_VERIFY("ARBITRARY_CARDS"),
    SETUP_STARTING_LOCATION("ARBITRARY_CARDS"),
    SETUP_STARTING_INTERRUPT("ARBITRARY_CARDS");

    /** The single wire parameter key the engine stamps the origin name into. */
    public static final String WIRE_PARAMETER = "decisionOrigin";

    private final String requiredWireTypeName;

    DecisionOrigin(String requiredWireTypeName) {
        this.requiredWireTypeName = requiredWireTypeName;
    }

    /**
     * The AwaitingDecisionType name this origin must appear as on the wire. Held as a
     * string only because of the common/logic module boundary (see class header).
     */
    public String requiredWireTypeName() {
        return requiredWireTypeName;
    }

    /**
     * Parse a wire value into a typed origin. Returns null when the value is absent or
     * is not one of the closed values (the resolver's "unowned" state). Never
     * throws on an unrecognized value.
     */
    public static DecisionOrigin fromWire(String wireValue) {
        if (wireValue == null) {
            return null;
        }
        for (DecisionOrigin origin : values()) {
            if (origin.name().equals(wireValue)) {
                return origin;
            }
        }
        return null;
    }
}
