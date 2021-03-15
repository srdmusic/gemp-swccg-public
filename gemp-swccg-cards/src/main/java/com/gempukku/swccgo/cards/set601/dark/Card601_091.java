package com.gempukku.swccgo.cards.set601.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Block 6
 * Type: Effect
 * Title: Ni Chuba Na?? (V)
 */
public class Card601_091 extends AbstractNormalEffect {
    public Card601_091() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Ni Chuba Na??", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'Your buddy here was about to be turned into orange goo. He picked a fight with a Dug. An especially dangerous Dug called Sebulba.'");
        setGameText("Deploy on table.  Your Force generation is +1.  Once per game, may relocate this Effect to a site.  At same and related locations, Revolution is canceled and your cards may not have their deploy costs modified by Goo Nee Tay.  Security Precautions is canceled. (Immune to Alter.)");
        addIcons(Icon.TATOOINE, Icon.EPISODE_I, Icon.LEGACY_BLOCK_6);
        addImmuneToCardTitle(Title.Alter);
        setAsLegacy(true);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalForceGenerationModifier(self, 1, playerId));

//        modifiers.add(new ImmuneToDeployCostModifiersToLocationModifier(self, Filters.and(Filters.your(self),
//                Filters.or(Icon.CLOUD_CITY, Icon.JABBAS_PALACE, Icon.SPECIAL_EDITION), Filters.character),
//                Filters.Goo_Nee_Tay, Filters.Bespin_location));
        return modifiers;
    }
//TODO cancel Revolution/Security Precautions
    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.LEGACY__NI_CHUBA_NA__RELOCATE_TO_SITE;

        //TODO don't use Filters.canRelocateEffectTo
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.canSpot(game, self, Filters.canRelocateEffectTo(playerId, self))) {

            Collection<PhysicalCard> sites =  Filters.filterTopLocationsOnTable(game, Filters.canRelocateEffectTo(playerId, self));
            if (!sites.isEmpty()) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Relocate to a site");
                action.appendUsage(new OncePerGameEffect(action));
                action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Relocate "+ GameUtils.getCardLink(self)+" to which site?", Filters.in(sites)) {
                    @Override
                    protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                        action.appendEffect(
                                new AttachCardFromTableEffect(action, self, targetedCard));
                    }
                });
                // Perform result(s)
                return Collections.singletonList(action);
            }
        }

        return null;
    }
}