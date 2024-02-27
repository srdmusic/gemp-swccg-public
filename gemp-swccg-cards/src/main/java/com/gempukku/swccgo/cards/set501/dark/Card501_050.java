package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Gor Karesh
 */
public class Card501_050 extends AbstractAlien {
    public Card501_050() {
        super(Side.DARK, 3, 3, 3, 2, 5, "Gor Karesh", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Information Broker. Abyssin.");
        setGameText("Your mercenaries, scouts and thieves are power and forfeit +1 here. Opponent may not cancel your battle destiny draws where you have a gangster. When deployed, may deploy a non-[M] Gamorrean from Reserve Deck here for -2 Force; reshuffle.");
        addKeywords(Keyword.INFORMATION_BROKER);
        setSpecies(Species.ABYSSIN);
        setTestingText("Gor Karesh");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Filter affectFilter = Filters.and(Filters.your(self), Filters.or(Keyword.MERCENARY, Filters.scout, Filters.thief), Filters.atSameLocation(self));
        modifiers.add(new PowerModifier(self, affectFilter, 1));
        modifiers.add(new ForfeitModifier(self, affectFilter, 1));
        modifiers.add(new MayNotCancelBattleDestinyModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.gangster)), playerId, opponent));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.GOR_KARESH__DOWNLOAD_GAMORREAN;

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, true, false)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy non-[M] Gamorrean ");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.not(Filters.icon(Icon.MAINTENANCE)), Filters.Gamorrean), Filters.here(self), -2, true));
            return Collections.singletonList(action);
        }
        return null;
    }
}