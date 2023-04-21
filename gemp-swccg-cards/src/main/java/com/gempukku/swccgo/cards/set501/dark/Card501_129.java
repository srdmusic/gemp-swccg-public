package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CancelDestinyAndCauseRedrawEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveGameTextCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Vader's Cape (V)
 */
public class Card501_129 extends AbstractNormalEffect {
    public Card501_129() {
        super(Side.DARK, 3, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Vader's Cape", Uniqueness.UNIQUE, ExpansionSet.SET_21, Rarity.V);
        setLore("A symbol of the Dark Lord of the Sith, and of the seductive power of the dark side.");
        setGameText("Deploy on table. Vader’s game text may not be canceled. While Vader armed with a [DSII] lightsaber and is alone and present at a site, your Force drains there are +1. May lose Effect to cancel and re-draw a weapon destiny targeting Vader. [Immune to Alter.]");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Vader's Cape (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, Filters.Vader));
        Filter dsIILightsaber = Filters.and(Filters.icon(Icon.DEATH_STAR_II), Filters.lightsaber);
        Filter aloneAndPresentAtASite = Filters.and(Filters.alone, Filters.presentAt(Filters.site));
        Filter vaderArmedWithDsIILightsaberAloneAndPresentAtASite = Filters.and(Filters.Vader, Filters.armedWith(dsIILightsaber), aloneAndPresentAtASite);
        modifiers.add(new ForceDrainModifier(self, Filters.sameSiteAs(self, vaderArmedWithDsIILightsaberAloneAndPresentAtASite), 1, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isWeaponDestinyJustDrawnTargeting(game, effectResult, Filters.opponents(playerId), Filters.Vader)
                && GameConditions.canCancelDestinyAndCauseRedraw(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Cancel destiny and cause re-draw");
            // Pay cost(s)
            action.appendCost(
                    new LoseCardFromTableEffect(action, self));
            // Perform result(s)
            action.appendEffect(
                    new CancelDestinyAndCauseRedrawEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}