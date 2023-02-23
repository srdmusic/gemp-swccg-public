package com.gempukku.swccgo.cards.set7.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.TargetedByWeaponCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
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
import com.gempukku.swccgo.logic.effects.ModifySabaccTotalEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Special Edition
 * Type: Character
 * Subtype: Alien
 * Title: Iasa, The Traitor Of Jawa Canyon
 */
public class Card7_180 extends AbstractAlien {
    public Card7_180() {
        super(Side.DARK, 1, 3, 3, 1, 2, "Iasa, The Traitor Of Jawa Canyon", Uniqueness.UNIQUE, ExpansionSet.SPECIAL_EDITION, Rarity.U);
        setLore("While on a trip, King Kalit entrusted his credits, sandcrawler and mate to his friend Iasa. When Kalit returned, one was spent, one was sold and the other was missing.");
        setGameText("Deploys only on Tatooine. When firing any Jawa weapon, subtracts 3 from target's defense value. When playing Dune Sea Sabacc, may subtract 1 from or add 1 to your total.");
        addIcons(Icon.SPECIAL_EDITION, Icon.WARRIOR);
        setSpecies(Species.JAWA);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Deploys_on_Tatooine;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DefenseValueModifier(self, Filters.any, new TargetedByWeaponCondition(Filters.any, Filters.and(Filters.Jawa_weapon, Filters.attachedTo(self))), -3));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<OptionalGameTextTriggerAction>();

        // Check condition(s)
        if (TriggerConditions.isCalculatingSabaccTotals(game, effectResult)
                && GameConditions.isPlayingSabacc(game, self, Filters.title("Dune Sea Sabacc"))) {

            final OptionalGameTextTriggerAction action1 = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action1.setText("Add 1 to Sabacc total");
            // Perform result(s)
            action1.appendEffect(
                    new ModifySabaccTotalEffect(action1, playerId, 1));
            actions.add(action1);

            // Check condition(s)
            float sabaccTotal = game.getModifiersQuerying().getSabaccTotal(game.getGameState(), playerId);
            if (sabaccTotal > 0) {

                final OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action2.setText("Subtract 1 from Sabacc total");
                // Perform result(s)
                action2.appendEffect(
                        new ModifySabaccTotalEffect(action2, playerId, -1));
                actions.add(action2);
            }
        }
        return actions;
    }
}
