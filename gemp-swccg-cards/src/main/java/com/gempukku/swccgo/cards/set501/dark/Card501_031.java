package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceAndStackFaceDownEffect;
import com.gempukku.swccgo.logic.modifiers.DefinedByGameTextDeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Effect
 * Title: Thrawn's Art Collection
 */
public class Card501_031 extends AbstractNormalEffect {
    public Card501_031() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Thrawns_Art_Collection, Uniqueness.UNIQUE);
        setGameText("Use 4 Force to deploy on table. Cards stacked here are 'artwork.' If you just initiated a Force drain (or won a battle) at the same battleground as Thrawn, opponent loses 1 Force (cannot be reduced) and stacks it here face down. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_19);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Thrawn's Art Collection");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DefinedByGameTextDeployCostModifier(self, 4));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedBy(game, effectResult, playerId, Filters.and(Filters.battleground, Filters.sameLocationAs(self, Filters.Thrawn)))
                || TriggerConditions.wonBattleAt(game, effectResult, playerId, Filters.and(Filters.battleground, Filters.sameLocationAs(self, Filters.Thrawn)))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make opponent lose 1 Force and stack here");
            action.setActionMsg("Make opponent lose 1 Force and stack lost card face down on " + GameUtils.getCardLink(self));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceAndStackFaceDownEffect(action, opponent, 1, self) {
                        @Override
                        public boolean isShownIfLostFromHand() {
                            return true;
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }
}