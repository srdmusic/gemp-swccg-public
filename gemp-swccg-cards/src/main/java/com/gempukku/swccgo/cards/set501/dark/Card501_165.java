package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDrawMoreThanBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Starship
 * Subtype: Starfighter
 * Title: Zuckuss And 4-LOM In Mist Hunter
 */
public class Card501_165 extends AbstractStarfighter {
    public Card501_165() {
        super(Side.DARK, 2, 6, 5, null, 5, 5, 7, "Zuckuss And 4-LOM In Mist Hunter", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Zuckuss is a dangerous adversary, especially when aboard his own starship. Mystical omens enable the Gand to predict enemy maneuvers in starship combat.");
        setGameText("May add 2 passengers. Permanent pilots are Zuckuss and 4-LOM, who provide 4 ability and adds one destiny to total power. Players may not draw more than one battle destiny here. Immune to attrition < 5.");
        addPersonas(Persona.MIST_HUNTER);
        addIcons(Icon.INDEPENDENT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_21);
        addIcon(Icon.PILOT, 2);
        addModelType(ModelType.BYBLOS_G1A_TRANSPORT);
        setPassengerCapacity(2);
        setTestingText("Zuckuss And 4-LOM In Mist Hunter");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(Persona.ZUCKUSS, 4)  {
            @Override
            public List<Modifier> getGameTextModifiers(PhysicalCard self) {
                List<Modifier> modifiers = new LinkedList<>();
                modifiers.add(new AddsDestinyToPowerModifier(self, 1));
                return modifiers;
            }
        });
        permanentsAboard.add(new AbstractPermanentPilot(Persona._4_LOM, 0) {
        });
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDrawMoreThanBattleDestinyModifier(self, Filters.here(self), 1, self.getOwner()));
        modifiers.add(new MayNotDrawMoreThanBattleDestinyModifier(self, Filters.here(self), 1, game.getOpponent(self.getOwner())));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }
}
