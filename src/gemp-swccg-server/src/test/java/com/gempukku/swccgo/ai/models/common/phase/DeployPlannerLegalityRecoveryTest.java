package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Replay 72361: an impossible first destination must not trap a funded pair. */
public class DeployPlannerLegalityRecoveryTest {
    private static final String PLAYER = "~Rando_Cal";
    private static final String MODEL_PACKAGE = "com.gempukku.swccgo.ai.models.";

    @Test
    public void tenForceChewieJynPacketUsesLegalHothInsteadOfUnreachableWorks()
            throws Exception {
        for (String bot : List.of("rando", "chosenone")) {
            Fixture fixture = new Fixture(bot);
            Object plan = fixture.establish();
            List<?> instructions = instructions(plan);
            assertEquals(bot, 2, instructions.size());
            int cost = 0;
            for (Object instruction : instructions) {
                assertEquals(bot, "174", get(instruction, "getTargetLocationId"));
                cost += (Integer) get(instruction, "getDeployCost");
            }
            assertEquals(bot, 9, cost);
            assertTrue(bot, cost <= 10);
        }
    }

    @Test
    public void engineAllowedSpyAndLaterPresenceKeepZeroIconSitesAvailable()
            throws Exception {
        for (String bot : List.of("rando", "chosenone")) {
            Fixture fixture = new Fixture(bot);
            assertFalse(bot, fixture.canDeploy(fixture.chewie, fixture.works));
            assertTrue(bot, fixture.canDeploy(fixture.jyn, fixture.works));
            fixture.worksOpened.set(true);
            assertTrue(bot, fixture.canDeploy(fixture.chewie, fixture.works));
        }
    }

    @Test
    public void missingExactTargetReleasesOnlyUnstartedMatchingPacket()
            throws Exception {
        for (String bot : List.of("rando", "chosenone")) {
            Fixture fixture = new Fixture(bot);
            Object plan = fixture.establish();
            set(fixture.planner, "currentPlan", plan);
            assertFalse(fixture.reject(fixture.chewie, List.of("174")));
            assertSame(plan, get(fixture.planner, "getCurrentPlan"));
            PhysicalCard otherCopy = character(999, "10_3", "Chewbacca, Protector", 6, 5, 2);
            assertFalse(fixture.reject(otherCopy, List.of("175")));
            assertSame(plan, get(fixture.planner, "getCurrentPlan"));
            assertTrue(fixture.reject(fixture.chewie, List.of("175")));
            assertNull(get(fixture.planner, "getCurrentPlan"));
            assertEquals(-1, field(fixture.planner, "lastPlanTurn"));
        }
    }

    @Test
    public void completedFirstDeploymentAndNonHandPromptsCannotBreakBuddyPromise()
            throws Exception {
        for (String bot : List.of("rando", "chosenone")) {
            Fixture fixture = new Fixture(bot);
            Object plan = fixture.establish();
            set(fixture.planner, "currentPlan", plan);
            when(fixture.chewie.getZone()).thenReturn(Zone.AT_LOCATION);
            assertFalse(fixture.reject(fixture.chewie, List.of("175")));
            when(fixture.chewie.getZone()).thenReturn(Zone.HAND);
            plan.getClass().getMethod("setDeploymentsMade", int.class).invoke(plan, 1);
            assertFalse(fixture.reject(fixture.chewie, List.of("175")));
            assertSame(plan, get(fixture.planner, "getCurrentPlan"));
        }
    }

    private static class Fixture {
        final Object planner;
        final String bot;
        final GameState state = mock(GameState.class);
        final SwccgGame game = mock(SwccgGame.class);
        final ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        final PhysicalCard chewie = character(206, "10_3", "Chewbacca, Protector", 6, 5, 2);
        final PhysicalCard jyn = character(334, "206_4", "Jyn Erso", 4, 4, 3);
        final PhysicalCard works = site(190, "Coruscant: The Works");
        final PhysicalCard hoth = site(174, "Hoth: Defensive Perimeter (3rd Marker)");
        final AtomicBoolean worksOpened = new AtomicBoolean();

        Fixture(String bot) throws Exception {
            this.bot = bot;
            planner = Class.forName(MODEL_PACKAGE + bot + ".strategy.DeployPhasePlanner")
                    .getConstructor().newInstance();
            when(game.getGameState()).thenReturn(state);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(state.getForcePileSize(PLAYER)).thenReturn(10);
            when(state.findCardByPermanentId(206)).thenReturn(chewie);
            when(state.findCardByPermanentId(334)).thenReturn(jyn);
            when(state.findCardById(174)).thenReturn(hoth);
            when(state.findCardById(190)).thenReturn(works);
            // Stub only the engine boundary, using its exact destination filter.
            // The AI must consult this authority, not infer legality from gametext.
            when(modifiers.isDeployableToTarget(any(), any(), any(), anyBoolean(),
                    any(), anyBoolean(), anyFloat(), any(), any(), any(), any(),
                    any(), any(), anyBoolean(), anyFloat())).thenAnswer(call -> {
                PhysicalCard card = call.getArgument(2);
                Filter target = call.getArgument(4);
                if (target.accepts(state, modifiers, hoth)) return true;
                return target.accepts(state, modifiers, works)
                        && (card == jyn || worksOpened.get());
            });
            set(planner, "currentGame", game);
            set(planner, "currentPlayerId", PLAYER);
        }

        Object establish() throws Exception {
            Class<?> cardInfo = Class.forName(MODEL_PACKAGE + bot + ".strategy.CardInfo");
            List<Object> cards = List.of(
                    cardInfo.getConstructor(PhysicalCard.class).newInstance(chewie),
                    cardInfo.getConstructor(PhysicalCard.class).newInstance(jyn));
            List<AiBoardAnalyzer.LocationAnalysis> sites = new ArrayList<>(List.of(
                    new AiBoardAnalyzer.LocationAnalysis(works, 0, 0, 0, 0,
                            0, 2, 0, 0, AiBoardAnalyzer.ContestStatus.EMPTY, false),
                    new AiBoardAnalyzer.LocationAnalysis(hoth, 0, 0, 0, 0,
                            1, 2, 0, 0, AiBoardAnalyzer.ContestStatus.EMPTY, true)));
            Method method = planner.getClass().getDeclaredMethod("generateEstablishPlan",
                    List.class, List.class, int.class, int.class, String.class);
            method.setAccessible(true);
            return method.invoke(planner, cards, sites, 10, 3, "ground");
        }

        boolean canDeploy(PhysicalCard card, PhysicalCard site) throws Exception {
            Method method = planner.getClass().getDeclaredMethod("canDeployToLocation",
                    PhysicalCard.class, PhysicalCard.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(planner, card, site);
        }

        boolean reject(PhysicalCard card, Collection<String> offered) throws Exception {
            return (Boolean) planner.getClass().getMethod("rejectUnavailableDeployTarget",
                    PhysicalCard.class, Collection.class).invoke(planner, card, offered);
        }
    }

    private static PhysicalCard character(int id, String blueprintId, String title,
                                           int power, int cost, int ability) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.HAND);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.getGameText()).thenReturn("No printed deployment restriction.");
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn((float) power);
        when(blueprint.getDeployCost()).thenReturn((float) cost);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn((float) ability);
        return card;
    }

    private static PhysicalCard site(int id, String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn("asdf");
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return card;
    }

    private static List<?> instructions(Object plan) throws Exception {
        return (List<?>) get(plan, "getInstructions");
    }

    private static Object get(Object object, String method) throws Exception {
        return object.getClass().getMethod(method).invoke(object);
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
