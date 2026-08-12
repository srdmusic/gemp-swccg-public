package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleRetentionEngineContractTest {

    @Test
    public void engineSatisfiesDamageAndAttritionInParallel() throws IOException {
        String source = source("gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/effects/ForfeitCardsFromTableSimultaneouslyEffect.java");

        assertTrue(source.contains(
                "_totalBattleDamageToSatisfyMap.put(cardToForfeit"));
        assertTrue(source.contains(
                "_totalAttritionToSatisfyMap.put(cardToForfeit"));
        assertTrue(source.contains(
                "amountToReduceForfeit = Math.max("));
    }

    @Test
    public void engineUsesAllPresentFixedAttritionAndMandatoryForfeits()
            throws IOException {
        String source = source("gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/timing/actions/battle/BattleDamageSegmentAction.java");

        assertTrue(source.contains(
                "float totalAttrition = battleState.getAttritionTotal"));
        assertTrue(source.contains("cardsThatMayNotBeForfeited"));
        assertTrue(source.contains("Filters.mustBeForfeited"));
        assertTrue(source.contains(
                "Filters.mustBeForfeitedBeforeOtherCharacters"));
        assertTrue(source.contains("Filters.not(Filters.character)"));
    }

    @Test
    public void sourceContractsRequireDependencyAndPilotRecalculation()
            throws IOException {
        String leavePlay = source("gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/effects/CardsLeavePlayUtils.java");
        String power = source("gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/modifiers/querying/Power.java");

        assertTrue(leavePlay.contains("cardsToLeavePlay("));
        assertTrue(leavePlay.contains("getAttachedCards(card, true)"));
        assertTrue(power.contains("!isPiloted(gameState, physicalCard, false)"));
    }

    @Test
    public void purePolicyAndPublicReaderCannotReachEngineAttrition()
            throws IOException {
        String policy = source("gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleRetentionPolicy.java");
        String reader = source("gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleRetentionFactsReader.java");

        for (String forbidden : new String[] {
                "com.gempukku.swccgo.game",
                "SwccgGame",
                "GameState",
                "BattleState",
                "PhysicalCard"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
        assertFalse(reader.contains("getTotalAttrition("));
        assertFalse(reader.contains("predictBattle("));
        assertFalse(reader.contains("Knowledge.EXACT"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(repoRoot().resolve("src").resolve(relative));
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve("src/gemp-swccg-server"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate repository root");
    }
}
