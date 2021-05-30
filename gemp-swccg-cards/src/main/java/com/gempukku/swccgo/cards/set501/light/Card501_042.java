package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Han Solo, Optimistic General
 */
public class Card501_042 extends AbstractRebel {
    public Card501_042() {
        super(Side.LIGHT, 1, 4, 4, 3, 6, "Han Solo, Optimistic General", Uniqueness.UNIQUE);
        setLore("Leader. Scout.");
        setGameText("May be revealed by I Want That Map as a Resistance Agent. Adds 3 to the power of anything he pilots. Kylo's game text here is canceled. May add one destiny to total power with Chewie or [E] Leia. [Endor] scouts are destiny +1.");
        addIcons(Icon.WARRIOR, Icon.PILOT, Icon.ENDOR, Icon.VIRTUAL_SET_15);
        addPersona(Persona.HAN);
        addKeywords(Keyword.LEADER, Keyword.SCOUT, Keyword.GENERAL);
        setTestingText("Han Solo, Optimistic General");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayBeRevealedAsResistanceAgentModifier(self, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new CancelsGameTextModifier(self, Filters.and(Filters.Kylo, Filters.here(self))));
        modifiers.add(new AddsDestinyToPowerModifier(self, new WithCondition(self, Filters.or(Filters.Chewie, Filters.and(Filters.icon(Icon.ENDOR), Filters.Leia))), 1));
        modifiers.add(new DestinyModifier(self, Filters.and(Filters.icon(Icon.ENDOR), Filters.scout), 1));
        return modifiers;
    }
}