package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.LightSideGoesFirstEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Epic Event
 * Title: The Force Is Strong In My Family
 */
public class Card501_028 extends AbstractEpicEventDeployable {
    public Card501_028() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Force_Is_Strong_In_My_Family);
        setGameText("Deploys on table only at start of game. Light Side goes first. Choose one:" +
                "My Father Has It: Deploy Your Thoughts Dwell On Your Mother. You may not deploy characters of ability > 4 (except [Episode I] Jedi)." +
                "I Have It: Deploy Like My Father Before Me. You may not deploy Jedi (except Luke or Ahsoka)." +
                "You Have That Power Too: Deploy My Parents Were Strong. You may not deploy Jedi (except [Episode VII] Jedi).");
        addIcons(Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_16);
        setTestingText("The Force Is Strong In My Family");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.justDeployed(game, effectResult, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setPerformingPlayer(self.getOwner());

            final String MY_FATHER_HAS_IT = "My Father Has It";
            final String I_HAVE_IT = "I Have It";
            final String YOU_HAVE_THAT_POWER_TOO = "You Have That Power Too";

            String[] possibleResults = new String[]{MY_FATHER_HAS_IT, I_HAVE_IT, YOU_HAVE_THAT_POWER_TOO};

            action.appendTargeting(
                    new PlayoutDecisionEffect(action, self.getOwner(), new MultipleChoiceAwaitingDecision("Choose an option", possibleResults) {
                        @Override
                        protected void validDecisionMade(int index, String result) {
                            Filter effectFilter = null;
                            Filter cardsThatMayNotDeployFilter = null;

                            switch (result) {
                                case MY_FATHER_HAS_IT:
                                    //Deploy Your Thoughts Dwell On Your Mother. You may not deploy characters of ability > 4 (except [Episode I] Jedi).
                                    effectFilter = Filters.title(Title.Your_Thoughts_Dwell_On_Your_Mother);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.character, Filters.abilityMoreThan(4), Filters.except(Filters.and(Filters.icon(Icon.EPISODE_I), Filters.Jedi)));
                                    break;
                                case I_HAVE_IT:
                                    //Deploy Like My Father Before Me. You may not deploy Jedi (except Luke or Ahsoka).
                                    effectFilter = Filters.title(Title.Like_My_Father_Before_Me);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.Jedi, Filters.except(Filters.or(Filters.Luke, Filters.Ahsoka)));
                                    break;
                                case YOU_HAVE_THAT_POWER_TOO:
                                    //Deploy My Parents Were Strong. You may not deploy Jedi (except [Episode VII] Jedi).
                                    effectFilter = Filters.title(Title.My_Parents_Were_Strong);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.Jedi, Filters.except(Icon.EPISODE_VII));
                                    break;
                            }
                            action.appendEffect(
                                    new LightSideGoesFirstEffect(action));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, effectFilter, false)
                            );
                            action.appendEffect(
                                    new AddUntilEndOfGameModifierEffect(action,
                                            new MayNotPlayModifier(self, cardsThatMayNotDeployFilter, self.getOwner()), "")
                            );
                            action.appendEffect(
                                    new SetWhileInPlayDataEffect(action, self, new WhileInPlayData(result))
                            );

                        }
                    })
            );

            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        return "Chosen option: " + self.getWhileInPlayData().getTextValue();
    }
}