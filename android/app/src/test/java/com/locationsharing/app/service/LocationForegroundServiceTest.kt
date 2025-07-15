package com.locationsharing.app.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.locationsharing.app.data.models.LocationData
import com.locationsharing.app.domain.location.LocationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@ExtendWith(MockitoExtension::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationForegroundServiceTest {

    @Mock
    private lateinit var locationService: LocationService

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var service: LocationForegroundService

    @BeforeEach
    fun setup() {
        service = Robolectric.setupService(LocationForegroundService::class.java)
        
        // Inject mocked location service
        val field = LocationForegroundService::class.java.getDeclaredField("locationService")
        field.isAccessible = true
        field.set(service, locationService)
    }

    @Test
    fun `onStartCommand starts location tracking`() {
        // Given
        whenever(locationService.getLocationUpdates()).thenReturn(flowOf(LocationData.Loading))
        val intent = Intent()

        // When
        service.onStartCommand(intent, 0, 1)

        // Then
        verify(locationService).startLocationUpdates()
    }

    @Test
    fun `onDestroy stops location tracking`() {
        // Given
        whenever(locationService.getLocationUpdates()).thenReturn(flowOf(LocationData.Loading))

        // When
        service.onDestroy()

        // Then
        verify(locationService).stopLocationUpdates()
    }

    @Test
    fun `startService creates and starts foreground service`() = runTest {
        // Given
        val context = mock<Context>()

        // When
        LocationForegroundService.startService(context)

        // Then
        verify(context).startForegroundService(any<Intent>())
    }

    @Test
    fun `stopService stops the service`() {
        // Given
        val context = mock<Context>()

        // When
        LocationForegroundService.stopService(context)

        // Then
        verify(context).stopService(any<Intent>())
    }
}