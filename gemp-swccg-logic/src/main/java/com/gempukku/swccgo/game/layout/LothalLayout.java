package com.gempukku.swccgo.game.layout;

import com.gempukku.swccgo.filters.Filters;

/**
 * Represents the location layout for Lothal.
 */
public class LothalLayout extends AbstractSystemLayout {

    // Layout order for Lothal:
    //  1) Sites (in forward or reverse order)
    //      A) Interior sites (except Jedi Temple)
    //      B) Capital City
    //      C) Exterior sites
    //      D) Jedi Temple
    //  2) Clouds
    //  3) Planet
    //  4) Asteroids
    //  5) Big One
    //  6) Big One: Asteroid Cave

    /**
     * Needed to generate snapshot.
     */
    public LothalLayout() {
    }

    /**
     * Creates the location layout for Tatooine.
     * @param systemName the system name
     * @param parsec the parsec number for the system
     */
    public LothalLayout(String systemName, int parsec) {
        super(systemName, parsec);

        //  1) Sites (in forward or reverse order)
        _groupOrders.add(
                new LocationReversibleGroupOrder(
                        // A) Interior sites (except Jedi Temple)
                        new LocationGroup("Interior sites", Filters.and(Filters.interior_site,
                                Filters.not(Filters.or(Filters.exterior_site, Filters.Lothal_Jedi_Temple)), Filters.partOfSystem(systemName))),
                        //  B) Capital City
                        new LocationGroup("Capital City", Filters.Lothal_Capital_City),
                        //  C) Exterior sites
                        new LocationGroup("Exterior sites", Filters.and(Filters.exterior_site, Filters.not(Filters.or(Filters.interior_site,
                                Filters.Lothal_Capital_City)), Filters.partOfSystem(systemName))),
                        //  D) Jedi Temple
                        new LocationGroup("Jedi Temple", Filters.Lothal_Jedi_Temple)));

        //  2) Clouds
        //  3) Planet
        //  4) Asteroids
        //  5) Big One
        //  6) Big One: Asteroid Cave
        _groupOrders.add(
                new LocationFixedGroupOrder(getPlanetSystemAndSectorsLocationGroups(systemName)));
    }
}
