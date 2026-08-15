package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CaptureOpponentObjectiveTransferPolicyTest {
    private static StartingSetup tigihSetup() {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                HashMap<String, String> cards = new HashMap<>();
                cards.put("objective", "9_61");
                cards.put("hut", "8_71");
                cards.put("luke", "9_24");
                cards.put("lightsaber", "9_90");
                cards.put("platform", "8_76");
                cards.put("conflict", "9_34");
                return cards;
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        };
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("vader", "1_168");
                    put("imperial", "1_170");
                }},
                20,
                20,
                tigihSetup(),
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void exactSevenEightPressureBoundaryControlsTransfer() {
        PolicyOperation bleedStop = only(
                CaptureObjectivePolicy.scoreTransferLukeToVader(
                        new CaptureObjectivePolicy
                            .TransferLukeToVaderFacts(
                                "transfer", true,
                                true, 7.0f)));

        assertEquals(PolicyOperationKind.ADD, bleedStop.kind());
        assertEquals(
                "OBJECTIVE.TIGIH.OPPONENT_TRANSFER_BLEED_STOP",
                bleedStop.ruleArmId().id());
        assertEquals(
                TraceDomainId.OBJECTIVE_INTENT,
                bleedStop.domainId());
        assertEquals(
                TraceOutputKind.BANDED,
                bleedStop.outputKind());
        assertEquals(300.0f, bleedStop.delta(), 0.0f);

        PolicyOperation lethalHold = only(
                CaptureObjectivePolicy.scoreTransferLukeToVader(
                        new CaptureObjectivePolicy
                            .TransferLukeToVaderFacts(
                                "hold", true,
                                true, 8.0f)));
        assertEquals(
                PolicyOperationKind.ADD,
                lethalHold.kind());
        assertEquals(
                "OBJECTIVE.TIGIH.OPPONENT_TRANSFER_HOLD",
                lethalHold.ruleArmId().id());
        assertEquals(
                TraceDomainId.OBJECTIVE_INTENT,
                lethalHold.domainId());
        assertEquals(
                TraceOutputKind.BANDED,
                lethalHold.outputKind());
        assertEquals(-300.0f, lethalHold.delta(), 0.0f);

        assertTrue(
                CaptureObjectivePolicy.scoreTransferLukeToVader(
                    new CaptureObjectivePolicy
                        .TransferLukeToVaderFacts(
                            "not-legal", false,
                            true, 0.0f))
                    .operations().isEmpty());
        assertTrue(
                CaptureObjectivePolicy.scoreTransferLukeToVader(
                    new CaptureObjectivePolicy
                        .TransferLukeToVaderFacts(
                            "unknown-pressure", true,
                            false, 0.0f))
                    .operations().isEmpty());
    }

    @Test
    public void bhbmOpponentTargetDownloadRequiresExactSourceAndText() {
        PolicyOperation download = only(
                CaptureObjectivePolicy
                    .scoreBhbmOpponentTargetDownload(
                        new CaptureObjectivePolicy
                            .BhbmOpponentTargetDownloadFacts(
                                "download", true)));
        assertEquals(PolicyOperationKind.ADD, download.kind());
        assertEquals(
                "OBJECTIVE.BHBM.OPPONENT_TARGET_DOWNLOAD",
                download.ruleArmId().id());
        assertEquals(300.0f, download.delta(), 0.0f);
        assertTrue(
                CaptureObjectivePolicy
                    .scoreBhbmOpponentTargetDownload(
                        new CaptureObjectivePolicy
                            .BhbmOpponentTargetDownloadFacts(
                                "foreign", false))
                    .operations().isEmpty());

        VirtualTableScenario scn = new VirtualTableScenario(
                new HashMap<>() {{
                    put("other", "1_47");
                }},
                new HashMap<>(),
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.BHBMObjective,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
        scn.StartGame();

        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl other = scn.GetLSCard("other");
        assertTrue(
                CaptureOpponentObjectiveFacts
                    .assessBhbmOpponentTargetDownload(
                        scn.game(), LS, objective,
                        "Deploy Luke from Reserve Deck")
                    .legalObjectiveDownload());
        assertTrue(
                CaptureOpponentObjectiveFacts
                    .assessBhbmOpponentTargetDownload(
                        scn.game(), LS, objective,
                        "Deploy Kanan from Lost Pile")
                    .legalObjectiveDownload());
        assertFalse(
                CaptureOpponentObjectiveFacts
                    .assessBhbmOpponentTargetDownload(
                        scn.game(), LS, objective,
                        "Deploy Emperor from Reserve Deck")
                    .legalObjectiveDownload());
        assertFalse(
                CaptureOpponentObjectiveFacts
                    .assessBhbmOpponentTargetDownload(
                        scn.game(), LS, other,
                        "Deploy Luke from Reserve Deck")
                    .legalObjectiveDownload());
        assertFalse(
                CaptureOpponentObjectiveFacts
                    .assessBhbmOpponentTargetDownload(
                        scn.game(), DS, objective,
                        "Deploy Luke from Reserve Deck")
                    .legalObjectiveDownload());
    }

    @Test
    public void opponentObjectiveFactRequiresExactBackSideBoardLegality() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl conflict = scn.GetLSCard("conflict");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();

        assertFalse("The front side can never arm the back-side action",
                CaptureOpponentObjectiveFacts
                    .isLegalTigihTransferLukeToVader(
                        scn.game(), DS, objective));

        scn.MoveCardsToLocation(hut, imperial);
        scn.SkipToPhase(Phase.CONTROL);
        scn.PassAllResponses();

        assertTrue("The native eligible-Imperial route must flip first",
                objective.isFlipped());
        assertFalse("No present Vader means no legal transfer",
                CaptureOpponentObjectiveFacts
                    .isLegalTigihTransferLukeToVader(
                        scn.game(), DS, objective));

        scn.MoveCardsToLocation(hut, vader);

        CaptureOpponentObjectiveFacts.TigihTransferAssessment
                safeAssessment =
                    CaptureOpponentObjectiveFacts
                        .assessTigihTransferLukeToVader(
                            scn.game(), DS, objective);
        assertTrue(safeAssessment.legal());
        assertTrue(
                safeAssessment
                    .crossoverModifierPressureKnown());
        assertEquals(
                0.0f,
                safeAssessment.crossoverModifierPressure(),
                scn.epsilon);

        scn.StackCardsOn(
                conflict,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3));
        CaptureOpponentObjectiveFacts.TigihTransferAssessment
                lethalAssessment =
                    CaptureOpponentObjectiveFacts
                        .assessTigihTransferLukeToVader(
                            scn.game(), DS, objective);
        assertTrue(lethalAssessment.legal());
        assertTrue(
                lethalAssessment
                    .crossoverModifierPressureKnown());
        assertEquals(
                9.0f,
                lethalAssessment.crossoverModifierPressure(),
                scn.epsilon);
        assertFalse("Only the objective's exact physical source qualifies",
                CaptureOpponentObjectiveFacts
                    .isLegalTigihTransferLukeToVader(
                        scn.game(), DS, conflict));
        assertFalse("The objective owner cannot claim its opponent action",
                CaptureOpponentObjectiveFacts
                    .isLegalTigihTransferLukeToVader(
                        scn.game(), LS, objective));
    }

    @Test
    public void nativeTransferDoesNotRequireEscortOwnedByActingBot() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        scn.MoveCardsToLocation(hut, imperial);
        scn.SkipToPhase(Phase.CONTROL);
        scn.PassAllResponses();
        scn.MoveCardsToLocation(hut, vader);
        imperial.setOwner(LS);

        PhysicalCard nativeCaptiveLuke = Filters.findFirstActive(
                scn.game(),
                objective,
                SpotOverride.INCLUDE_CAPTIVE,
                Filters.and(
                    Filters.Luke,
                    Filters.captiveNotProhibitedFromBeingTransferred,
                    Filters.escortedBy(
                        objective,
                        Filters.and(
                            Filters.Imperial,
                            Filters.except(Filters.Vader)))));
        assertNotNull("The native source has no escort-owner restriction",
                nativeCaptiveLuke);
        assertNotNull("The native source still finds escort-capable Vader",
                Filters.findFirstActive(
                    scn.game(),
                    objective,
                    Filters.and(
                        Filters.Vader,
                        Filters.presentWith(nativeCaptiveLuke),
                        Filters.canEscortCaptive(nativeCaptiveLuke))));
        assertTrue("AI facts must mirror the native escort filter",
                CaptureOpponentObjectiveFacts
                    .isLegalTigihTransferLukeToVader(
                        scn.game(), DS, objective));
    }

    @Test
    public void vaderOwnershipIsDelegatedToNativeEscortCapability()
            throws IOException {
        String nativeSource = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-cards/src/main/java/"
                        + "com/gempukku/swccgo/cards/set9/light/"
                        + "Card9_061_BACK.java"));
        String nativeAction = methodSlice(
                nativeSource,
                "getOpponentsCardGameTextTopLevelActions(",
                "getGameTextTopLevelActions(");
        assertFalse(nativeAction.contains("Filters.owner("));
        assertTrue(nativeAction.contains(
                "Filters.and(Filters.Vader, "
                    + "Filters.presentWith(captiveLuke), "
                    + "Filters.canEscortCaptive(captiveLuke))"));

        String factsSource = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "CaptureOpponentObjectiveFacts.java"));
        String transferFacts = methodSlice(
                factsSource,
                "PhysicalCard captiveLuke =",
                "float modifierPressure =");
        assertFalse("AI facts must not add a bot-owner predicate",
                transferFacts.contains("Filters.owner(playerId)"));
        assertTrue(transferFacts.contains(
                "Filters.canEscortCaptive(captiveLuke)"));
    }

    private static PolicyOperation only(
            com.gempukku.swccgo.ai.models.common.policy.PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static String methodSlice(
            String source,
            String startToken,
            String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("")
                .toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))
                    && Files.isDirectory(cursor.resolve(
                        "src/gemp-swccg-cards/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate repository root");
    }
}
