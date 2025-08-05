package com.locationsharing.app.data.discovery

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactMatchType
import com.locationsharing.app.domain.model.FriendRequestStatus
import com.locationsharing.app.domain.model.UserDiscoveryResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FirebaseUserDiscoveryService
 * Tests user discovery functionality with mocked Firebase responses
 */
class FirebaseUserDiscoveryServiceTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userDiscoveryService: FirebaseUserDiscoveryService
    
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockQuery: Query
    private lateinit var mockQuerySnapshot: QuerySnapshot
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    
    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockQuery = mockk(relaxed = true)
        mockQuerySnapshot = mockk(relaxed = true)
        mockDocumentSnapshot = mockk(relaxed = true)
        
        userDiscoveryService = FirebaseUserDiscoveryService(firestore, auth)
        
        // Mock Tasks.await()
        mockkStatic(Tasks::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(Tasks::class)
    }
    
    @Test
    fun `discoverUsers returns NoMatches for empty contact list`() = runTest {
        // Given
        val emptyContacts = emptyList<Contact>()
        
        // When
        val result = userDiscoveryService.discoverUsers(emptyContacts).first()
        
        // Then
        assertTrue(result is UserDiscoveryResult.NoMatches)
        assertEquals(0, (result as UserDiscoveryResult.NoMatches).totalContactsSearched)
    }
    
    @Test
    fun `discoverUsers returns NoMatches when no valid hashes generated`() = runTest {
        // Given
        val invalidContacts = listOf(
            Contact(
                id = "1",
                displayName = "Invalid Contact",
                phoneNumbers = emptyList(),
                emailAddresses = emptyList()
            )
        )
        
        // When
        val result = userDiscoveryService.discoverUsers(invalidContacts).first()
        
        // Then
        assertTrue(result is UserDiscoveryResult.NoMatches)
        assertEquals(1, (result as UserDiscoveryResult.NoMatches).totalContactsSearched)
    }
    
    @Test
    fun `hashContacts creates valid hashes for contacts with phone numbers`() = runTest {
        // Given
        val contacts = listOf(
            Contact(
                id = "1",
                displayName = "John Doe",
                phoneNumbers = listOf("+15551234567"),
                emailAddresses = emptyList()
            )
        )
        
        // When
        val contactHashes = userDiscoveryService.hashContacts(contacts)
        
        // Then
        assertEquals(1, contactHashes.size)
        val contactHash = contactHashes.first()
        assertNotNull(contactHash.hashedPhone)
        assertNull(contactHash.hashedEmail)
        assertTrue(contactHash.isValidForDiscovery)
    }
    
    @Test
    fun `hashContacts creates valid hashes for contacts with email addresses`() = runTest {
        // Given
        val contacts = listOf(
            Contact(
                id = "1",
                displayName = "Jane Doe",
                phoneNumbers = emptyList(),
                emailAddresses = listOf("jane@example.com")
            )
        )
        
        // When
        val contactHashes = userDiscoveryService.hashContacts(contacts)
        
        // Then
        assertEquals(1, contactHashes.size)
        val contactHash = contactHashes.first()
        assertNull(contactHash.hashedPhone)
        assertNotNull(contactHash.hashedEmail)
        assertTrue(contactHash.isValidForDiscovery)
    }
    
    @Test
    fun `hashContacts creates composite hash for contacts with both phone and email`() = runTest {
        // Given
        val contacts = listOf(
            Contact(
                id = "1",
                displayName = "John Doe",
                phoneNumbers = listOf("+15551234567"),
                emailAddresses = listOf("john@example.com")
            )
        )
        
        // When
        val contactHashes = userDiscoveryService.hashContacts(contacts)
        
        // Then
        assertEquals(1, contactHashes.size)
        val contactHash = contactHashes.first()
        assertNotNull(contactHash.hashedPhone)
        assertNotNull(contactHash.hashedEmail)
        assertNotNull(contactHash.compositeHash)
        assertTrue(contactHash.isValidForDiscovery)
    }
    
    @Test
    fun `findUserByContact returns null when no user found`() = runTest {
        // Given
        val phoneNumber = "+15551234567"
        
        every { firestore.collection("users") } returns mockCollection
        every { mockCollection.whereEqualTo("hashedPhone", any()) } returns mockQuery
        every { mockQuery.limit(1) } returns mockQuery
        every { mockQuery.get() } returns mockk<Task<QuerySnapshot>>()
        
        every { mockQuerySnapshot.isEmpty } returns true
        coEvery { Tasks.await(any<Task<QuerySnapshot>>()) } returns mockQuerySnapshot
        
        // When
        val result = userDiscoveryService.findUserByContact(phoneNumber = phoneNumber)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `updateFriendRequestStatuses returns original list when no authenticated user`() = runTest {
        // Given
        val discoveredUsers = listOf(
            createMockDiscoveredUser("user1", "John Doe")
        )
        
        every { auth.currentUser } returns null
        
        // When
        val result = userDiscoveryService.updateFriendRequestStatuses(discoveredUsers)
        
        // Then
        assertEquals(discoveredUsers, result)
    }
    
    @Test
    fun `getDiscoveryStats returns initial empty stats`() = runTest {
        // When
        val stats = userDiscoveryService.getDiscoveryStats()
        
        // Then
        assertEquals(0, stats.totalDiscoveryAttempts)
        assertEquals(0, stats.totalContactsProcessed)
        assertEquals(0, stats.totalUsersDiscovered)
        assertEquals(0f, stats.averageMatchRate)
        assertFalse(stats.hasStats)
    }
    
    @Test
    fun `clearCache clears discovery cache and stats`() = runTest {
        // When
        userDiscoveryService.clearCache()
        
        // Then
        val stats = userDiscoveryService.getDiscoveryStats()
        assertEquals(0, stats.totalDiscoveryAttempts)
        assertEquals(0, stats.totalContactsProcessed)
        assertEquals(0, stats.totalUsersDiscovered)
    }
    
    private fun createMockDiscoveredUser(userId: String, displayName: String) = mockk<com.locationsharing.app.domain.model.DiscoveredUser> {
        every { this@mockk.userId } returns userId
        every { this@mockk.displayName } returns displayName
        every { profilePictureUrl } returns null
        every { matchedContact } returns mockk {
            every { id } returns "contact_$userId"
            every { this@mockk.displayName } returns displayName
            every { phoneNumbers } returns listOf("+15551234567")
            every { emailAddresses } returns emptyList()
        }
        every { friendRequestStatus } returns FriendRequestStatus.NONE
        every { matchType } returns ContactMatchType.PHONE
        every { lastActive } returns null
        every { isOnline } returns false
    }
}