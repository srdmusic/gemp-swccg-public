package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: You Cannot Escape Your Destiny
 */
public class Card501_057 extends AbstractNormalEffect {
    public Card501_057() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE,"You Cannot Escape Your Destiny", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If I Feel The Conflict on table, deploy on table. Once per turn, may deploy a docking bay or His Destiny from Reserve Deck; reshuffle. If you played a [Death Star II] Interrupt during your move phase, may relocate Luke to a battleground (or Emperor's) site. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("You Cannot Escape Your Destiny");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.I_Feel_The_Conflict);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.YOU_CANNOT_ESCAPE_YOUR_DESTINY__DEPLOY_CARD_FROM_RESERVE_DECK;

        if(GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)){

            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy His Destiny or a docking bay");

            action.appendUsage(
                    new OncePerTurnEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.title(Title.His_Destiny), Filters.docking_bay), true)
            );
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isPlayingCard(game, effect, playerId, Filters.and(Icon.DEATH_STAR_II, Filters.Interrupt))
                && GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.canTarget(game, self, Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(Filters.or(Filters.battleground_site, Filters.sameSiteAs(self, Filters.Emperor)), 0)))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Relocate Luke");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Luke", Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(Filters.or(Filters.battleground_site, Filters.sameSiteAs(self, Filters.Emperor)), 0))) {
                @Override
                protected void cardTargeted(final int targetGroupId1, PhysicalCard targetedCard) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose location", Filters.and(Filters.or(Filters.battleground_site, Filters.sameSiteAs(self, Filters.Emperor)), Filters.site, Filters.locationCanBeRelocatedTo(targetedCard, 0))) {
                        @Override
                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedCard) {

                            action.allowResponses(new RespondableEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalLuke = action.getPrimaryTargetCard(targetGroupId1);
                                    PhysicalCard finalLocation = action.getPrimaryTargetCard(targetGroupId2);

                                    action.appendEffect(
                                            new RelocateBetweenLocationsEffect(action, finalLuke, finalLocation));
                                }
                            });
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }

        return null;
    }
}