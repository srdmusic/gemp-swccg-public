package com.gempukku.swccgo.framework;

public interface Sabacc extends TestBase {

    /**
     * Checks current Sabacc hand total for a player.  Unassigned value cards (wild and clone)
     * have a value of -1 each.
     * @param playerId The player to get the Sabacc total for
     * @return float value of the player's Sabacc total
     */
    default float GetSabaccTotal(String playerId) {
        return game().getModifiersQuerying().getSabaccTotal(game().getGameState(), playerId);
    }

    /**
	 * Checks current Sabacc hand total for DS player.  Unassigned value cards (wild and clone)
	 * have a value of -1 each.
	 * @return float value of DS Sabacc total
	 */
	default float GetDSSabaccTotal() {
        return GetSabaccTotal(DS);
	}

    /**
     * Checks current Sabacc hand total for LS player.  Unassigned value cards (wild and clone)
     * have a value of -1 each.
     * @return float value of LS Sabacc total
     */
    default float GetLSSabaccTotal() {
        return GetSabaccTotal(LS);
    }
}
