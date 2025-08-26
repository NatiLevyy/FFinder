package com.locationsharing.app.navigation.security

import com.locationsharing.app.navigation.NavigationError
import timber.log.Timber

/**
 * Interface for validating navigation routes and ensuring secure navigation operations.
 */
interface RouteValidator {
    /**
     * Validates if a route is allowed for navigation.
     * 
     * @param route The route to validate
     * @param currentRoute The current route (optional)
     * @return ValidationResult indicating if the route is valid
     */
    fun validateRoute(route: String, currentRoute: String? = null): ValidationResult
    
    /**
     * Validates if navigation from one route to another is allowed.
     * 
     * @param fromRoute The source route
     * @param toRoute The destination route
     * @return ValidationResult indicating if the navigation is allowed
     */
    fun validateNavigation(fromRoute: String, toRoute: String): ValidationResult
    
    /**
     * Validates deep link parameters for security.
     * 
     * @param route The route from deep link
     * @param parameters The parameters from deep link
     * @return ValidationResult indicating if the deep link is safe
     */
    fun validateDeepLink(route: String, parameters: Map<String, String>): ValidationResult
}

/**
 * Result of route validation.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val error: SecurityError) : ValidationResult()
}

/**
 * Security errors that can occur during route validation.
 */
sealed class SecurityError {
    object InvalidRoute : SecurityError()
    object UnauthorizedNavigation : SecurityError()
    object MaliciousDeepLink : SecurityError()
    object SessionExpired : SecurityError()
    data class CustomError(val message: String) : SecurityError()
}

/**
 * Implementation of RouteValidator with comprehensive security checks.
 */
class RouteValidatorImpl : RouteValidator {
    
    companion object {
        private const val TAG = "RouteValidator"
        
        // Define allowed routes
        private val ALLOWED_ROUTES = setOf(
            "home",
            "map",
            "friends",
            "settings",
            "auth/login",
            "auth/register"
        )
        
        // Define restricted navigation patterns
        private val RESTRICTED_NAVIGATIONS = mapOf(
            "auth/login" to setOf("auth/register"),
            "auth/register" to setOf("auth/login")
        )
        
        // Define dangerous deep link patterns
        private val DANGEROUS_PATTERNS = listOf(
            "javascript:",
            "data:",
            "file:",
            "<script",
            "eval(",
            "document.cookie"
        )
    }
    
    override fun validateRoute(route: String, currentRoute: String?): ValidationResult {
        try {
            // Check if route is in allowed list
            if (!isRouteAllowed(route)) {
                Timber.w("$TAG: Invalid route attempted: $route")
                return ValidationResult.Invalid(SecurityError.InvalidRoute)
            }
            
            // Check for malicious patterns
            if (containsMaliciousPattern(route)) {
                Timber.w("$TAG: Malicious pattern detected in route: $route")
                return ValidationResult.Invalid(SecurityError.MaliciousDeepLink)
            }
            
            // Additional route-specific validation
            if (!validateRouteFormat(route)) {
                Timber.w("$TAG: Invalid route format: $route")
                return ValidationResult.Invalid(SecurityError.InvalidRoute)
            }
            
            return ValidationResult.Valid
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validating route: $route")
            return ValidationResult.Invalid(SecurityError.CustomError("Route validation failed"))
        }
    }
    
    override fun validateNavigation(fromRoute: String, toRoute: String): ValidationResult {
        try {
            // First validate both routes individually
            val fromValidation = validateRoute(fromRoute)
            if (fromValidation is ValidationResult.Invalid) {
                return fromValidation
            }
            
            val toValidation = validateRoute(toRoute)
            if (toValidation is ValidationResult.Invalid) {
                return toValidation
            }
            
            // Check for restricted navigation patterns
            val restrictedDestinations = RESTRICTED_NAVIGATIONS[fromRoute]
            if (restrictedDestinations?.contains(toRoute) == true) {
                Timber.w("$TAG: Restricted navigation from $fromRoute to $toRoute")
                return ValidationResult.Invalid(SecurityError.UnauthorizedNavigation)
            }
            
            return ValidationResult.Valid
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validating navigation from $fromRoute to $toRoute")
            return ValidationResult.Invalid(SecurityError.CustomError("Navigation validation failed"))
        }
    }
    
    override fun validateDeepLink(route: String, parameters: Map<String, String>): ValidationResult {
        try {
            // First validate the route
            val routeValidation = validateRoute(route)
            if (routeValidation is ValidationResult.Invalid) {
                return routeValidation
            }
            
            // Validate parameters for malicious content
            for ((key, value) in parameters) {
                if (containsMaliciousPattern(key) || containsMaliciousPattern(value)) {
                    Timber.w("$TAG: Malicious pattern in deep link parameter: $key=$value")
                    return ValidationResult.Invalid(SecurityError.MaliciousDeepLink)
                }
                
                // Check parameter length to prevent buffer overflow attacks
                if (key.length > 100 || value.length > 1000) {
                    Timber.w("$TAG: Deep link parameter too long: $key")
                    return ValidationResult.Invalid(SecurityError.MaliciousDeepLink)
                }
            }
            
            // Validate parameter count to prevent DoS attacks
            if (parameters.size > 20) {
                Timber.w("$TAG: Too many deep link parameters: ${parameters.size}")
                return ValidationResult.Invalid(SecurityError.MaliciousDeepLink)
            }
            
            return ValidationResult.Valid
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validating deep link: $route")
            return ValidationResult.Invalid(SecurityError.CustomError("Deep link validation failed"))
        }
    }
    
    private fun isRouteAllowed(route: String): Boolean {
        return ALLOWED_ROUTES.contains(route) || 
               ALLOWED_ROUTES.any { allowedRoute -> 
                   route.startsWith("$allowedRoute/") 
               }
    }
    
    private fun containsMaliciousPattern(input: String): Boolean {
        val lowerInput = input.lowercase()
        return DANGEROUS_PATTERNS.any { pattern -> 
            lowerInput.contains(pattern.lowercase()) 
        }
    }
    
    private fun validateRouteFormat(route: String): Boolean {
        // Check for valid route format (alphanumeric, slashes, hyphens, underscores)
        val validRoutePattern = Regex("^[a-zA-Z0-9/_-]+$")
        return validRoutePattern.matches(route) && 
               !route.contains("..") && // Prevent directory traversal
               !route.startsWith("/") && // Routes should be relative
               !route.endsWith("/") // Routes should not end with slash
    }
}