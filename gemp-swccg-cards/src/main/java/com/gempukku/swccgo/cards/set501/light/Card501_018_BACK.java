package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelBattleEffect;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CaptureWithImprisonmentEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Objective
 * Title: Rescue The Princess / Sometimes I Amaze Even Myself (V)
 */
public class Card501_018_BACK extends AbstractObjective {
    public Card501_018_BACK() {
        super(Side.LIGHT, 7, Title.Sometimes_I_Amaze_Even_Myself);
        setGameText("For remainder of game I Can't Believe He's Gone may only add power in battles involving Luke or Leia. You retrieve no Force from Detention Block Corridor." +
                "While this side up, whenever you 'hit' a character with a blaster, opponent loses 1 Force. " +
                "May place Obi-Wan out of play from a Death Star site to cancel a battle at another Death Star site." +
                "If Leia is about to be removed from table, either player may imprison her in Detention Block Corridor instead." +
                "Flip this card if Leia is not on table.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        setTestingText("Rescue The Princess / Sometimes I Amaze Even Myself (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        Filter yourDeathStarSites = Filters.and(Filters.your(playerId), Filters.Death_Star_site);
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ForceGenerationModifier(self, yourDeathStarSites, 1, self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.Jedi, Filters.not(Filters.ObiWan)), self.getOwner()));
        modifiers.add(new ModifyGameTextModifier(self, Filters.Set_Your_Course_For_Alderaan, ModifyGameTextType.SET_YOUR_COURSE_FOR_ALDERAAN__ONLY_AFFECTS_DARK_SIDE_DEATH_STAR_SITES));
        modifiers.add(new ModifyGameTextModifier(self, Filters.I_Cant_Believe_Hes_Gone, ModifyGameTextType.I_CANT_BELIEVE_HES_GONE__ONLY_EFFECTS_BATTLES_WITH_LUKE_OR_LEIA));
        modifiers.add(new MayNotContributeToForceRetrievalModifier(self, Filters.Detention_Block_Corridor));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (GameConditions.canSpot(game, self, Filters.and(Filters.ObiWan, Filters.at(Filters.Death_Star_site)))
                && TriggerConditions.battleInitiatedAt(game, effectResult, Filters.and(Filters.Death_Star_site, Filters.not(Filters.sameLocationAs(self, Filters.ObiWan))))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Cancel battle");
            // Pay cost(s)
            action.appendCost(
                    new PlaceCardOutOfPlayFromTableEffect(action, Filters.findFirstActive(game, self, Filters.ObiWan)));
            // Perform result(s)
            action.appendEffect(
                    new CancelBattleEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        if (GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, Filters.Leia)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Flip");
            action.appendEffect(
                    new FlipCardEffect(action, self)
            );
            actions.add(action);
        }

        if (TriggerConditions.justHitBy(game, effectResult, Filters.character, Filters.and(Filters.your(self.getOwner()), Filters.blaster))) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make opponent lose 1 Force");
            action.appendEffect(
                    new LoseForceEffect(action, game.getOpponent(self.getOwner()), 1)
            );
            actions.add(action);
        }

        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.Leia)
                && GameConditions.canSpot(game, self, Filters.Detention_Block_Corridor)) {
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard leia = result.getCardAboutToLeaveTable();

            if (leia != null) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Imprison Leia");
                action.setPerformingPlayer(game.getOpponent(self.getOwner()));
                action.appendEffect(
                        new PassthruEffect(action) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                result.getPreventableCardEffect().preventEffectOnCard(leia);
                            }
                        });
                action.appendEffect(
                        new CaptureWithImprisonmentEffect(action, leia, Filters.findFirstActive(game, self, Filters.Detention_Block_Corridor), leia.isUndercover(), leia.isMissing())
                );
                actions.add(action);
            }

        }

        return actions;
    }
}
