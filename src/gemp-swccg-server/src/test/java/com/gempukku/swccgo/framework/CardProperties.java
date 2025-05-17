package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCardImpl;

import java.util.List;

/**
 * Functions for checking aspects of a card, such as numeric states and other state-based properties.
 *
 * If you are just checking the printed stats, then you can retrieve the blueprint of a card and check those values
 * directly.  These helper functions are more for live cards which may be affected by modifiers sourced from other cards.
 */
public interface CardProperties extends TestBase {


    default List<PhysicalCardImpl> GetAttachedCards(PhysicalCardImpl card) {
        return (List<PhysicalCardImpl>)(List<?>)gameState().getAttachedCards(card);
    }

    default List<PhysicalCardImpl> GetStackedCards(PhysicalCardImpl card) {
        return (List<PhysicalCardImpl>)(List<?>)gameState().getStackedCards(card);
    }



    default boolean IsAttachedTo(PhysicalCardImpl card, PhysicalCardImpl bearer) {
        if(card.getZone() != Zone.ATTACHED) {
            return false;
        }

        return bearer == card.getAttachedTo();
    }


	default float GetDestiny(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getDestiny(gameState(), card);
	}

	default float GetAbility(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getAbility(gameState(), card);
	}

	default float GetBattleDestinyAbility(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getAbilityForBattleDestiny(gameState(), card);
	}

	default float GetArmor(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getArmor(gameState(), card);
	}

	default float GetDeployCost(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getDeployCost(gameState(), card);
	}

	default float GetForfeit(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getForfeit(gameState(), card);
	}

	default float GetHyperspeed(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getHyperspeed(gameState(), card);
	}

	default float GetIconCount(PhysicalCardImpl card, Icon icon)
	{
		return game().getModifiersQuerying().getIconCount(gameState(), card, icon);
	}

    default boolean HasKeyword(PhysicalCardImpl card, Keyword keyword)
    {
        return game().getModifiersQuerying().hasKeyword(gameState(), card, keyword);
    }


    default boolean HasType(PhysicalCardImpl card, CardType type)
    {
        return  game().getModifiersQuerying().getCardTypes(gameState(), card).contains(type);
    }

}
