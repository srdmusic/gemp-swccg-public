package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.DuringAttackWithParticipantCondition;
import com.gempukku.swccgo.cards.conditions.InBattleWithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.NumDestinyDrawsDuringAttackModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Alien
 * Title: Grummgar
 */
public class Card501_098 extends AbstractAlien {
    public Card501_098() {
        super(Side.DARK, 2, 4, 6, 2, 5, "Grummgar", Uniqueness.UNIQUE);
        setLore("Dowutin mercenary.");
        setGameText("During battle with an information broker (or during an attack), adds one destiny to total power. Once per game, may deploy a blaster, rifle, or creature (or a non-[Permanent Weapon], non-weapon card with 'creature' in lore or game text) here from Reserve Deck; reshuffle.");
        setSpecies(Species.DOWUTIN);
        addIcons(Icon.WARRIOR, Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        setTestingText("Grummgar");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new AddsDestinyToPowerModifier(self, new InBattleWithCondition(self, Filters.information_broker), 1, self.getOwner()));
        modifiers.add(new NumDestinyDrawsDuringAttackModifier(self, new DuringAttackWithParticipantCondition(self), 1, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.GRUMMGAR__DEPLOY_CARD_HERE;

        //Once per game, may deploy a non-[Permanent Weapon] non-weapon card with “creature” in lore or game text (or any blaster, rifle, or creature) here from Reserve Deck; reshuffle.
        Filter filter = Filters.or(Filters.and(Filters.not(Icon.PERMANENT_WEAPON), Filters.not(Filters.weapon),
                Filters.or(Filters.loreContains("creature"), Filters.loreContains("creatures"), Filters.gameTextContains("creature"), Filters.gameTextContains("creatures"))),
                Filters.blaster, Filters.rifle, Filters.creature);
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card here from Reserve Deck");
            action.setActionMsg("Deploy a blaster, rifle, or creature (or a non-[Permanent Weapon], non-weapon card with 'creature' in lore or game text) here from Reserve Deck");
            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, filter, Filters.here(self), true));
            return Collections.singletonList(action);
        }


        return null;
    }
}
