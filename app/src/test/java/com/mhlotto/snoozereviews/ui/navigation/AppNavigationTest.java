package com.mhlotto.snoozereviews.ui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.mhlotto.snoozereviews.R;

import org.junit.Test;

public class AppNavigationTest {
    @Test
    public void menuIdsMapToDestinations() {
        assertEquals(AppNavigation.Destination.HISTORY, AppNavigation.destinationForMenuItem(R.id.action_history));
        assertEquals(AppNavigation.Destination.STATS, AppNavigation.destinationForMenuItem(R.id.action_stats));
        assertEquals(AppNavigation.Destination.ADD_BY_DATE, AppNavigation.destinationForMenuItem(R.id.action_add_by_date));
    }

    @Test
    public void unknownMenuIdIsNotConsumed() {
        assertNull(AppNavigation.destinationForMenuItem(123456));
    }

    @Test
    public void destinationsMapToActiveMenuIds() {
        assertEquals(R.id.action_history, AppNavigation.menuItemIdForDestination(AppNavigation.Destination.HISTORY));
        assertEquals(R.id.action_stats, AppNavigation.menuItemIdForDestination(AppNavigation.Destination.STATS));
        assertEquals(R.id.action_add_by_date, AppNavigation.menuItemIdForDestination(AppNavigation.Destination.ADD_BY_DATE));
    }
}
