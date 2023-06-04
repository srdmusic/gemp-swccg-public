package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Assembly Area
 */
public class Card501_068 extends AbstractSite {
    public Card501_068() {
        super(Side.LIGHT, "Assembly Area", Uniqueness.DIAMOND_1, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Deploys only to same system as Clone Command Center. Your droids are power +1 here.");
        setLocationLightSideGameText("During your move phase, a pair of [Clone Army] characters may move between here and a site you occupy.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.CLONE_ARMY, Icon.VIRTUAL_SET_21);
        setTestingText("Assembly Area");
    }

    @Override
    public boolean mayNotBePartOfSystem(SwccgGame game, String system) {
        return Filters.filterTopLocationsOnTable(game, Filters.and(Filters.titleContains(Title.Clone_Command_Center), Filters.partOfSystem(system))).isEmpty();
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.droid, Filters.here(self)), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(final String playerOnLightSideOfLocation, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        final Filter siteYouOccupy = Filters.and(Filters.other(self), Filters.site, Filters.occupies(playerOnLightSideOfLocation));

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId, Phase.MOVE)
                && GameConditions.canSpotLocation(game, siteYouOccupy)
                && GameConditions.canUseForce(game, playerOnLightSideOfLocation, 1)) {

            final Filter characterFilter = Filters.and(Filters.your(playerOnLightSideOfLocation), Icon.CLONE_ARMY, Filters.character, Filters.hasNotPerformedRegularMove);

            //TODO choose the site first (copy MoveUsingLocationTextAction)
            if (GameConditions.canSpot(game, self, Filters.and(Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0)),
                    Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0))), Filters.here(self)))) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
                action.setText("Move from here to a site you occupy");
                action.appendUsage(
                        new OncePerPhaseEffect(action));


                action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a [Clone Army] character",
                        Filters.and(Filters.or(Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0), Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0)))),
                                Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0), Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(siteYouOccupy, 0))))), Filters.here(self))) {
                    @Override
                    protected void cardTargeted(final int targetGroupId1, final PhysicalCard targetedCharacter1) {
                        action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a [Clone Army] charater", Filters.and(Icon.CLONE_ARMY, Filters.character, Filters.here(self), Filters.canBeRelocated(false))) {
                            @Override
                            protected void cardTargeted(final int targetGroupId2, final PhysicalCard targetedCharacter2) {
                                action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a location to relocate to", Filters.and(siteYouOccupy, Filters.locationCanBeRelocatedTo(targetedCharacter1, 0), Filters.locationCanBeRelocatedTo(targetedCharacter2, 0))) {
                                    @Override
                                    protected void cardTargeted(final int targetGroupId_site, PhysicalCard targetedSite) {

                                        //TODO account for free movement
                                        action.appendCost(
                                                new UseForceEffect(action, playerOnLightSideOfLocation, 1));
                                        action.allowResponses(new RespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                PhysicalCard character1 = action.getPrimaryTargetCard(targetGroupId1);
                                                PhysicalCard character2 = action.getPrimaryTargetCard(targetGroupId2);
                                                PhysicalCard site = action.getPrimaryTargetCard(targetGroupId_site);

                                                Collection<PhysicalCard> toMove = new HashSet<>();
                                                toMove.add(character1);
                                                toMove.add(character2);
                                                action.appendEffect(
                                                        new RelocateBetweenLocationsEffect(action, toMove, site, true));
                                            }
                                        });
                                    }
                                });
                            }
                        });

                    }
                });
                actions.add(action);
            }

            if (GameConditions.canSpot(game, self,Filters.and(Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0)), Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0))), Filters.at(siteYouOccupy)))) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
                action.setText("Move from a site you occupy to here");
                action.appendUsage(
                        new OncePerPhaseEffect(action));

                action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a [Clone Army] character",
                        Filters.and(Filters.or(Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0), Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0)))),
                                Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0), Filters.with(self, Filters.and(characterFilter, Filters.canBeRelocatedToLocation(self, 0))))), Filters.at(siteYouOccupy))) {
                    @Override
                    protected void cardTargeted(final int targetGroupId1, final PhysicalCard targetedCharacter1) {
                        action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a [Clone Army] character", Filters.and(characterFilter, Filters.with(targetedCharacter1), Filters.canBeRelocatedToLocation(self, 0))) {
                            @Override
                            protected void cardTargeted(final int targetGroupId2, final PhysicalCard targetedCharacter2) {

                                //TODO account for free movement
                                action.appendCost(
                                        new UseForceEffect(action, playerOnLightSideOfLocation, 1));
                                action.allowResponses(new RespondableEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        PhysicalCard character1 = action.getPrimaryTargetCard(targetGroupId1);
                                        PhysicalCard character2 = action.getPrimaryTargetCard(targetGroupId2);

                                        Collection<PhysicalCard> toMove = new HashSet<>();
                                        toMove.add(character1);
                                        toMove.add(character2);
                                        action.appendEffect(
                                                new RelocateBetweenLocationsEffect(action, toMove, self, true));
                                    }
                                });
                            }
                        });

                    }
                });

                actions.add(action);
            }
        }


        return actions;
    }
}