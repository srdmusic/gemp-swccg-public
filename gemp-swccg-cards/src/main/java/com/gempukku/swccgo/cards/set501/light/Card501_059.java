package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebelResistance;
import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.RevealTopCardsOfCardPileAndTakeCardsIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.ShuffleReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Resistance
 * Title: Lando, Hero of the Rebellion
 */
public class Card501_059 extends AbstractResistance {
    public Card501_059() {
        super(Side.LIGHT, 1, 3, 2, 3, 5, "Lando, Hero of the Rebellion", Uniqueness.UNIQUE);
        setLore("Leader. Resistance Agent.");
        setGameText("Deploys -1 aboard Falcon. Adds one destiny to total power with Chewie or Jannah (or while piloting). While piloting a freighter, adds 1 to maneuver and your starships at same system are immune to Lateral Damage and Overwhelmed.");
        addPersona(Persona.LANDO);
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_16, Icon.PILOT, Icon.WARRIOR);
        addKeywords(Keyword.LEADER, Keyword.RESISTANCE_AGENT);
        setMatchingStarshipFilter(Filters.Falcon);
        setTestingText("Lando, Hero Of The Rebellion (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostAboardModifier(self, -1, Persona.FALCON));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        PilotingCondition pilotingCondition = new PilotingCondition(self);
        WithCondition withJannahOrChewieCondition = new WithCondition(self, Filters.or(Filters.Chewie, Filters.Jannah));
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new AddsDestinyToPowerModifier(self, new OrCondition(pilotingCondition, withJannahOrChewieCondition), 1));
        modifiers.add(new ManeuverModifier(self, Filters.and(Filters.hasPiloting(self), Filters.freighter), 1));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.your(self), Filters.starship, Filters.atSameSystem(self)), new PilotingCondition(self, Filters.freighter), Title.Lateral_Damage));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.your(self), Filters.starship, Filters.atSameSystem(self)), new PilotingCondition(self, Filters.freighter), Title.Overwhelmed));
        return modifiers;
    }
}
