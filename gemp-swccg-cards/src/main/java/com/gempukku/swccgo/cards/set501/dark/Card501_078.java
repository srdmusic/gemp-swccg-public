package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublicSith;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.ExcludeFromBattleEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Republic / Sith
 * Title: Darth Vader, Betrayer Of The Jedi
 */
public class Card501_078 extends AbstractRepublicSith {
    public Card501_078() {
        super(Side.DARK, 1, 6, 6, 6, 8, "Darth Vader, Betrayer Of The Jedi", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Leader");
        setGameText("[Pilot] 3. Vader’s game text may not be canceled. While on Coruscant, adds one [D] icon here. In battle with no other Dark Jedi, may: exclude Padme OR cancel immunity to attrition of a Jedi or Padawan. Immune to attrition < 5.");
        addPersona(Persona.VADER);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_15);
        setTestingText("Darth Vader, Betrayer Of The Jedi");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self));
        modifiers.add(new IconModifier(self, Filters.here(self), new OnCondition(self, Title.Coruscant), Icon.DARK_FORCE));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isDuringBattleAt(game, Filters.here(self))
                && !GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.other(self), Filters.Dark_Jedi))
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            TargetingReason targetingReason = TargetingReason.TO_BE_EXCLUDED_FROM_BATTLE;

            if (GameConditions.isDuringBattleWithParticipant(game, Filters.Padme)
                    && GameConditions.canTarget(game, self, targetingReason, Filters.Padme)) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Exclude Padme from battle");
                action.appendUsage(
                        new OncePerBattleEffect(action)
                );
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose character", targetingReason, Filters.Padme) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard targetedCard) {
                                action.addAnimationGroup(targetedCard);
                                // Allow response(s)
                                action.allowResponses("Exclude " + GameUtils.getCardLink(targetedCard) + " from battle",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new ExcludeFromBattleEffect(action, targetedCard));
                                            }
                                        }
                                );
                            }
                        });
                actions.add(action);
            }

            Filter jediOrPadawanHereFilter = Filters.and(Filters.or(Filters.Jedi, Filters.padawan), Filters.here(self), Filters.hasAnyImmunityToAttrition);
            if (GameConditions.isDuringBattleWithParticipant(game, jediOrPadawanHereFilter)
                    && GameConditions.canTarget(game, self, jediOrPadawanHereFilter)) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Cancel immunity to attrition of a Jedi or Padawan");
                action.appendUsage(
                        new OncePerBattleEffect(action)
                );
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose Jedi or Padawan", jediOrPadawanHereFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard cardTargeted) {
                                action.addAnimationGroup(cardTargeted);
                                // Allow response(s)
                                action.allowResponses("Cancel " + GameUtils.getCardLink(cardTargeted) + "'s immunity to attrition",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new AddUntilEndOfGameModifierEffect(action,
                                                                new CancelImmunityToAttritionModifier(self, cardTargeted),
                                                                "Cancels " + GameUtils.getCardLink(cardTargeted) + "'s immunity to attrition")
                                                );
                                            }
                                        }
                                );
                            }
                        }
                );
                actions.add(action);
            }
        }
        return actions;
    }
}
