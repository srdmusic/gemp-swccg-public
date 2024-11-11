package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FireWeaponEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyWhenDrawnForDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Fennec Shand
 */
public class Card501_070 extends AbstractAlien {
    public Card501_070() {
        super(Side.DARK, 2, 3, 4, 2, 4, "Fennec Shand", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setArmor(4);
        setLore("Female assassin, bounty hunter, and mercenary.");
        setGameText("Destiny +2 if Quietly Observing on table. If alone, draws one battle destiny if unable to otherwise. Once per game, Fennec Shand may fire a weapon in your control phase.");
        addPersona(Persona.FENNEC_SHAND);
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.FEMALE, Keyword.ASSASSIN, Keyword.BOUNTY_HUNTER, Keyword.MERCENARY);
        setTestingText("Fennec Shand (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DestinyWhenDrawnForDestinyModifier(self, new OnTableCondition(self, Filters.Quietly_Observing), 2));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, new AloneCondition(self), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.FENNEC_SHAND__FIRE_WEAPON;
        
        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, self, Phase.CONTROL)) {
            if (GameConditions.isArmedWith(game, self, Filters.weapon)) {

                Filter weaponFilter = Filters.and(Filters.weapon, Filters.attachedTo(self));
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Fire a weapon");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose weapon to fire", weaponFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard weapon) {
                                action.addAnimationGroup(weapon);
                                // Allow response(s)
                                action.allowResponses("Fire " + GameUtils.getCardLink(weapon),
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                final PhysicalCard finalWeapon = action.getPrimaryTargetCard(targetGroupId);
                                                action.appendUsage(
                                                        new OncePerGameEffect(action));
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new FireWeaponEffect(action, finalWeapon, false, Filters.canBeTargetedBy(self)));
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
