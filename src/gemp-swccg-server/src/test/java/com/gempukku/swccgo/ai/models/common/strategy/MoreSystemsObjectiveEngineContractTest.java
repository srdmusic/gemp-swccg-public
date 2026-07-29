package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Extension batch (2026-07-29): native engine contract for More Systems
 * Will Rally To Our Cause / The Galaxy Torn Apart (501_19, DARK,
 * playtest set 501, no twins). Card Java unchanged.
 *
 * The law is CONQUEST-driven: flip when two [Separatist] systems are
 * spottable (Card501_019.java L134) — a pure ownership-free count of a
 * FACE-UP-BLUEPRINT icon. Only three faces in the pool print it:
 * Geonosis 501_20 front, Utapau 501_23 BACK, Christophsis 501_26 BACK.
 * Utapau flips to its Separatist back when the DARK player initiates a
 * Force drain (or wins a battle) at an Utapau site (Card501_023.java
 * L44-L56) — so the flip driver must be PLAYED, not placed: this test
 * runs the real drain. The back is the exact negation (L127). Setup
 * deploys all three systems (they may not sit in Reserve) plus Droid
 * Racks. The owner may not deploy non-[Episode I]
 * characters/ships/vehicles — fixture bodies are raw-placed and the drain
 * itself is the table change. No hard-loss on either side.
 */
public class MoreSystemsObjectiveEngineContractTest {

    private static final StartingSetup MSWR = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "501_19");
                put("geonosis", "501_20");
                put("utapau", "501_23");
                put("christophsis", "501_26");
                put("droidRacks", "14_96");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Four required deploys: [Separatist] Geonosis, two [Clone
            // Army] systems (Utapau + Christophsis are the only matches),
            // Droid Racks (an Effect to your side of table).
            for (int i = 0; i < 12; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    if (scn.GetDSCard("geonosis").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("geonosis"));
                    } else if (scn.GetDSCard("utapau").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("utapau"));
                    } else if (scn.GetDSCard("christophsis").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("christophsis"));
                    } else {
                        scn.DSChooseCard(scn.GetDSCard("droidRacks"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario mswrScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("pauCity", "501_24");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                MSWR,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToSystem(
            VirtualTableScenario scn, PhysicalCardImpl site, String system) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, system, null);
        assertFalse("Expected a legal placement at " + system,
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    @Test
    public void mswrFlipsWhenTheRealDrainConquersUtapauAndFactsTrackIt() {
        var scn = mswrScenario();
        var objective = scn.GetDSCard("objective");
        var utapau = scn.GetDSCard("utapau");
        var pauCity = scn.GetDSCard("pauCity");

        scn.StartGame();
        assertFalse(objective.isFlipped());
        // One Separatist system (Geonosis front) exists; the flip needs a
        // second, reachable only by conquest. Stage the drain: Pau City
        // (one Light Force icon) with a lone raw-placed trooper — the
        // owner's non-[Episode I] deploy ban never applies to raw fixture
        // placement, and the DRAIN is the table change.
        moveSiteToSystem(scn, pauCity, Title.Utapau);
        scn.MoveCardsToLocation(pauCity, scn.GetDSFiller(1));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The authored profile must hydrate for 501_19",
                analyzer.isAnalyzed());
        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("The front encodes one onTable count rule", 1,
                preFlip.size());
        assertFalse("One Separatist system leaves the encoded law unmet",
                preFlip.get(0).conditionSatisfied());

        scn.SkipToPhase(Phase.CONTROL);
        assertTrue("The drain at Pau City must be available",
                scn.DSForceDrainAvailable(pauCity));
        scn.DSForceDrainAt(pauCity);
        scn.PassAllResponses();

        assertTrue("The drain initiation must conquer Utapau (flip it to its Separatist back)",
                utapau.isFlipped());
        assertTrue("Two Separatist systems must flip the objective natively",
                objective.isFlipped());

        var postConquest = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Two Separatist systems complete the encoded law",
                postConquest.get(0).conditionSatisfied());
        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one negated count rule", 1,
                postFlip.size());
        assertFalse("With two Separatist systems the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }
}
