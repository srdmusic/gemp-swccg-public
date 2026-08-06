package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Batch Twenty (2026-07-27): native engine contract for The Hidden Path /
 * Gather Allies And Train (226_28, LIGHT, no twin printings). Card Java
 * unchanged.
 *
 * Law (Card226_028.java L151): flips when YOUR Jedi occupy two non-Mapuzo
 * sites — occupiesWith count 2 (at-least), presence not control, owner's
 * Jedi only, undercover excluded, IEFB. Jedi is the computed class (LIGHT
 * character with ability >= 6), not the Jedi_Survivor keyword; battleground
 * is NOT required. Back (Card226_028_BACK.java L124): the exact negation —
 * flips back below two. No hard-loss on either side. Fixture Jedi: Obi-Wan
 * 226_24 sits exactly on the ability-6 boundary; Quinlan Vos 226_26 is 7.
 * Jabiim: Path Operations Center 226_15 is a NON-battleground non-Mapuzo
 * site, proving plain sites satisfy the gate.
 */
public class HiddenPathObjectiveEngineContractTest {

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(VirtualTableScenario scn) {
            var decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.LS);
            assertNotNull("Expected Light Side decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            assertEquals("Rando/Chosen parity for " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private static final StartingSetup HIDDEN_PATH = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "226_28");
                put("village", "226_21");
                put("safehouse", "226_22");
                put("corridor", "226_23");
                put("fallenOrder", "226_14");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Four required free deploys: the three Mapuzo sites, then the
            // Fallen Order Epic Event to the owner's side of table. Each
            // filter is single-match in this deck; answer side placement
            // and any single-card choice prompts as they appear.
            for (int i = 0; i < 24; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    if (scn.GetLSCard("village").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("village"));
                    } else if (scn.GetLSCard("safehouse").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("safehouse"));
                    } else if (scn.GetLSCard("corridor").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("corridor"));
                    } else {
                        scn.LSChooseCard(scn.GetLSCard("fallenOrder"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario hiddenPathScenario() {
        return hiddenPathScenario(
                StartingSetup.DefaultDSGroundLocation);
    }

    private VirtualTableScenario hiddenPathRouteScenario() {
        return hiddenPathScenario(StartingSetup.DoNothingSetup);
    }

    private VirtualTableScenario hiddenPathRelocationScenario() {
        return hiddenPathScenario(
                StartingSetup.DefaultDSGroundLocation,
                new HashMap<>() {{
                    put("plaza", "7_270");
                }});
    }

    private VirtualTableScenario hiddenPathForceLossScenario() {
        return hiddenPathScenario(
                StartingSetup.DefaultDSGroundLocation,
                new HashMap<>() {{
                    put("stormtrooper", "1_194");
                }});
    }

    private VirtualTableScenario hiddenPathHostileRouteScenario() {
        return hiddenPathScenario(
                StartingSetup.DoNothingSetup,
                new HashMap<>() {{
                    put("lightsaber", "1_155");
                }},
                new HashMap<>() {{
                    put("vader", "1_168");
                }});
    }

    private VirtualTableScenario hiddenPathScenario(
            StartingSetup darkSetup) {
        return hiddenPathScenario(darkSetup, new HashMap<>());
    }

    private VirtualTableScenario hiddenPathScenario(
            StartingSetup darkSetup,
            HashMap<String, String> darkCards) {
        return hiddenPathScenario(
                darkSetup, new HashMap<>(), darkCards);
    }

    private VirtualTableScenario hiddenPathScenario(
            StartingSetup darkSetup,
            HashMap<String, String> extraLightCards,
            HashMap<String, String> darkCards) {
        HashMap<String, String> lightCards = new HashMap<>() {{
            put("kelleran", "226_18");
            put("obiwan", "226_24");
            put("quinlan", "226_26");
            put("poc", "226_15");
            put("hangar", "226_16");
            put("holocron", "216_31");
            put("lossFodder", "1_28");
        }};
        lightCards.putAll(extraLightCards);
        return new VirtualTableScenario(
                lightCards,
                darkCards,
                24,
                24,
                HIDDEN_PATH,
                darkSetup,
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

    /**
     * Fallen Order makes the flip pulse noisy: pre-flip it cancels every
     * Jedi Survivor's game text, and the flip drops its Hidden-Path title
     * gate, so the flip spawns required RESTORE triggers for every survivor
     * on table (an ordering decision when more than one), followed by a
     * RESTORED_GAME_TEXT optional-response window. All orderings reach the
     * same state — answer required windows with the first action, pass
     * optional windows, until the deploy menu returns.
     */
    private void resolveRequiredWindows(VirtualTableScenario scn) {
        for (int i = 0; i < 8; i++) {
            var decision = scn.LSGetDecision();
            if (decision != null && decision.getText() != null
                    && decision.getText().contains("Required responses")) {
                scn.PlayerDecided(VirtualTableScenario.LS, "0");
                continue;
            }
            var current = scn.GetCurrentDecision();
            if (current != null && current.getText() != null
                    && current.getText().toLowerCase().contains("optional response")) {
                scn.PassAllResponses();
                continue;
            }
            return;
        }
    }

    private void keepOnlyLightHandCards(
            VirtualTableScenario scn, PhysicalCard... kept) {
        List<PhysicalCard> keep = List.of(kept);
        for (PhysicalCard card : new ArrayList<>(
                scn.gameState().getHand(VirtualTableScenario.LS))) {
            if (!keep.contains(card)) {
                scn.MoveCardsToBottomOfLSReserveDeck(
                        (PhysicalCardImpl) card);
            }
        }
    }

    private PhysicalCard deployNextSurvivorWithBothBots(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl fallenOrder, PhysicalCardImpl safehouse) {
        resolveRequiredWindows(scn);
        scn.PassAllResponses();
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        String deployAction = scn.GetCardActionId(
                VirtualTableScenario.LS, fallenOrder,
                "Deploy a Jedi Survivor stacked here");
        assertNotNull("Fallen Order must offer its stacked Survivor action",
                deployAction);
        assertEquals("The objective route must beat the holocron/device step",
                deployAction, bots.decideBoth(scn));
        scn.LSDecided(deployAction);

        String survivorChoice = bots.decideBoth(scn);
        PhysicalCard survivor = scn.gameState().findCardById(
                Integer.parseInt(survivorChoice));
        assertTrue("The child must be one of the three physical Survivors",
                survivor == scn.GetLSCard("kelleran")
                    || survivor == scn.GetLSCard("obiwan")
                    || survivor == scn.GetLSCard("quinlan"));
        scn.LSDecided(survivorChoice);
        scn.PassAllResponses();

        if (survivor.getZone() == Zone.STACKED) {
            String destination = bots.decideBoth(scn);
            assertEquals("Every front-side Survivor must deploy to Safehouse",
                    Integer.toString(safehouse.getCardId()), destination);
            scn.LSDecided(destination);
        }
        scn.PassAllResponses();
        resolveRequiredWindows(scn);
        scn.PassAllResponses();
        assertEquals(Zone.AT_LOCATION, survivor.getZone());
        assertEquals(safehouse,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), survivor));
        return survivor;
    }

    private PhysicalCard pullJabiimWithBothBots(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl objective,
            PhysicalCardImpl poc, PhysicalCardImpl hangar) {
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        String jabiimAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy a Jabiim location");
        assertNotNull("The objective must offer its Jabiim route",
                jabiimAction);
        var parentDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertEquals("The Jabiim route must lead the deploy hierarchy",
                jabiimAction, bots.decideBoth(scn));
        assertEquals("The real action source must be the physical objective",
                objective,
                AiActionSourceProvenance.selectedActionSource(
                        parentDecision, jabiimAction));
        scn.LSDecided(jabiimAction);

        var siteDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String siteChoice = bots.decideBoth(scn);
        List<String> offeredIds = List.of(
                siteDecision.getDecisionParameters().get("cardId"));
        int selectedIndex = offeredIds.indexOf(siteChoice);
        assertTrue("Expected an offered Jabiim site choice, got "
                + siteChoice, selectedIndex >= 0);
        String selectedBlueprint = siteDecision
                .getDecisionParameters().get("blueprintId")[selectedIndex];
        PhysicalCardImpl selected;
        if (selectedBlueprint.equals(poc.getBlueprintId(true))) {
            selected = poc;
        } else {
            assertEquals("Only the two physical Jabiim sites are legal",
                    hangar.getBlueprintId(true), selectedBlueprint);
            selected = hangar;
        }
        scn.LSDecided(siteChoice);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        } else if (scn.LSDecisionAvailable("next to (or convert)")) {
            PhysicalCardImpl existing = selected == poc ? hangar : poc;
            assertTrue("The other Jabiim site must already be in play",
                    existing.getZone() != null
                        && existing.getZone().isInPlay());
            scn.LSChooseCard(existing);
            scn.PassAllResponses();
        }
        return selected;
    }

    private PhysicalCard moveSafehouseSurvivorToCorridorWithBothBots(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl corridor) {
        scn.PassAllResponses();
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();
        var moveDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String moveAction = bots.decideBoth(scn);
        PhysicalCard mover = AiActionSourceProvenance
                .selectedActionSource(moveDecision, moveAction);
        assertNotNull("A physical Survivor must supply the landspeed action",
                mover);
        scn.LSDecided(moveAction);
        String destination = bots.decideBoth(scn);
        assertEquals("Hidden Path Survivors must walk Safehouse to Corridor",
                Integer.toString(corridor.getCardId()), destination);
        scn.LSDecided(destination);
        scn.PassAllResponses();
        assertEquals(corridor,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), mover));
        return mover;
    }

    private record CorridorTransit(
            PhysicalCard mover, PhysicalCard destination) { }

    private String chooseBattleForfeitBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            PhysicalCard... candidates) {
        List<String> cardIds = new ArrayList<>();
        List<String> blueprints = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<Boolean> selectable = new ArrayList<>();
        for (PhysicalCard candidate : candidates) {
            cardIds.add(Integer.toString(candidate.getCardId()));
            blueprints.add(candidate.getBlueprintId(true));
            titles.add(candidate.getTitle());
            selectable.add(true);
        }

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION",
                        "Choose a card from battle to forfeit",
                        "hidden-path-battle-forfeit", Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.LIGHT);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setTestingTexts(titles);
        randoContext.setSelectable(selectable);
        randoContext.setNoPass(true);
        randoContext.setMin(1);
        randoContext.setMax(1);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION",
                        "Choose a card from battle to forfeit",
                        "hidden-path-battle-forfeit", Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.LIGHT);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setTestingTexts(titles);
        chosenContext.setSelectable(selectable);
        chosenContext.setNoPass(true);
        chosenContext.setMin(1);
        chosenContext.setMax(1);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals("Rando/Chosen forfeit parity",
                rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return rando.getActionId();
    }

    private CorridorTransit transitCorridorSurvivorWithBothBots(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl corridor,
            PhysicalCardImpl poc, PhysicalCardImpl hangar) {
        scn.PassAllResponses();
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();
        String corridorAction = scn.GetCardActionId(
                VirtualTableScenario.LS, corridor,
                "Move Jedi Survivor here to a site");
        assertNotNull("Underground Corridor must offer its paid transit",
                corridorAction);
        var parentDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertEquals("The paid Corridor route must beat ordinary movement",
                corridorAction, bots.decideBoth(scn));
        assertEquals(corridor,
                AiActionSourceProvenance.selectedActionSource(
                        parentDecision, corridorAction));
        scn.LSDecided(corridorAction);

        assertEquals("The action must originate at the physical Corridor",
                Integer.toString(corridor.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(corridor.getCardId()));

        String destinationChoice = bots.decideBoth(scn);
        PhysicalCard destination = scn.gameState().findCardById(
                Integer.parseInt(destinationChoice));
        assertTrue("The route destination must be one of the two Jabiim sites",
                destination == poc || destination == hangar);
        scn.LSDecided(destinationChoice);

        String survivorChoice = bots.decideBoth(scn);
        var survivorDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        var liveState = scn.gameState().getTopGameTextActionState();
        var liveAction = liveState != null
                ? liveState.getGameTextAction() : null;
        assertFalse("The public bot must choose the final Survivor child; decision="
                        + (survivorDecision != null
                            ? survivorDecision.getText() : "null")
                        + " live="
                        + (liveAction != null
                            ? liveAction.getText() : "null"),
                survivorChoice.isEmpty());
        PhysicalCard mover = scn.gameState().findCardById(
                Integer.parseInt(survivorChoice));
        assertNotNull("The route must choose a physical Jedi Survivor", mover);
        scn.LSDecided(survivorChoice);
        scn.PassAllResponses();
        resolveRequiredWindows(scn);
        scn.PassAllResponses();
        assertEquals(destination,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), mover));
        return new CorridorTransit(mover, destination);
    }

    @Test
    public void thpFrontNeedsTwoNonMapuzoSitesEachWithYourJedi() {
        var scn = hiddenPathScenario();
        var objective = scn.GetLSCard("objective");
        var safehouse = scn.GetLSCard("safehouse");
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        // Two active near misses: a Jedi at a MAPUZO site never counts, and
        // a non-Jedi trooper at a legal site never counts. Only Obi-Wan
        // (ability exactly 6 — the boundary) at Marketplace is live.
        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(safehouse, quinlan);
        scn.MoveCardsToLocation(poc, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A Jedi on Mapuzo and a non-Jedi off Mapuzo must not flip",
                objective.isFlipped());
        scn.DSPass();

        // Quinlan leaves Mapuzo for the NON-battleground Jabiim site:
        // plain non-Mapuzo sites satisfy the gate.
        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two non-Mapuzo sites each with your Jedi must flip (battleground not required)",
                objective.isFlipped());
    }

    @Test
    public void thpBackFlipsBackBelowTwoAndReFlipsSymmetrically() {
        var scn = hiddenPathScenario();
        var objective = scn.GetLSCard("objective");
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);
        var pulseThree = scn.GetLSFiller(3);

        var pulseFour = scn.GetLSFiller(4);
        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree, pulseFour);
        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        // Quinlan starts on Mapuzo so Fallen Order's cancel trigger
        // resolves on the first pulse, before the flip pulse.
        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(scn.GetLSCard("safehouse"), quinlan);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertFalse("One Jedi-occupied non-Mapuzo site must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertTrue("Two Jedi-occupied non-Mapuzo sites must flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertFalse("Losing the second Jedi site must flip the back to front (exact negation)",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseFour, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertTrue("Restoring the second Jedi site must flip again (symmetric law)",
                objective.isFlipped());
    }

    @Test
    public void thpProfileRulesTrackTheEngineLaw() {
        var scn = hiddenPathScenario();
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 226_28", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("THP front encodes one occupyWith rule", 1,
                preFlip.size());
        assertFalse("With no Jedi placed the encoded law is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(poc, quinlan);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Two Jedi at two non-Mapuzo sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one negated hold rule", 1,
                postFlip.size());
        assertFalse("With both sites held the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }

    @Test
    public void thpBackProtectsTheLastJediAtEachHeldSiteForBothBots() {
        var scn = hiddenPathScenario();
        var objective = scn.GetLSCard("objective");
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var nonJediCompanion = scn.GetLSFiller(1);
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(), obiwan, nonJediCompanion);
        scn.MoveCardsToLocation(scn.GetLSCard("safehouse"), quinlan);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertFalse(objective.isFlipped());
        scn.DSPass();
        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertTrue("Fixture must reach the real back face",
                objective.isFlipped());

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertTrue("The exact non-battleground Jabiim hold is protected",
                rando.isFlipBackProtectionLocation(
                        poc, scn.game(), VirtualTableScenario.LS));
        assertEquals("Chosen One must consume the same back rule",
                rando.isFlipBackProtectionLocation(
                        poc, scn.game(), VirtualTableScenario.LS),
                chosen.isFlipBackProtectionLocation(
                        poc, scn.game(), VirtualTableScenario.LS));
        assertTrue("A non-Jedi companion cannot hide the last required Jedi",
                rando.wouldDepartureTriggerFlipBack(
                        scn.game(), VirtualTableScenario.LS, obiwan));
        assertEquals("Departure protection must be mirrored",
                rando.wouldDepartureTriggerFlipBack(
                        scn.game(), VirtualTableScenario.LS, obiwan),
                chosen.wouldDepartureTriggerFlipBack(
                        scn.game(), VirtualTableScenario.LS, obiwan));
        assertEquals(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, obiwan));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, obiwan),
                chosen.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, obiwan));
        assertEquals("Battle loss must forfeit the non-Jedi before the singleton hold Jedi",
                Integer.toString(nonJediCompanion.getCardId()),
                chooseBattleForfeitBoth(
                    scn, rando, chosen,
                    obiwan, nonJediCompanion));
    }

    @Test
    public void publicForceLossPreservesTheJediAndJabiimRoute() {
        var scn = hiddenPathForceLossScenario();
        var poc = scn.GetLSCard("poc");
        var kelleran = scn.GetLSCard("kelleran");
        var fodder = scn.GetLSCard("lossFodder");

        scn.StartGame();
        scn.MoveCardsToLSHand(poc, kelleran, fodder);
        keepOnlyLightHandCards(scn, poc, kelleran, fodder);
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(),
                scn.GetDSCard("stormtrooper"));

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                    .REQUIRED_LOCATION,
                analyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.LS, poc));
        assertEquals(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                    .REQUIRED_ACTOR,
                analyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.LS, kelleran));

        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.DSForceDrainAt(scn.GetDSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose Force to lose"));
        String loss = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("Keep the still-needed Jabiim route card",
                Integer.toString(poc.getCardId()).equals(loss));
        assertFalse("Keep the still-needed Jedi actor",
                Integer.toString(kelleran.getCardId()).equals(loss));
        scn.LSDecided(loss);
        scn.PassAllResponses();
        assertEquals(Zone.HAND, poc.getZone());
        assertEquals(Zone.HAND, kelleran.getZone());
    }

    @Test
    public void publicCorridorTransitRejectsTheHostileJabiimSite() {
        var scn = hiddenPathHostileRouteScenario();
        var objective = scn.GetLSCard("objective");
        var corridor = scn.GetLSCard("corridor");
        var poc = scn.GetLSCard("poc");
        var hangar = scn.GetLSCard("hangar");
        var kelleran = scn.GetLSCard("kelleran");
        var vader = scn.GetDSCard("vader");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        moveSiteToSystem(scn, hangar, Title.Jabiim);
        scn.MoveCardsToLocation(corridor, kelleran);
        scn.MoveCardsToLocation(hangar, vader);
        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.MOVE);
        while (scn.GetLSForcePileCount() > 1) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.GetLSForcePileCount() < 1) {
            scn.LSActivateForceCheat(1);
        }
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();

        var bots = PublicBots.forGame(scn);
        String corridorAction = scn.GetCardActionId(
                VirtualTableScenario.LS, corridor,
                "Move Jedi Survivor here to a site");
        assertNotNull(corridorAction);
        assertEquals(corridorAction, bots.decideBoth(scn));
        scn.LSDecided(corridorAction);
        assertEquals(Integer.toString(corridor.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(corridor.getCardId()));
        assertEquals("Use the empty Jabiim route, never the Vader-held one",
                Integer.toString(poc.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(poc.getCardId()));
        assertEquals(Integer.toString(kelleran.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(kelleran.getCardId()));
        scn.PassAllResponses();
        resolveRequiredWindows(scn);
        assertEquals(poc,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), kelleran));
        assertEquals(0, scn.GetLSForcePileCount());
        assertFalse(objective.isFlipped());
    }

    @Test
    public void v62RequiresAnOwnedJediAndNetDistinctSiteProgress() {
        var scn = hiddenPathRouteScenario();
        var corridor = scn.GetLSCard("corridor");
        var poc = scn.GetLSCard("poc");
        var hangar = scn.GetLSCard("hangar");
        var obiwan = scn.GetLSCard("obiwan");
        var nonJedi = scn.GetLSCard("lossFodder");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        moveSiteToSystem(scn, hangar, Title.Jabiim);
        scn.MoveCardsToLocation(corridor, nonJedi);
        scn.MoveCardsToLocation(hangar, obiwan);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertFalse("A non-Jedi mover cannot earn fake Hidden Path progress",
                analyzer.advancesHiddenPathDistinctHoldSiteByMovingTo(
                    scn.game(), VirtualTableScenario.LS,
                    nonJedi, poc));
        assertFalse("Moving the sole held Jedi from one qualifying site to another is zero net progress",
                analyzer.advancesHiddenPathDistinctHoldSiteByMovingTo(
                    scn.game(), VirtualTableScenario.LS,
                    obiwan, poc));

        scn.MoveCardsToLocation(corridor, obiwan);
        assertTrue("A Jedi leaving Mapuzo for an empty non-Mapuzo site is real progress",
                analyzer.advancesHiddenPathDistinctHoldSiteByMovingTo(
                    scn.game(), VirtualTableScenario.LS,
                    obiwan, poc));
    }

    @Test
    public void publicCorridorFinalChildChoosesTheSurvivorSafeForItsSelectedDestination() {
        var scn = hiddenPathHostileRouteScenario();
        var corridor = scn.GetLSCard("corridor");
        var poc = scn.GetLSCard("poc");
        var kelleran = scn.GetLSCard("kelleran");
        var obiwan = scn.GetLSCard("obiwan");
        var lightsaber = scn.GetLSCard("lightsaber");
        var lossFodder = scn.GetLSCard("lossFodder");
        var vader = scn.GetDSCard("vader");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        scn.MoveCardsToLocation(corridor, kelleran, obiwan);

        List<PhysicalCard> offeredSurvivors = new ArrayList<>(
                Filters.filterActive(
                    scn.game(), null,
                    SpotOverride.INCLUDE_UNDERCOVER,
                    Filters.and(
                        Filters.owner(VirtualTableScenario.LS),
                        Filters.Jedi_Survivor,
                        Filters.hasNotPerformedRegularMove,
                        Filters.here(corridor))));
        assertEquals(2, offeredSurvivors.size());
        PhysicalCard unsafeSurvivor = offeredSurvivors.get(0);
        PhysicalCard safeSurvivor = offeredSurvivors.get(1);
        scn.AttachCardsTo(
                (PhysicalCardImpl) safeSurvivor, lightsaber);

        scn.MoveCardsToLocation(poc, lossFodder, vader);
        for (int i = 1; i <= 7; i++) {
            scn.MoveCardsToLocation(poc, scn.GetDSFiller(i));
        }

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertFalse("The first offered Survivor must be unsafe for POC",
                analyzer.isHiddenPathCorridorTransitFormationSafe(
                    scn.game(), VirtualTableScenario.LS,
                    unsafeSurvivor, poc));
        assertTrue("The armed Survivor must be safe for POC",
                analyzer.isHiddenPathCorridorTransitFormationSafe(
                    scn.game(), VirtualTableScenario.LS,
                    safeSurvivor, poc));

        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.MOVE);
        while (scn.GetLSForcePileCount() > 1) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();

        var bots = PublicBots.forGame(scn);
        String corridorAction = scn.GetCardActionId(
                VirtualTableScenario.LS, corridor,
                "Move Jedi Survivor here to a site");
        assertNotNull(corridorAction);
        assertEquals(corridorAction, bots.decideBoth(scn));
        scn.LSDecided(corridorAction);
        assertEquals(Integer.toString(corridor.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(corridor.getCardId()));
        assertEquals(Integer.toString(poc.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(poc.getCardId()));
        assertEquals("The final child must preserve destination-specific safety",
                Integer.toString(safeSurvivor.getCardId()),
                bots.decideBoth(scn));
        scn.LSDecided(Integer.toString(safeSurvivor.getCardId()));
        scn.PassAllResponses();
        resolveRequiredWindows(scn);

        assertEquals(poc,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), safeSurvivor));
        assertEquals(corridor,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), unsafeSurvivor));
        assertEquals(0, scn.GetLSForcePileCount());
    }

    @Test
    public void publicBotsRefuseRelocationThatWouldFlipTheObjectiveBack() {
        var scn = hiddenPathRelocationScenario();
        var objective = scn.GetLSCard("objective");
        var poc = scn.GetLSCard("poc");
        var village = scn.GetLSCard("village");
        var marketplace = scn.GetDSStartingLocation();
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulse = scn.GetLSFiller(1);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        scn.SkipToLSTurn(Phase.DEPLOY);
        moveSiteToSystem(scn, poc, Title.Jabiim);
        scn.MoveCardsToLocation(poc, quinlan);
        scn.MoveCardsToLocation(marketplace, obiwan);
        scn.LSActivateForceCheat(12);
        scn.LSDeployCardAndPassResponses(
                pulse, marketplace);
        resolveRequiredWindows(scn);
        assertTrue("Fixture must reach Gather Allies And Train",
                objective.isFlipped());

        var relocationAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        relocationAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var mapuzoTrap = relocationAnalyzer
                .assessHiddenPathRelocation(
                    scn.game(), VirtualTableScenario.LS,
                    quinlan, village);
        assertTrue("A Jabiim Jedi may legally relocate to this Mapuzo battleground",
                mapuzoTrap.legal());
        assertFalse("Mapuzo may advance the payoff but cannot replace a required hold site",
                mapuzoTrap.preservesHold());
        assertEquals(1, mapuzoTrap.holdSitesAfter());
        assertEquals(1, mapuzoTrap.battlegroundGain());
        assertFalse("No offered relocation may trade away the flipped objective",
                relocationAnalyzer.hasUsefulHiddenPathRelocation(
                    scn.game(), VirtualTableScenario.LS));

        while (scn.GetLSForcePileCount() > 2) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.GetLSForcePileCount() < 2) {
            scn.LSActivateForceCheat(
                    2 - scn.GetLSForcePileCount());
        }
        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();
        String relocate = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Relocate a Jedi");
        assertNotNull("The unsafe native relocation must still be offered",
                relocate);
        var parent = scn.GetAwaitingDecision(VirtualTableScenario.LS);
        assertEquals(objective,
                AiActionSourceProvenance.selectedActionSource(
                    parent, relocate));
        assertFalse("Every offered relocation collapses the two-site hold, so never select it",
                relocate.equals(
                    PublicBots.forGame(scn).decideBoth(scn)));
        assertEquals(2, scn.GetLSForcePileCount());
        assertTrue(objective.isFlipped());
    }

    @Test
    public void publicBotsRelocateThroughAllChildrenAndKeepTheBackFlipped() {
        var scn = hiddenPathRelocationScenario();
        var objective = scn.GetLSCard("objective");
        var fallenOrder = scn.GetLSCard("fallenOrder");
        var village = scn.GetLSCard("village");
        var poc = scn.GetLSCard("poc");
        var marketplace = scn.GetDSStartingLocation();
        var kelleran = scn.GetLSCard("kelleran");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulse = scn.GetLSFiller(1);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        scn.SkipToLSTurn(Phase.DEPLOY);
        moveSiteToSystem(scn, poc, Title.Jabiim);
        scn.MoveCardsToLocation(poc, kelleran, quinlan);
        scn.MoveCardsToLocation(marketplace, obiwan);
        scn.LSActivateForceCheat(12);
        scn.LSDeployCardAndPassResponses(
                pulse, marketplace);
        resolveRequiredWindows(scn);
        assertTrue("Fixture must reach Gather Allies And Train",
                objective.isFlipped());

        while (scn.GetLSForcePileCount() > 2) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.GetLSForcePileCount() < 2) {
            scn.LSActivateForceCheat(
                    2 - scn.GetLSForcePileCount());
        }
        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();
        var bots = PublicBots.forGame(scn);
        String relocate = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Relocate a Jedi");
        assertNotNull(relocate);
        var relocationAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenRelocationAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        relocationAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosenRelocationAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("226_28",
                relocationAnalyzer.getObjectiveBlueprintId());
        assertTrue(relocationAnalyzer.isFlipped());
        assertTrue(relocationAnalyzer.isHiddenPathBackRelocateAction(
                scn.game(), VirtualTableScenario.LS,
                objective, "Relocate a Jedi"));
        var relocation = relocationAnalyzer
                .assessHiddenPathRelocation(
                    scn.game(), VirtualTableScenario.LS,
                    quinlan, village);
        assertTrue("POC to an empty Mapuzo battleground must be legal",
                relocation.legal());
        assertTrue("A redundant POC Jedi must leave the hold intact",
                relocation.preservesHold());
        assertEquals("The route must create the second Jedi battleground",
                1, relocation.battlegroundGain());
        assertTrue(relocationAnalyzer.hasUsefulHiddenPathRelocation(
                scn.game(), VirtualTableScenario.LS));
        assertEquals("The back must preserve the exact relocation payment",
                2, relocationAnalyzer
                    .getHiddenPathBackRelocationForceReserve(
                        scn.game(), VirtualTableScenario.LS));
        assertEquals("Chosen One must consume the same back reserve",
                relocationAnalyzer
                    .getHiddenPathBackRelocationForceReserve(
                        scn.game(), VirtualTableScenario.LS),
                chosenRelocationAnalyzer
                    .getHiddenPathBackRelocationForceReserve(
                        scn.game(), VirtualTableScenario.LS));
        assertEquals("A stacked Survivor costs its printed eight minus the back Objective's one",
                7, relocationAnalyzer
                    .getHiddenPathRouteActionForcePayment(
                        scn.game(), VirtualTableScenario.LS,
                        fallenOrder,
                        "Deploy a Jedi Survivor stacked here"));
        assertTrue("Eight Force cannot buy the Survivor and preserve the relocation payment",
                relocationAnalyzer
                    .wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        fallenOrder,
                        "Deploy a Jedi Survivor stacked here", 8));
        assertFalse("Nine Force pays seven and leaves the exact relocation reserve",
                relocationAnalyzer
                    .wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        fallenOrder,
                        "Deploy a Jedi Survivor stacked here", 9));
        assertFalse("Mapuzo never becomes a qualifying flip-back hold site",
                relocationAnalyzer.isHiddenPathFlipSite(
                    scn.game(), VirtualTableScenario.LS,
                    village));
        assertEquals(2, relocationAnalyzer
                .getHiddenPathRouteActionForcePayment(
                    scn.game(), VirtualTableScenario.LS,
                    objective, "Relocate a Jedi"));
        assertEquals("The safe payoff relocation must beat Pass",
                relocate, bots.decideBoth(scn));
        scn.LSDecided(relocate);

        assertTrue(scn.GetAwaitingDecision(VirtualTableScenario.LS)
                .getText().startsWith("Choose Jedi to relocate"));
        String moverChoice = bots.decideBoth(scn);
        PhysicalCard mover = scn.gameState().findCardById(
                Integer.parseInt(moverChoice));
        assertTrue("Use a redundant Jabiim Jedi, never the singleton battleground anchor",
                mover == kelleran || mover == quinlan);
        scn.LSDecided(moverChoice);

        assertTrue(scn.GetAwaitingDecision(VirtualTableScenario.LS)
                .getText().startsWith("Choose site to relocate "));
        String destinationChoice = bots.decideBoth(scn);
        PhysicalCard chosenDestination = scn.gameState().findCardById(
                Integer.parseInt(destinationChoice));
        var chosenAssessment = relocationAnalyzer
                .assessHiddenPathRelocation(
                    scn.game(), VirtualTableScenario.LS,
                    mover, chosenDestination);
        assertFalse("Do not pile onto the occupied battleground anchor",
                marketplace == chosenDestination);
        assertFalse("With two hold sites already safe, pursue the available Mapuzo battleground payoff",
                relocationAnalyzer.isHiddenPathFlipSite(
                    scn.game(), VirtualTableScenario.LS,
                    chosenDestination));
        assertTrue("The selected empty battleground must preserve the objective",
                chosenAssessment.preservesHold());
        assertEquals("The selected route must create a second Jedi battleground",
                1, chosenAssessment.battlegroundGain());
        scn.LSDecided(destinationChoice);
        scn.PassAllResponses();
        resolveRequiredWindows(scn);
        assertEquals("The native relocation costs exactly 2 Force",
                0, scn.GetLSForcePileCount());
        assertEquals(chosenDestination,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), mover));
        assertTrue("The same physical objective must remain flipped",
                objective.isFlipped());
        assertEquals("Once two Jedi battlegrounds are occupied, do not reserve for a pointless third",
                0, relocationAnalyzer
                    .getHiddenPathBackRelocationForceReserve(
                        scn.game(), VirtualTableScenario.LS));
        assertTrue("The native relocation is a regular move",
                scn.game().getModifiersQuerying()
                    .hasPerformedRegularMoveThisTurn(mover));
        assertFalse("A Jedi that already relocated cannot be assessed for another regular move",
                relocationAnalyzer.assessHiddenPathRelocation(
                    scn.game(), VirtualTableScenario.LS,
                    mover, poc).legal());
        assertFalse("The exact relocation is once during each MOVE phase",
                GameConditions.isOnceDuringYourPhase(
                    scn.game(), objective,
                    VirtualTableScenario.LS,
                    objective.getCardId(),
                    GameTextActionId.OTHER_CARD_ACTION_1,
                    Phase.MOVE));

        scn.SkipToDSTurn(Phase.DRAW);
        int darkLifeBeforePayoff = scn.GetDSLifeForceRemaining();
        scn.DSPass();
        scn.LSPass();
        scn.PassAllResponses();
        assertTrue("Two ordinary Jedi battlegrounds must fire the native end-turn payoff",
                scn.AwaitingDSForceLossPayment());
        scn.DSPayRemainingForceLossFromReserveDeck();
        assertEquals("Gather Allies And Train must make the opponent lose exactly 1 Force",
                darkLifeBeforePayoff - 1,
                scn.GetDSLifeForceRemaining());
        assertTrue(objective.isFlipped());
    }

    @Test
    public void exactCorridorReserveAndHiddenParentPaymentsAreMirrored() {
        var scn = hiddenPathRouteScenario();
        var objective = scn.GetLSCard("objective");
        var fallenOrder = scn.GetLSCard("fallenOrder");
        var corridor = scn.GetLSCard("corridor");
        var poc = scn.GetLSCard("poc");
        var hangar = scn.GetLSCard("hangar");
        var kelleran = scn.GetLSCard("kelleran");
        var obiwan = scn.GetLSCard("obiwan");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        moveSiteToSystem(scn, hangar, Title.Jabiim);
        scn.MoveCardsToLocation(
                corridor, kelleran, obiwan, scn.GetLSFiller(1));

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertEquals("Only the two legal unmoved Jedi Survivors reserve Force",
                2, rando.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS));
        assertEquals("Chosen One must read the same physical reserve",
                rando.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS),
                chosen.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS));

        assertTrue("A two-Force holocron may not consume both exits",
                rando.wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        objective, "Deploy a holocron from Reserve Deck", 2));
        assertFalse("Four Force pays for the holocron and both exits",
                rando.wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        objective, "Deploy a holocron from Reserve Deck", 4));
        assertTrue("A third cheap Survivor may not consume the exits at four Force",
                rando.wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        fallenOrder, "Deploy a Jedi Survivor stacked here", 4));
        assertFalse("Five Force may stage the third Survivor and retain both exits",
                rando.wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        fallenOrder, "Deploy a Jedi Survivor stacked here", 5));
        assertFalse("The free Jabiim pull never spends transit Force",
                rando.wouldHiddenPathRouteActionConsumeTransitReserve(
                        scn.game(), VirtualTableScenario.LS,
                        objective, "Deploy a Jabiim location", 2));

        scn.MoveCardsToLocation(poc, kelleran);
        assertEquals("One held site leaves one paid Corridor exit",
                1, rando.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS));
        assertEquals(
                rando.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS),
                chosen.getHiddenPathCorridorTransitForceReserve(
                        scn.game(), VirtualTableScenario.LS));
    }

    @Test
    public void publicBotsPullJabiimThenFundTwoSurvivorsBeforeHolocron() {
        var scn = hiddenPathRouteScenario();
        var objective = scn.GetLSCard("objective");
        var fallenOrder = scn.GetLSCard("fallenOrder");
        var safehouse = scn.GetLSCard("safehouse");
        var corridor = scn.GetLSCard("corridor");
        var poc = scn.GetLSCard("poc");
        var hangar = scn.GetLSCard("hangar");
        var holocron = scn.GetLSCard("holocron");

        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(poc, hangar, holocron);
        keepOnlyLightHandCards(scn);
        assertEquals(Zone.STACKED, scn.GetLSCard("kelleran").getZone());
        assertEquals(Zone.STACKED, scn.GetLSCard("obiwan").getZone());
        assertEquals(Zone.STACKED, scn.GetLSCard("quinlan").getZone());

        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        while (scn.GetLSForcePileCount() > 6) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        var bots = PublicBots.forGame(scn);
        PhysicalCard firstJabiim = pullJabiimWithBothBots(
                scn, bots, objective, poc, hangar);
        PhysicalCardImpl secondExpected =
                firstJabiim == poc ? hangar : poc;

        PhysicalCard first = deployNextSurvivorWithBothBots(
                scn, bots, fallenOrder, safehouse);
        PhysicalCard second = deployNextSurvivorWithBothBots(
                scn, bots, fallenOrder, safehouse);
        assertTrue(first != second);
        assertEquals("Two cheap Survivors consume the exact six-Force budget",
                0, scn.GetLSForcePileCount());
        assertTrue("The optional holocron must remain in Reserve for later",
                holocron.getZone() == Zone.RESERVE_DECK
                    || holocron.getZone() == Zone.TOP_OF_RESERVE_DECK);
        assertFalse(objective.isFlipped());

        scn.SkipToPhase(Phase.MOVE);
        PhysicalCard firstMover =
                moveSafehouseSurvivorToCorridorWithBothBots(
                    scn, bots, corridor);
        PhysicalCard secondMover =
                moveSafehouseSurvivorToCorridorWithBothBots(
                    scn, bots, corridor);
        assertTrue(firstMover != secondMover);

        scn.SkipToLSTurn(Phase.DEPLOY);
        // Turn activation can move either searched card out of Reserve. Put
        // the exact remaining route site and the optional device back before
        // asserting the deploy hierarchy.
        scn.MoveCardsToBottomOfLSReserveDeck(
                secondExpected, holocron);
        while (scn.GetLSForcePileCount() > 2) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.GetLSForcePileCount() < 2) {
            scn.LSActivateForceCheat(
                    2 - scn.GetLSForcePileCount());
        }
        var routeAnalyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        routeAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The remaining Jabiim site must physically be in Reserve",
                secondExpected.getZone() == Zone.RESERVE_DECK
                    || secondExpected.getZone() == Zone.TOP_OF_RESERVE_DECK);
        assertTrue("The remaining physical Jabiim site must be a native route candidate",
                routeAnalyzer.isNativeObjectiveLocationRouteCandidate(
                        scn.game(), VirtualTableScenario.LS,
                        objective, secondExpected));
        assertTrue("The exact Reserve scan must see the productive second pull",
                routeAnalyzer.hasObjectiveLocationRouteCandidateInReserve(
                        scn.game(), VirtualTableScenario.LS, objective));
        String secondPullAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy a Jabiim location");
        var secondScript = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhaseScript().selectAllowedActions(
                    scn.GetAwaitingDecision(VirtualTableScenario.LS),
                    scn.gameState(), scn.game(),
                    VirtualTableScenario.LS, routeAnalyzer);
        assertTrue("The second pull must remain in the DPS location bucket: "
                        + secondScript.reason + " allowed="
                        + secondScript.allowedActionIds,
                secondScript.allowedActionIds.contains(secondPullAction));
        PhysicalCard secondJabiim = pullJabiimWithBothBots(
                scn, bots, objective, poc, hangar);
        assertTrue(firstJabiim != secondJabiim);
        assertEquals("The free second pull preserves both transit Force",
                2, scn.GetLSForcePileCount());

        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        String holocronAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy a holocron from Reserve Deck");
        assertNotNull("The optional holocron remains legally offered",
                holocronAction);
        assertEquals("With both Jabiim sites out and exactly two Force, the public bots must pass instead of spending the transit budget",
                "", bots.decideBoth(scn));
        assertTrue(holocron.getZone() == Zone.RESERVE_DECK
                || holocron.getZone() == Zone.TOP_OF_RESERVE_DECK);

        scn.SkipToPhase(Phase.MOVE);
        CorridorTransit firstTransit =
                transitCorridorSurvivorWithBothBots(
                    scn, bots, corridor, poc, hangar);
        assertEquals("The first real transit costs one Force",
                1, scn.GetLSForcePileCount());
        assertFalse(objective.isFlipped());
        CorridorTransit secondTransit =
                transitCorridorSurvivorWithBothBots(
                    scn, bots, corridor, poc, hangar);
        assertTrue(firstTransit.mover() != secondTransit.mover());
        assertTrue("The two Survivors must occupy distinct sites",
                firstTransit.destination()
                    != secondTransit.destination());
        assertEquals("The complete route spends exactly two Force",
                0, scn.GetLSForcePileCount());
        assertTrue("The physical objective must flip from the two real transits",
                objective.isFlipped());
    }
}
