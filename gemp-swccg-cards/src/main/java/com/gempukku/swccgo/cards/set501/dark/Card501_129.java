package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveGameTextCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Vader's Malediction
 */
public class Card501_129 extends AbstractNormalEffect {
    public Card501_129() {
        super(Side.DARK, 3, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Vader's Malediction", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("A symbol of the Dark Lord of the Sith, and of the seductive power of the dark side.");
        setGameText("Deploy on table. If Vader is your apprentice, his game text may not be canceled and if he controls [Special Edition] Chasm Walkway, opponent's Interrupts are Lost Interrupts. While Vader alone, armed with a [Death Star II] lightsaber, and present at a site, your Force drains there are +1. [Immune to Alter.]");
        addIcons(Icon.CLOUD_CITY, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Vader's Malediction");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition vaderApprenticeCondition = new Condition() {
            @Override
            public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                PhysicalCard rots = Filters.findFirstActive(gameState.getGame(), self, Filters.Revenge_Of_The_Sith);
                if (rots != null
                        && GameConditions.cardHasWhileInPlayDataSet(rots)
                        && rots.getWhileInPlayData().getTextValue() != null) {
                    return "Vader".equals(rots.getWhileInPlayData().getTextValue());
                }
                return false;
            }
        };
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, Filters.Vader, vaderApprenticeCondition));
        modifiers.add(new LostInterruptModifier(self, Filters.and(Filters.opponents(self), Filters.Interrupt), new AndCondition(vaderApprenticeCondition, new ControlsWithCondition(self, playerId, Filters.and(Icon.SPECIAL_EDITION, Filters.Chasm_Walkway), Filters.Vader))));
        Filter dsIILightsaber = Filters.and(Filters.icon(Icon.DEATH_STAR_II), Filters.lightsaber);
        Filter aloneAndPresentAtASite = Filters.and(Filters.alone, Filters.presentAt(Filters.site));
        Filter vaderArmedWithDsIILightsaberAloneAndPresentAtASite = Filters.and(Filters.Vader, Filters.armedWith(dsIILightsaber), aloneAndPresentAtASite);
        modifiers.add(new ForceDrainModifier(self, Filters.sameSiteAs(self, vaderArmedWithDsIILightsaberAloneAndPresentAtASite), 1, self.getOwner()));
        return modifiers;
    }
}