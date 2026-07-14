package com.gempukku.swccgo.common;

/** Engine-owned parameters that distinguish BATTLE decision subroutes. */
public final class BattleDecisionWire {
    /** True only when attrition remains but every forfeitable card is immune. */
    public static final String OPTIONAL_IMMUNE_FORFEIT = "battleOptionalImmuneForfeit";

    private BattleDecisionWire() {
    }
}
