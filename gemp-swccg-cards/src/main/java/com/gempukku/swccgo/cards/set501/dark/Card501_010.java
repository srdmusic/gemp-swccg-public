package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.*;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveForfeitValueIncreasedModifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 17
 * Type: Objective
 * Title: Imperial Occupation (V) / Imperial Control (V)
 */
public class Card501_010 extends AbstractObjective {
    public Card501_010() {
        super(Side.DARK, 0, Title.Imperial_Occupation);
        setVirtualSuffix(true);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy [Set 8] 5th marker and either Hoth system or [Set 17] 4th marker. \n" +
                "For remainder of game, AT-AT Cannons are immune to Sabotage. Sunsdown and your non-[Set 17] Epic Events are canceled. Imperials (except snowtroopers) may not have their forfeit increased. Your non-[M] AT-ATs, snowtroopers, and Star Destroyers are destiny +1. \n" +
                "Flip this card if Main Power Generators 'blown away' or if your AT-ATs control three Hoth sites and you occupy Hoth system (with Hoth Blockade there).");
        addIcons(Icon.SPECIAL_EDITION, Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("[Set 18] Imperial Occupation (V)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_8, Filters.Fifth_Marker), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose [Set 8] 5th marker to deploy";
                    }
                });
        action.appendOptionalEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Hoth_system, Filters.and(Icon.VIRTUAL_SET_17, Filters.Fourth_Marker)),  true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Hoth system or [Set 17] 4th marker to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToTitleModifier(self, Filters.AT_AT_Cannon, Title.Sabotage));
        modifiers.add(new DestinyModifier(self, Filters.and(Filters.your(self), Filters.or(Filters.and(Filters.not(Icon.MAINTENANCE), Filters.AT_AT), Filters.snowtrooper, Filters.Star_Destroyer)), 1));
        modifiers.add(new MayNotHaveForfeitValueIncreasedModifier(self, Filters.and(Filters.character, Filters.Imperial, Filters.except(Filters.snowtrooper))));
        modifiers.add(new MayNotPlayModifier(self, Filters.or(Filters.Sunsdown, Filters.and(Filters.Epic_Event, Filters.except(Icon.VIRTUAL_SET_17))), playerId));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.canBeFlipped(game, self)
                && (TriggerConditions.isBlownAwayLastStep(game, effectResult, Filters.title(Title.Main_Power_Generators, true))
                || (GameConditions.occupies(game, playerId, Filters.and(Filters.Hoth_system, Filters.sameSystemAs(self, Filters.title("Hoth Blockade")))))
                && GameConditions.controlsWith(game, self, playerId, 3, Filters.Hoth_site, Filters.and(Filters.your(self), Filters.AT_AT)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        return actions;
    }
}