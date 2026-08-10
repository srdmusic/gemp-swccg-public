package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;


/**
 * Set: Playtesting
 * Type: Effect
 * Title: Behind Everything
 */
public class Card501_018 extends AbstractNormalEffect {
    public Card501_018() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Behind Everything", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Mysterious Sith Master who is manipulating the Trade Federation for his own nefarious ends. Shrouded in mystery, his identity and agenda remain unclear.");
        setGameText("If Revenge Of The Sith on table, deploy on table. While Sidious alone on Coruscant, he adds one [Dark Side] icon and one [Light Side] icon there. If your apprentice in battle, Blast The Door, Kid! and It's A Trap! may not be played and you may choose a player to activate 1 Force. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.SIDIOUS, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Behind Everything");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Revenge_Of_The_Sith);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        Condition sidiousAloneOnCoruscantCondition = new OnTableCondition(self, Filters.and(Filters.Sidious, Filters.alone, Filters.On_Coruscant));
        Condition yourApprenticeInBattle = new OnTableCondition(self, Filters.and(Filters.your(self), Filters.Sith_Apprentice, Filters.participatingInBattle));

        modifiers.add(new IconModifier(self, Filters.sameLocationAs(self, Filters.Sidious), sidiousAloneOnCoruscantCondition, Icon.DARK_FORCE, 1));
        modifiers.add(new IconModifier(self, Filters.sameLocationAs(self, Filters.Sidious), sidiousAloneOnCoruscantCondition, Icon.LIGHT_FORCE, 1));
        modifiers.add(new MayNotPlayModifier(self, Filters.or(Filters.Blast_The_Door_Kid, Filters.Its_A_Trap), yourApprenticeInBattle));

        return modifiers;
    }
}
