package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Daughter Of Skywalker (V)
 */
public class Card501_014 extends AbstractRebel {
    public Card501_014() {
        super(Side.LIGHT, 1, 5, 4, 5, 8, Title.Daughter_Of_Skywalker, Uniqueness.UNIQUE);
        setLore("Scout. Leader. Made friends with Wicket. Negotiated an alliance with the Ewoks. Leia found out the truth about her father from Luke in the Ewok village.");
        setGameText("Adds 1 [LS] Icon here. When in battle, may target one opponent’s character present. Draw destiny. Target’s gametext canceled and power -2 if destiny > ability. Scouts here are immune to Sniper, You Are Beaten and to attrition <4.");
        addPersona(Persona.LEIA);
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_15, Icon.WARRIOR);
        addKeywords(Keyword.SCOUT, Keyword.LEADER, Keyword.FEMALE);
        setVirtualSuffix(true);
        setTestingText("Daughter Of Skywalker (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        return modifiers;
    }
}
