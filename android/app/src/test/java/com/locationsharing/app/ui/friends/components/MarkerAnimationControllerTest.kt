package com.locationsharing.app.ui.friends.components

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.LocationUpdateType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for MarkerAnimationController
 */
class MarkerAnimationControllerTest {
    
    private lateinit var controller: MarkerAnimationController
    
    @Before
    fun setUp() {
        controller = MarkerAnimationController()
    }
    
    @Test
    fun `getAnimationState should return new state for unknown friend`() {
        val state = controller.getAnimationState("unknown-friend")
        
        assertFalse(state.isVisible)
        assertFalse(state.shouldShowAppearAnimation)
        assertFalse(state.shouldShowMovementTrail)
    }
    
    @Test
    fun `processLocationUpdate with INITIAL_LOAD should set appear animation`() = runTest {
        val update = FriendLocationUpdate(
            friendId = "friend1",
            previousLocation = null,
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.INITIAL_LOAD
        )
        
        controller.processLocationUpdate(update)
        
        val state = controller.getAnimationState("friend1")
        assertTrue(state.shouldShowAppearAnimation)
        assertTrue(state.isVisible)
    }
    
    @Test
    fun `processLocationUpdate with FRIEND_APPEARED should set appear animation`() = runTest {
        val update = FriendLocationUpdate(
            friendId = "friend1",
            previousLocation = null,
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.FRIEND_APPEARED
        )
        
        controller.processLocationUpdate(update)
        
        val state = controller.getAnimationState("friend1")
        assertTrue(state.shouldShowAppearAnimation)
        assertTrue(state.isVisible)
        assertFalse(state.shouldShowMovementTrail)
    }
    
    @Test
    fun `processLocationUpdate with POSITION_CHANGE should set movement trail`() = runTest {
        val update = FriendLocationUpdate(
            friendId = "friend1",
            previousLocation = LatLng(37.7749, -122.4194),
            newLocation = LatLng(37.7750, -122.4195),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.POSITION_CHANGE
        )
        
        controller.processLocationUpdate(update)
        
        val state = controller.getAnimationState("friend1")
        assertTrue(state.shouldShowMovementTrail)
        assertTrue(state.isMoving)
    }
    
    @Test
    fun `processLocationUpdate with STATUS_CHANGE should update pulse state`() = runTest {
        val onlineUpdate = FriendLocationUpdate(
            friendId = "friend1",
            previousLocation = LatLng(37.7749, -122.4194),
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.STATUS_CHANGE
        )
        
        controller.processLocationUpdate(onlineUpdate)
        
        val state = controller.getAnimationState("friend1")
        assertTrue(state.shouldPulse)
        
        val offlineUpdate = onlineUpdate.copy(isOnline = false)
        controller.processLocationUpdate(offlineUpdate)
        
        assertFalse(state.shouldPulse)
    }
    
    @Test
    fun `processLocationUpdate with FRIEND_DISAPPEARED should set disappear animation`() = runTest {
        val update = FriendLocationUpdate(
            friendId = "friend1",
            previousLocation = LatLng(37.7749, -122.4194),
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = false,
            updateType = LocationUpdateType.FRIEND_DISAPPEARED
        )
        
        // First make friend visible
        val state = controller.getAnimationState("friend1")
        state.isVisible = true
        
        controller.processLocationUpdate(update)
        
        assertTrue(state.shouldShowDisappearAnimation)
        // Note: isVisible becomes false after delay in actual implementation
    }
    
    @Test
    fun `resetAnimationState should reset friend state`() {
        val state = controller.getAnimationState("friend1")
        state.isVisible = true
        state.shouldShowAppearAnimation = true
        state.shouldShowMovementTrail = true
        
        controller.resetAnimationState("friend1")
        
        val newState = controller.getAnimationState("friend1")
        assertFalse(newState.isVisible)
        assertFalse(newState.shouldShowAppearAnimation)
        assertFalse(newState.shouldShowMovementTrail)
    }
    
    @Test
    fun `clearAllStates should reset all friend states`() {
        // Create states for multiple friends
        controller.getAnimationState("friend1").isVisible = true
        controller.getAnimationState("friend2").isVisible = true
        controller.getAnimationState("friend3").isVisible = true
        
        controller.clearAllStates()
        
        // All states should be reset to default
        val state1 = controller.getAnimationState("friend1")
        val state2 = controller.getAnimationState("friend2")
        val state3 = controller.getAnimationState("friend3")
        
        assertFalse(state1.isVisible)
        assertFalse(state2.isVisible)
        assertFalse(state3.isVisible)
    }
}