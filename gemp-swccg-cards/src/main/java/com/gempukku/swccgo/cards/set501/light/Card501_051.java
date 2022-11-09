package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Device
 * Title: Mercenary Armor (V)
 */
public class Card501_051 extends AbstractCharacterDevice {
    public Card501_051() {
        super(Side.LIGHT, 3, Title.Mercenary_Armor, Uniqueness.RESTRICTED_2, ExpansionSet.SET_20, Rarity.V);
        setLore("Worn by hired guns throughout the galaxy. Often used by Rebels when infiltrating underworld organizations. Leia wore Boushh's armor when she infiltrated Black Sun.");
        setGameText("Deploy on your Rebel or Alien. Imperial Barrier is canceled. " +
                    "In battles here, opponent may not cancel destinies. " +
                    "If on Leia or Chewie, they are power and defense value +2, " +
                    "and may use 1 Force to return this card to hand.");
        addIcons(Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        setTestingText("Mercenary Armor (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.or(Filters.Rebel, Filters.alien));
    }

    @Override
    protected Filter getGameTextValidToUseDeviceFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.and(Filters.your(self), Filters.or(Filters.Rebel, Filters.alien));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter hasAttached = Filters.and(Filters.or(Filters.Leia, Filters.Chewie), Filters.hasAttached(self));

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, hasAttached, 2));
        modifiers.add(new DefenseValueModifier(self, hasAttached, 2));
        modifiers.add(new MayNotCancelBattleDestinyModifier(self, self.getOwner(), new InBattleCondition(self)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        if(GameConditions.isAttachedTo(game, self, Filters.or(Filters.Chewie, Filters.Leia))
            && GameConditions.canUseForce(game, playerId, 1)){
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Return this card to hand");
            action.setActionMsg("Return this card to hand");
            action.appendCost(
                    new UseForceEffect(action, playerId, 1)
            );
            action.appendEffect(
                    new ReturnCardToHandFromTableEffect(action, self)
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(SwccgGame game, Effect effect, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Imperial_Barrier)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }
}
