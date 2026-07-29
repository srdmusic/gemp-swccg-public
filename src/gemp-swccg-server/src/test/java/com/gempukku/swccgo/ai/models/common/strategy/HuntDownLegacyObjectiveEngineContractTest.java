package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Twenty-Two (2026-07-29): native engine contract for the LEGACY
 * Hunt Down And Destroy The Jedi (V) / Their Fire Has Gone Out Of The
 * Universe (601_87, DARK). Card Java unchanged. FALSE SIBLING of both
 * classic printings (7_297/213_31): those are Vader-only with Jedi/Luke
 * blockers; 601_87 flips on GALEN OR VADER at a battleground site with no
 * opponent unique character of ability > 3 at any battleground site — a
 * computed class, not a persona class (Card601_087.java L123-L124, using
 * at() which is wider than the printed "present at"). The back flips back
 * on the blocker's return OR when NEITHER hunter is spottable
 * (Card601_087_BACK.java L111-L113 — the second route is ABSENT from the
 * printed text). No hard-loss on either side. The fixture carries NO
 * VADER at all: Galen alone must drive both laws, which the classic
 * profiles (and the text-parsed huntDownNeedsVader static) get wrong.
 */
public class HuntDownLegacyObjectiveEngineContractTest {

    private static final StartingSetup HDADTJ_LEGACY = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "601_87");
                put("coruscant", "7_275");
                put("imperialCity", "7_277");
                put("sithsPlans", "601_73");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required: Coruscant system, Imperial City, A Sith's Plans.
            // The locations auto-deploy without prompts; A Sith's Plans
            // attaches to a SITE and asks for its host — give it Imperial
            // City. Optional If The Trace Was Correct (0-1): decline.
            for (int i = 0; i < 10; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("Trace")) {
                    scn.DSDecided("");
                } else if (scn.DSDecisionAvailable("Sith's Plans")) {
                    scn.DSChooseCard(scn.GetDSCard("imperialCity"));
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    if (scn.GetDSCard("coruscant").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("coruscant"));
                    } else {
                        scn.DSChooseCard(scn.GetDSCard("imperialCity"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario legacyScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "1_19");
                }},
                new HashMap<>() {{
                    put("galen", "601_81");
                }},
                24,
                24,
                // Chasm Walkway (both force icons) is the battleground where
                // everything happens: Imperial City has no Light icon and is
                // NOT a battleground.
                StartingSetup.DefaultLSGroundLocation,
                HDADTJ_LEGACY,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void hdadtjlFrontFlipsOnGalenAloneAndIsBlockedByBigUniqueOpponent() {
        var scn = legacyScenario();
        var objective = scn.GetDSCard("objective");
        var chasm = scn.GetLSStartingLocation();
        var galen = scn.GetDSCard("galen");
        var luke = scn.GetLSCard("luke");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Galen at the battleground with Luke (unique, ability 4) alongside:
        // the actor leg holds but the blocker leg blocks.
        scn.MoveCardsToLocation(chasm, galen);
        scn.MoveCardsToLocation(chasm, luke);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("A unique ability-4 opponent at a battleground must block the flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(luke);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Galen ALONE — no Vader anywhere in the deck — must flip",
                objective.isFlipped());
    }

    @Test
    public void hdadtjlBackFlipsBackOnBlockerReturnOrOnLosingBothHunters() {
        var scn = legacyScenario();
        var objective = scn.GetDSCard("objective");
        var chasm = scn.GetLSStartingLocation();
        var galen = scn.GetDSCard("galen");
        var luke = scn.GetLSCard("luke");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);
        var pulseThree = scn.GetDSFiller(3);
        var pulseFour = scn.GetDSFiller(4);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree, pulseFour);
        scn.StartGame();
        scn.MoveCardsToLocation(chasm, galen);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Galen at a battleground with no blocker must flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(chasm, luke);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("The blocker's return must flip the back to front (route 1)",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(luke);
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertTrue("Removing the blocker must flip forward again",
                objective.isFlipped());
        scn.LSPass();

        // The unprinted second route, straight from the code: with NEITHER
        // Galen nor Vader spottable, the back flips to front.
        scn.MoveOutOfPlay(galen);
        scn.DSDeployCardAndPassResponses(
                pulseFour, scn.GetLSStartingLocation());
        assertFalse("Losing both hunters must flip the back to front (unprinted route 2)",
                objective.isFlipped());
    }

    @Test
    public void hdadtjlProfileRulesTrackTheEngineLawNotTheClassicHijack() {
        var scn = legacyScenario();
        var chasm = scn.GetLSStartingLocation();
        var galen = scn.GetDSCard("galen");
        var luke = scn.GetLSCard("luke");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 601_87", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("The legacy front encodes one two-leg rule", 1,
                preFlip.size());
        assertFalse("With no hunter on table the encoded law is unmet",
                preFlip.get(0).conditionSatisfied());

        // Galen alone, NO Vader: the hijacked classic profile (Vader-only)
        // would leave this unmet — the authored legacy profile must not.
        scn.MoveCardsToLocation(chasm, galen);
        var galenOnly = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Galen alone completes the encoded law (no Vader required)",
                galenOnly.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(chasm, luke);
        var blocked = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertFalse("A unique ability-4 opponent at a battleground blocks the encoded law",
                blocked.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one two-route anyOf rule", 1,
                postFlip.size());
    }
}
