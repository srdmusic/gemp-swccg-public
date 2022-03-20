package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AttachedCondition;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: I Hope She's All Right & Part Of The Tribe
 */
public class Card501_049 extends AbstractNormalEffect {
    public Card501_049() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, "I Hope She's All Right & Part Of The Tribe", Uniqueness.UNIQUE);
        addComboCardTitles(Title.I_Hope_Shes_All_Right, "Part Of The Tribe");
        setGameText("Deploy on your Rebel of ability < 6. Character gains Ewok. Once per game, may take an Ewok into hand from Reserve Deck; reshuffle. During your draw phase, if you occupy two battleground sites, unless opponent’s character of destiny < 4 occupies a battleground site, opponent loses 1 Force. While on Endor, adds one [LS] icon. Immune to Alter while on Leia.");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_18);
        setTestingText("I Hope She's All Right & Part Of The Tribe");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.Rebel, Filters.abilityLessThan(6));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new SpeciesModifier(self, Filters.hasAttached(self), Species.EWOK));
        modifiers.add(new ImmuneToTitleModifier(self, new AttachedCondition(self, Filters.Leia), Title.Alter));
        modifiers.add(new IconModifier(self, Filters.sameLocation(self), new OnCondition(self, Title.Endor), Icon.LIGHT_FORCE, 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.I_HOPE_SHES_ALL_RIGHT_PART_OF_THE_TRIBE__UPLOAD_EWOK;
        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.canSearchReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Upload an Ewok");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.Ewok, true));
            actions.add(action);
        }


        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.occupies(game, playerId, 2, Filters.battleground_site)
                && !GameConditions.occupiesWith(game, self, opponent, Filters.battleground_site, Filters.and(Filters.character, Filters.destinyLessThan(6)))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // Check if reached end of your draw phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.DRAW, self.getOwner())
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.occupies(game, playerId, 2, Filters.battleground_site)
                && !GameConditions.occupiesWith(game, self, opponent, Filters.battleground_site, Filters.and(Filters.character, Filters.destinyLessThan(6)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 1 Force");
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return actions;
    }
}