package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.set5.dark.Card5_099;
import com.gempukku.swccgo.cards.set5.light.Card5_005;
import com.gempukku.swccgo.cards.set13.light.Card13_027;
import com.gempukku.swccgo.cards.set217.dark.Card217_013;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class LegacyLandoSideScopeTest {
    @Test
    public void lightSidePrintingsDoNotInheritLegacySafetyOrDiningRoomBias() {
        for (SwccgCardBlueprint card : new SwccgCardBlueprint[]{
                new Card13_027(), new Card5_005()}) {
            assertFalse(applies(card));
            assertEquals(DeployObjectiveSitingPolicy.LandoSafetyOutcome.NONE,
                    safety(card, 0).outcome());
            assertTrue(safety(card, 0).result().operations().isEmpty());
            assertTrue(DeployObjectiveSitingPolicy.evaluateLandoDestination(
                    new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                            "deploy", applies(card), "Cloud City: Dining Room"))
                    .result().operations().isEmpty());
        }
    }

    @Test
    public void darkSidePrintingsKeepTheirUnsupportedEnemyAndFriendlyBranches() {
        for (SwccgCardBlueprint card : new SwccgCardBlueprint[]{
                new Card5_099(), new Card217_013()}) {
            assertTrue(applies(card));
            assertEquals(DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ENEMY,
                    safety(card, 0).outcome());
            assertEquals(DeployObjectiveSitingPolicy.LandoSafetyOutcome.SAFE_FRIENDLY,
                    safety(card, 1).outcome());
        }
        assertFalse(DeployObjectiveSitingPolicy.isLegacyDarkSideLando(Side.DARK, false));
        assertFalse(DeployObjectiveSitingPolicy.isLegacyDarkSideLando(null, true));
    }

    @Test
    public void bothAdaptersReadCardSideAndPersonaInsteadOfThePromptName() throws Exception {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String source = Files.readString(Path.of("src/main/java/com/gempukku/swccgo/ai/models",
                    bot, "evaluators/CardSelectionEvaluator.java"));
            int start = source.indexOf("SwccgCardBlueprint v312LandoBlueprint =");
            assertTrue(bot, start >= 0);
            String guard = source.substring(start, source.indexOf(
                    "DeployObjectiveSitingPolicy.LandoDestinationEvaluation", start));
            assertTrue(guard.contains("objectiveProgressDeployingCard.getBlueprint()"));
            assertTrue(guard.contains("getBlueprintFromId(context, deployingBlueprintId)"));
            assertTrue(guard.contains("isLegacyDarkSideLando("));
            assertTrue(guard.contains("v312LandoBlueprint.getSide()"));
            assertTrue(guard.contains("v312LandoBlueprint.hasPersona("));
            assertFalse(guard.contains("decisionText.contains(\"lando\")"));
        }
    }

    private static boolean applies(SwccgCardBlueprint card) {
        return DeployObjectiveSitingPolicy.isLegacyDarkSideLando(
                card.getSide(), card.hasPersona(Persona.LANDO));
    }

    private static DeployObjectiveSitingPolicy.LandoSafetyEvaluation safety(
            SwccgCardBlueprint card, int friendlies) {
        return DeployObjectiveSitingPolicy.evaluateLandoSafety(
                new DeployObjectiveSitingPolicy.LandoSafetyFacts(
                        "deploy", applies(card), "Hoth: Defensive Perimeter", friendlies, 1, 3, true));
    }
}
