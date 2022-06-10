package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.cards.evaluators.MultiplyEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Alien
 * Title: Unkar Plutt
 */
public class Card501_004 extends AbstractRebel {
    public Card501_004() {
        super(Side.DARK, 1, 2, 2, 2, 4, "Unkar Plutt", Uniqueness.UNIQUE);
        setLore("Crolute scavenger and thief.");
        setGameText("During battle at a Jakku site (or with BB-8, Rey, or Falcon), adds one destiny to total power. Once per game, may retrieve a device or character weapon. During battle, your battle destinies are +¼ for each device, droid, starship, vehicle, or weapon here.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.SCAVENGER, Keyword.THIEF);
        setSpecies(Species.CROLUTE);
        setTestingText("Unkar Plutt");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsDestinyToPowerModifier(self, new OrCondition(new AtCondition(self, Filters.Jakku_site), new WithCondition(self, Filters.or(Filters.BB8, Filters.Rey, Filters.Falcon))), 1));
        modifiers.add(new EachBattleDestinyModifier(self, Filters.here(self), new MultiplyEvaluator(0.25f, new HereEvaluator(self, Filters.or(Filters.device, Filters.droid, Filters.starship, Filters.vehicle, Filters.weapon_or_character_with_permanent_weapon))), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.UNKAR_PLUTT__RETRIEVE_DEVICE_OR_WEAPON;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve a device or character weapon");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveCardEffect(action, playerId, Filters.or(Filters.device, Filters.character_weapon)));
            return Collections.singletonList(action);
        }
        return null;
    }
}
