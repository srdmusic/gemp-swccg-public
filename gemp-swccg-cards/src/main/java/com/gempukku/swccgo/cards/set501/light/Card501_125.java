package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.ManeuverModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Corran Horn, Jedi
 */

public class Card501_125 extends AbstractRebel {
    public Card501_125() {
        super(Side.LIGHT, 1, 5, 4, 6, 6, "Corran Horn, Jedi", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Corellian.  Rogue Squadron pilot.");
        setGameText("Adds 1 to maneuver of anything he pilots. Once per game, may deploy a blaster on Corran from hand or Reserve Deck (reshuffle) as a 'react.' During battle, may subtract 1 from a just drawn blaster weapon destiny; Corran is power +2. Immune to attrition < 5.");
        addPersona(Persona.CORRAN_HORN);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        setSpecies(Species.CORELLIAN);
        addKeyword(Keyword.ROGUE_SQUADRON);
        setTestingText("Corran Horn, Jedi");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ManeuverModifier(self, Filters.hasPiloting(self), 1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult, Filters.blaster)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            // Subtract 1
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Subtract 1 from destiny draw");
            action.appendUsage(
                    new OncePerTurnEffect(action)
            );
            // Perform result(s)
            action.appendEffect(
                    new ModifyDestinyEffect(action, -1));
            action.appendEffect(
                    new ModifyPowerUntilEndOfBattleEffect(action, self, 2));
            actions.add(action);
        }

        GameTextActionId gameTextActionId2 = GameTextActionId.CORRAN_HORN_JEDI__DOWNLOAD_BLASTER;
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if ((TriggerConditions.battleInitiated(game, effectResult, opponent)
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent))
                && GameConditions.isOncePerGame(game, self, gameTextActionId2)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId2, true)) {

            final OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId2);
            action2.setText("Deploy blaster from Reserve Deck as a 'react'");
            // Allow response(s)
            action2.appendUsage(
                    new OncePerGameEffect(action2));
            action2.appendEffect(
                    new DeployCardFromReserveDeckEffect(action2, Filters.blaster, false, true, true));
            actions.add(action2);
        }

        if ((TriggerConditions.battleInitiated(game, effectResult, opponent)
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent))
                && GameConditions.isOncePerGame(game, self, gameTextActionId2)
                && GameConditions.hasInHand(game, playerId, Filters.blaster)) {

            final OptionalGameTextTriggerAction action3 = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId2);
            action3.setText("Deploy blaster from hand as 'react'");
            // Allow response(s)
            action3.appendUsage(
                    new OncePerGameEffect(action3));
            action3.appendEffect(
                    new DeployCardFromHandEffect(action3, playerId, Filters.blaster, 0));
            actions.add(action3); 
        }
        return actions;
    }
}
