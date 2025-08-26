package com.locationsharing.app.navigation

/**
 * Sealed class representing all navigation destinations in the FFinder app.
 * Each screen has a unique route string for navigation.
 */
sealed class Screen(val route: String, val title: String) {
    object HOME : Screen("home", "Home")
    object MAP : Screen("map", "Map")
    object FRIENDS : Screen("friends", "Friends")
    object SETTINGS : Screen("settings", "Settings")
    object INVITE_FRIENDS : Screen("invite_friends", "Invite Friends")
    object SEARCH_FRIENDS : Screen("search_friends", "Search Friends")
    object FRIENDS_HUB : Screen("friends_hub", "Friends Hub")
    
    companion object {
        /**
         * Get all available screens as a list.
         */
        fun getAllScreens(): List<Screen> = listOf(
            HOME, MAP, FRIENDS, SETTINGS, INVITE_FRIENDS, SEARCH_FRIENDS, FRIENDS_HUB
        )
        
        /**
         * Find screen by route string.
         */
        fun fromRoute(route: String): Screen? = when (route) {
            HOME.route -> HOME
            MAP.route -> MAP
            FRIENDS.route -> FRIENDS
            SETTINGS.route -> SETTINGS
            INVITE_FRIENDS.route -> INVITE_FRIENDS
            SEARCH_FRIENDS.route -> SEARCH_FRIENDS
            FRIENDS_HUB.route -> FRIENDS_HUB
            else -> null
        }
    }
}