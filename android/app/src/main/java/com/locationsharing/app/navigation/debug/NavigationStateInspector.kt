package com.locationsharing.app.navigation.debug

import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Inspector for navigation state that provides detailed information about
 * navigation controller state, back stack, and route parameters.
 */
class NavigationStateInspector {
    
    private val _inspectionData = MutableStateFlow(NavigationInspectionData())
    val inspectionData: StateFlow<NavigationInspectionData> = _inspectionData.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Inspects the current navigation state and updates inspection data.
     */
    fun inspectNavigationState(
        navController: NavController?,
        navigationState: NavigationState
    ) {
        val currentDestination = navController?.currentDestination
        val backStack = navController?.currentBackStack?.value ?: emptyList()
        
        val inspectionData = NavigationInspectionData(
            timestamp = dateFormatter.format(Date()),
            currentRoute = currentDestination?.route,
            currentDestinationId = currentDestination?.id?.toString(),
            currentArguments = extractArguments(currentDestination),
            backStackSize = backStack.size,
            backStackEntries = backStack.map { entry ->
                BackStackEntryInfo(
                    route = entry.destination.route ?: "Unknown",
                    id = entry.destination.id.toString(),
                    arguments = entry.arguments?.let { bundle ->
                        bundle.keySet().associateWith { key ->
                            bundle.get(key)?.toString() ?: "null"
                        }
                    } ?: emptyMap()
                )
            },
            navigationState = navigationState,
            canNavigateUp = navController?.previousBackStackEntry != null,
            isNavigationControllerValid = navController != null,
            routeValidation = validateCurrentRoute(currentDestination?.route)
        )
        
        _inspectionData.value = inspectionData
    }
    
    /**
     * Validates the current route for potential issues.
     */
    private fun validateCurrentRoute(route: String?): RouteValidationResult {
        if (route == null) {
            return RouteValidationResult(
                isValid = false,
                issues = listOf("Route is null")
            )
        }
        
        val issues = mutableListOf<String>()
        
        // Check if route matches known screens
        val knownRoutes = Screen.getAllScreens().map { it.route }
        if (route !in knownRoutes) {
            issues.add("Route '$route' is not in known screens")
        }
        
        // Check for malformed route patterns
        if (route.contains("//")) {
            issues.add("Route contains double slashes")
        }
        
        if (route.startsWith("/") && route.length > 1) {
            issues.add("Route should not start with slash")
        }
        
        return RouteValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Extracts arguments from the current destination.
     */
    private fun extractArguments(destination: NavDestination?): Map<String, String> {
        return destination?.arguments?.mapValues { (_, argument) ->
            "Type: ${argument.type}, Default: ${argument.defaultValue}, Nullable: ${argument.isNullable}"
        } ?: emptyMap()
    }
    
    /**
     * Gets a detailed report of the current navigation state.
     */
    fun generateStateReport(): String {
        val data = _inspectionData.value
        
        return buildString {
            appendLine("=== Navigation State Report ===")
            appendLine("Timestamp: ${data.timestamp}")
            appendLine("Current Route: ${data.currentRoute ?: "None"}")
            appendLine("Destination ID: ${data.currentDestinationId ?: "None"}")
            appendLine("Navigation Controller Valid: ${data.isNavigationControllerValid}")
            appendLine("Can Navigate Up: ${data.canNavigateUp}")
            appendLine()
            
            appendLine("=== Navigation State ===")
            appendLine("Current Screen: ${data.navigationState.currentScreen.title}")
            appendLine("Can Navigate Back: ${data.navigationState.canNavigateBack}")
            appendLine("Is Navigating: ${data.navigationState.isNavigating}")
            appendLine("History Size: ${data.navigationState.navigationHistory.size}")
            appendLine()
            
            appendLine("=== Back Stack (${data.backStackSize} entries) ===")
            data.backStackEntries.forEachIndexed { index, entry ->
                appendLine("${index + 1}. Route: ${entry.route}")
                appendLine("   ID: ${entry.id}")
                if (entry.arguments.isNotEmpty()) {
                    appendLine("   Arguments:")
                    entry.arguments.forEach { (key, value) ->
                        appendLine("     $key: $value")
                    }
                }
                appendLine()
            }
            
            appendLine("=== Route Validation ===")
            appendLine("Valid: ${data.routeValidation.isValid}")
            if (data.routeValidation.issues.isNotEmpty()) {
                appendLine("Issues:")
                data.routeValidation.issues.forEach { issue ->
                    appendLine("  - $issue")
                }
            }
            
            if (data.currentArguments.isNotEmpty()) {
                appendLine()
                appendLine("=== Current Route Arguments ===")
                data.currentArguments.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
            }
        }
    }
    
    /**
     * Clears the inspection data.
     */
    fun clearInspectionData() {
        _inspectionData.value = NavigationInspectionData()
    }
}

/**
 * Data class containing detailed navigation inspection information.
 */
@Stable
data class NavigationInspectionData(
    val timestamp: String = "",
    val currentRoute: String? = null,
    val currentDestinationId: String? = null,
    val currentArguments: Map<String, String> = emptyMap(),
    val backStackSize: Int = 0,
    val backStackEntries: List<BackStackEntryInfo> = emptyList(),
    val navigationState: NavigationState = NavigationState(
        currentScreen = Screen.HOME,
        canNavigateBack = false,
        navigationHistory = emptyList(),
        isNavigating = false
    ),
    val canNavigateUp: Boolean = false,
    val isNavigationControllerValid: Boolean = false,
    val routeValidation: RouteValidationResult = RouteValidationResult()
)

/**
 * Information about a back stack entry.
 */
@Stable
data class BackStackEntryInfo(
    val route: String,
    val id: String,
    val arguments: Map<String, String> = emptyMap()
)

/**
 * Result of route validation.
 */
@Stable
data class RouteValidationResult(
    val isValid: Boolean = true,
    val issues: List<String> = emptyList()
)