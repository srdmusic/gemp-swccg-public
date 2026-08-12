package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicImmediateReactAnalyzerTest {

    @Test
    public void publicPilotedReactMoverWithAffordableExactPathIsExposure() {
        VirtualTableScenario darkReact = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("react", "202_15");
                    put("source", "7_273");
                }});
        var target = darkReact.GetLSStartingLocation();
        var source = darkReact.GetDSCard("source");
        var tempestScout = darkReact.GetDSCard("react");
        darkReact.StartGame();
        darkReact.MoveCardsToTopOfDSForcePile(darkReact.GetDSFiller(1));
        darkReact.MoveLocationToTable(source);
        darkReact.MoveCardsToLocation(source, tempestScout);

        PublicImmediateReactAnalyzer.Exposure darkExposure =
                PublicImmediateReactAnalyzer.analyze(
                        darkReact.game(), LS, target, true);
        assertTrue(darkExposure.scanComplete());
        assertTrue(darkExposure.triggerKnowable());
        assertTrue(darkExposure.exposureProven());
        assertEquals(3.0f, darkExposure.strongestMoverEffectivePower(), 0.0f);
        assertEquals(1, darkExposure.provenLegalMoverCount());

        VirtualTableScenario lightReact = hothScenario();
        var hothTarget = lightReact.GetDSStartingLocation();
        var hothSource = lightReact.GetLSCard("source");
        var rogue2 = lightReact.GetLSCard("react");
        lightReact.StartGame();
        lightReact.MoveCardsToTopOfLSForcePile(lightReact.GetLSFiller(1));
        lightReact.MoveLocationToTable(hothSource);
        lightReact.MoveCardsToLocation(hothSource, rogue2);

        PublicImmediateReactAnalyzer.Exposure lightExposure =
                PublicImmediateReactAnalyzer.analyze(
                        lightReact.game(), DS, hothTarget, true);
        assertTrue(lightExposure.scanComplete());
        assertTrue(lightExposure.triggerKnowable());
        assertTrue(lightExposure.exposureProven());
        assertEquals(6.0f, lightExposure.strongestMoverEffectivePower(), 0.0f);
        assertEquals(1, lightExposure.provenLegalMoverCount());
    }

    @Test
    public void ordinaryMobilityAndHiddenReactCardAreNotExposure() {
        VirtualTableScenario scn = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("hidden-react", "202_15");
                    put("ordinary", "3_155");
                    put("source", "7_273");
                }});
        var target = scn.GetLSStartingLocation();
        var source = scn.GetDSCard("source");
        var hiddenReact = scn.GetDSCard("hidden-react");
        var ordinary = scn.GetDSCard("ordinary");
        scn.StartGame();
        scn.MoveLocationToTable(source);
        scn.MoveCardsToLocation(source, ordinary);
        scn.MoveCardsToDSHand(hiddenReact);

        PublicImmediateReactAnalyzer.Exposure exposure =
                PublicImmediateReactAnalyzer.analyze(
                        scn.game(), LS, target, true);
        assertTrue(exposure.scanComplete());
        assertTrue(exposure.triggerKnowable());
        assertFalse(exposure.exposureProven());
        assertEquals(0.0f, exposure.strongestMoverEffectivePower(), 0.0f);
        assertEquals(0, exposure.provenLegalMoverCount());

        scn.MoveCardsToLocation(target, scn.GetDSFiller(2));
        PublicImmediateReactAnalyzer.Exposure contested =
                PublicImmediateReactAnalyzer.analyze(
                        scn.game(), LS, target, true);
        assertTrue(contested.scanComplete());
        assertFalse(contested.triggerKnowable());
        assertFalse(contested.exposureProven());
    }

    @Test
    public void publicOppositeOwnedPermissionSourceIsScanned() {
        VirtualTableScenario scn = new VirtualTableScenario(
                new HashMap<>() {{
                    put("target", "1_127");
                    put("origin", "1_126");
                    put("mover", "1_140");
                }},
                new HashMap<>() {{
                    put("comm", "4_159");
                    put("executor", "4_167");
                }},
                20, 20,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
        var target = scn.GetLSCard("target");
        var origin = scn.GetLSCard("origin");
        var mover = scn.GetLSCard("mover");
        var comm = scn.GetDSCard("comm");
        var executor = scn.GetDSCard("executor");
        scn.StartGame();
        scn.MoveCardsToTopOfLSForcePile(scn.GetLSFiller(1));
        scn.MoveLocationToTable(target);
        scn.MoveLocationToTable(origin);
        scn.MoveLocationToTable(comm);
        scn.MoveCardsToLocation(comm, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(origin, executor, mover);

        PublicImmediateReactAnalyzer.Exposure exposure =
                PublicImmediateReactAnalyzer.analyze(
                        scn.game(), DS, target, true);

        assertTrue(exposure.scanComplete());
        assertTrue(exposure.triggerKnowable());
        assertTrue(exposure.exposureProven());
        assertEquals(5.0f, exposure.strongestMoverEffectivePower(), 0.0f);
        assertEquals(1, exposure.provenLegalMoverCount());
    }

    @Test
    public void unreachableTargetAndNoDrainRowsStayOut() {
        VirtualTableScenario unreachable = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("react", "202_15");
                    put("source", "7_273");
                }});
        var remoteTarget = unreachable.GetDSStartingLocation();
        var source = unreachable.GetDSCard("source");
        var react = unreachable.GetDSCard("react");
        unreachable.StartGame();
        unreachable.MoveLocationToTable(source);
        unreachable.MoveCardsToLocation(source, react);

        PublicImmediateReactAnalyzer.Exposure unreachableExposure =
                PublicImmediateReactAnalyzer.analyze(
                        unreachable.game(), LS, remoteTarget, true);
        assertTrue(unreachableExposure.scanComplete());
        assertFalse(unreachableExposure.exposureProven());

        VirtualTableScenario noDrain = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("react", "202_15");
                    put("source", "7_273");
                    put("no-drain", "2_150");
                }});
        var noDrainTarget = noDrain.GetDSCard("no-drain");
        var noDrainSource = noDrain.GetDSCard("source");
        var noDrainReact = noDrain.GetDSCard("react");
        noDrain.StartGame();
        noDrain.MoveLocationToTable(noDrainSource);
        noDrain.MoveLocationToTable(noDrainTarget);
        noDrain.MoveCardsToLocation(noDrainSource, noDrainReact);

        PublicImmediateReactAnalyzer.Exposure noDrainExposure =
                PublicImmediateReactAnalyzer.analyze(
                        noDrain.game(), LS, noDrainTarget, true);
        assertTrue(noDrainExposure.scanComplete());
        assertFalse(noDrainExposure.triggerKnowable());
        assertFalse(noDrainExposure.exposureProven());
    }

    @Test
    public void additionalReactCostUsesOnlyCurrentPublicForce() {
        VirtualTableScenario oneForce = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("react", "8_171");
                    put("source", "7_273");
                }});
        var target = oneForce.GetLSStartingLocation();
        var source = oneForce.GetDSCard("source");
        var react = oneForce.GetDSCard("react");
        oneForce.StartGame();
        oneForce.MoveCardsToTopOfDSForcePile(oneForce.GetDSFiller(1));
        oneForce.MoveLocationToTable(source);
        oneForce.MoveCardsToLocation(source, react);

        PublicImmediateReactAnalyzer.Exposure unaffordable =
                PublicImmediateReactAnalyzer.analyze(
                        oneForce.game(), LS, target, true);
        assertTrue(unaffordable.scanComplete());
        assertFalse(unaffordable.exposureProven());

        oneForce.MoveCardsToTopOfDSForcePile(oneForce.GetDSFiller(2));
        PublicImmediateReactAnalyzer.Exposure affordable =
                PublicImmediateReactAnalyzer.analyze(
                        oneForce.game(), LS, target, true);
        assertTrue(affordable.exposureProven());
        assertEquals(3.0f,
                affordable.strongestMoverEffectivePower(), 0.0f);
    }

    @Test
    public void unpilotedIntrinsicMoverAndTargetProhibitionStayOut() {
        VirtualTableScenario unpiloted = cloudCityScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("react", "8_169");
                    put("source", "7_273");
                }});
        var target = unpiloted.GetLSStartingLocation();
        var source = unpiloted.GetDSCard("source");
        var bike = unpiloted.GetDSCard("react");
        unpiloted.StartGame();
        unpiloted.MoveCardsToTopOfDSForcePile(unpiloted.GetDSFiller(1));
        unpiloted.MoveLocationToTable(source);
        unpiloted.MoveCardsToLocation(source, bike);
        PublicImmediateReactAnalyzer.Exposure unpilotedExposure =
                PublicImmediateReactAnalyzer.analyze(
                        unpiloted.game(), LS, target, true);
        assertTrue(unpilotedExposure.scanComplete());
        assertFalse(unpilotedExposure.exposureProven());

        VirtualTableScenario prohibited = new VirtualTableScenario(
                new HashMap<>() {{
                    put("derlin", "203_8");
                    put("target", "3_56");
                }},
                new HashMap<>() {{
                    put("react", "202_15");
                    put("source", "3_149");
                }},
                10, 10,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
        var prohibitedTarget = prohibited.GetLSCard("target");
        var hothSource = prohibited.GetDSCard("source");
        var tempest = prohibited.GetDSCard("react");
        prohibited.StartGame();
        prohibited.MoveCardsToTopOfDSForcePile(prohibited.GetDSFiller(1));
        prohibited.MoveLocationToTable(prohibitedTarget);
        prohibited.MoveLocationToTable(hothSource);
        prohibited.MoveCardsToLocation(prohibitedTarget,
                prohibited.GetLSCard("derlin"));
        prohibited.MoveCardsToLocation(hothSource, tempest);

        PublicImmediateReactAnalyzer.Exposure blocked =
                PublicImmediateReactAnalyzer.analyze(
                        prohibited.game(), LS, prohibitedTarget, true);
        assertTrue(blocked.scanComplete());
        assertTrue(blocked.triggerKnowable());
        assertFalse(blocked.exposureProven());

        prohibited.MoveCardsToLocation(hothSource,
                prohibited.GetLSCard("derlin"));
        PublicImmediateReactAnalyzer.Exposure originBlocked =
                PublicImmediateReactAnalyzer.analyze(
                        prohibited.game(), LS, prohibitedTarget, true);
        assertTrue(originBlocked.scanComplete());
        assertTrue(originBlocked.triggerKnowable());
        assertFalse(originBlocked.exposureProven());
    }

    @Test
    public void moverWideReactProhibitionStaysOut() {
        VirtualTableScenario scn = cloudCityScenario(
                new HashMap<>() {{
                    put("blocker", "209_17");
                }},
                new HashMap<>() {{
                    put("react", "202_15");
                    put("source", "7_273");
                }});
        var target = scn.GetLSStartingLocation();
        var source = scn.GetDSCard("source");
        var tempest = scn.GetDSCard("react");
        scn.StartGame();
        scn.MoveCardsToTopOfDSForcePile(scn.GetDSFiller(1));
        scn.MoveLocationToTable(source);
        scn.MoveCardsToLocation(source, tempest);
        scn.AttachCardsTo(tempest, scn.GetLSCard("blocker"));

        PublicImmediateReactAnalyzer.Exposure blocked =
                PublicImmediateReactAnalyzer.analyze(
                        scn.game(), LS, target, true);
        assertTrue(blocked.scanComplete());
        assertTrue(blocked.triggerKnowable());
        assertFalse(blocked.exposureProven());
    }

    @Test
    public void grantedReactTargetFilterMismatchStaysOut() {
        VirtualTableScenario scn = new VirtualTableScenario(
                new HashMap<>() {{
                    put("target", "3_56");
                }},
                new HashMap<>() {{
                    put("officer", "3_92");
                    put("trooper", "3_91");
                    put("source", "3_149");
                }},
                10, 10,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
        var target = scn.GetLSCard("target");
        var source = scn.GetDSCard("source");
        scn.StartGame();
        scn.MoveCardsToTopOfDSForcePile(scn.GetDSFiller(1));
        scn.MoveLocationToTable(target);
        scn.MoveLocationToTable(source);
        scn.MoveCardsToLocation(source,
                scn.GetDSCard("officer"), scn.GetDSCard("trooper"));

        PublicImmediateReactAnalyzer.Exposure mismatch =
                PublicImmediateReactAnalyzer.analyze(
                        scn.game(), LS, target, true);
        assertTrue(mismatch.scanComplete());
        assertTrue(mismatch.triggerKnowable());
        assertFalse(mismatch.exposureProven());
    }

    private static VirtualTableScenario cloudCityScenario(
            HashMap<String, String> lightCards,
            HashMap<String, String> darkCards) {
        return new VirtualTableScenario(
                lightCards, darkCards, 10, 10,
                StartingSetup.LSStartingLocation("7_113"),
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static VirtualTableScenario hothScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("react", "224_23");
                    put("source", "3_63");
                }},
                new HashMap<>(), 10, 10,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.DSStartingLocation("3_144"),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }
}
