package com.gempukku.swccgo.ai.models.common.phase;

/** Closed semantic routes inside the BATTLE decision window. */
public enum BattleWindowRoute {
    LEGACY_UNOWNED,
    INITIATE,
    FIRE,
    ADD_DESTINY,
    TACTIC,
    DELEGATED_MOVE,
    DELEGATED_PULL,
    GENERIC,
    PASS
}
