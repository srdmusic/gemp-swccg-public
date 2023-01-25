package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfWeaponFiringModifierEffect;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.TargetCardsOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckSimultaneouslyWithCardEffect;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.modifiers.ResetDefenseValueModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.GuiUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Aratech Corporation & Royal Escort
 */
public class Card501_045 extends AbstractNormalEffect {
    public Card501_045() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Aratech Corporation & Royal Escort", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles("Aratech Corporation", "Royal Escort");
        setGameText("Deploy on table. Your AT-STs and speeder bikes are power and forfeit +1. During your deploy phase, may reveal an AT-ST or Speeder Bike from hand to [upload] a unique (•) [Endor] Imperial pilot of ability < 3 (or vice versa) and deploy both simultaneously. If opponent just used a weapon to target your character aboard a piloted vehicle, that character may use that vehicle's defense value. [Immune to Alter.]");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Aratech Corporation & Royal Escort");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter filter = Filters.and(Filters.your(self), Filters.or(Filters.AT_ST, Filters.speeder_bike));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, filter, 1));
        modifiers.add(new ForfeitModifier(self, filter, 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final Filter atstOrSpeederBike = Filters.or(Filters.AT_ST, Filters.speeder_bike);
        Filter filter = Filters.and(Filters.or(Filters.and(Filters.unique, Icon.ENDOR, Filters.Imperial, Filters.pilot, Filters.abilityLessThan(3)), atstOrSpeederBike), Filters.isUniquenessOnTableNotReached);

        GameTextActionId gameTextActionId = GameTextActionId.ARATECH_CORPORATION__DEPLOY_ATST_SPEEDER_BIKE_OR_PILOT;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.hasInHand(game, playerId, filter)
                && GameConditions.canSearchReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Reveal pilot, AT-ST, or Speeder Bike from hand");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Choose target(s)
            action.appendTargeting(
                    new ChooseCardFromHandEffect(action, playerId, filter) {
                        @Override
                        protected void cardSelected(SwccgGame game, final PhysicalCard selectedCard) {
                            final Filter searchFilter;
                            if (Filters.character.accepts(game, selectedCard)) {
                                action.setActionMsg("Take an AT-ST or Speeder Bike from Reserve Deck and deploy both simultaneously");
                                searchFilter = atstOrSpeederBike;
                            }
                            else {
                                action.setActionMsg("Take a unique (•) [Endor] Imperial pilot of ability < 3 from Reserve Deck and deploy both simultaneously");
                                searchFilter = Filters.and(Filters.unique, Icon.ENDOR, Filters.Imperial, Filters.pilot, Filters.abilityLessThan(3));
                            }
                            // Perform result(s)
                            action.appendEffect(
                                    new ShowCardOnScreenEffect(action, selectedCard));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckSimultaneouslyWithCardEffect(action, selectedCard, searchFilter, true));
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }


    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, final SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTargetedByWeapon(game, effect, Filters.and(Filters.your(self), Filters.character), Filters.any)) {
            Collection<PhysicalCard> characters = game.getGameState().getWeaponFiringState().getTargets();
            final Map<PhysicalCard, PhysicalCard> characterVehicleMap = new HashMap<>();
            for (PhysicalCard character : characters) {
                PhysicalCard vehicle = Filters.findFirstActive(game, self,
                        Filters.and(Filters.piloted, Filters.vehicle, Filters.hasAboardExceptRelatedSites(character)));
                if (vehicle != null) {
                    characterVehicleMap.put(character, vehicle);
                }
            }

            if (!characterVehicleMap.isEmpty()) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Have character" + GameUtils.s(characterVehicleMap.keySet()) + " use vehicle's defense value");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardsOnTableEffect(action, playerId, "Choose characters to use vehicle's defense value'", 1, Integer.MAX_VALUE, Filters.in(characterVehicleMap.keySet())) {
                            @Override
                            protected void cardsTargeted(int targetGroupId, final Collection<PhysicalCard> characters) {
                                action.addAnimationGroup(characters);
                                // Allow response(s)
                                action.allowResponses("Have " + GameUtils.getAppendedNames(characters) + " use vehicle's defense value",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                for (PhysicalCard character : characters) {
                                                    float defenseValue = game.getModifiersQuerying().getDefenseValue(game.getGameState(), characterVehicleMap.get(character));
                                                    action.appendEffect(
                                                            new AddUntilEndOfWeaponFiringModifierEffect(action,
                                                                    new ResetDefenseValueModifier(self, character, defenseValue),
                                                                    "Resets " + GameUtils.getCardLink(character) + "'s defense value to " + GuiUtils.formatAsString(defenseValue)));
                                                }
                                            }
                                        }
                                );
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}