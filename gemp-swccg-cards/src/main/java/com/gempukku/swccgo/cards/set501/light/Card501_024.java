package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.conditions.InBattleAtCondition;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayBeFiredTwicePerBattleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: TK-422 (V)
 */
public class Card501_024 extends AbstractRebel {
    public Card501_024() {
        super(Side.LIGHT, 1, 3, 3, 3, 6, Title.TK422, Uniqueness.UNIQUE);
        setArmor(5);
        setLore("Corellian smuggler. Spy. Han stole the armor and identity of an enemy soldier that boarded the Millennium Falcon. Bluffed his way into the detention area.");
        setGameText("Adds 3 to anything he pilots. Stormtrooper. Deploys only if Cell 2187 on table. Han's weapon destiny draws are +1 for each trooper here. In battle, if opponent has more characters participating than you, Han may fire a blaster a second time.");
        addPersona(Persona.HAN);
        addIcons(Icon.SPECIAL_EDITION, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.SPY, Keyword.SMUGGLER, Keyword.STORMTROOPER);
        setSpecies(Species.CORELLIAN);
        setVirtualSuffix(true);
        setTestingText("TK-422 (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Cell_2187);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.any, self, new HereEvaluator(self, Filters.trooper)));
        modifiers.add(new MayBeFiredTwicePerBattleModifier(self, Filters.and(Filters.blaster, Filters.attachedTo(self)), new InBattleAtCondition(self, Filters.wherePlayerHasFewerCharacters(self, self.getOwner()))));
        return modifiers;
    }
}
