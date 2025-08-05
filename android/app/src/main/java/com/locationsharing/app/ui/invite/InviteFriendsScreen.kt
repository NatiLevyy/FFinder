package com.locationsharing.app.ui.invite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.DiscoveredUser
import com.locationsharing.app.domain.model.FriendRequestStatus
import com.locationsharing.app.utils.PhoneNumberUtils
import timber.log.Timber

/**
 * Complete Invite Friends screen with contact import, user discovery, and sharing
 * Features two sections: Friends on FFinder (Add Friend) and Invite to FFinder (Share)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteFriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.importContactsAndDiscover()
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Invite Friends",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.discoveryComplete) {
                        IconButton(onClick = { viewModel.retryImport() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    
                    // Debug menu (only in debug builds)
                    if (BuildConfig.DEBUG) {
                        var showDebugMenu by remember { mutableStateOf(false) }
                        
                        IconButton(onClick = { showDebugMenu = true }) {
                            Text("🧪", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        if (showDebugMenu) {
                            DebugMenu(
                                viewModel = viewModel,
                                onDismiss = { showDebugMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            // Show FAB when there are selections
            AnimatedVisibility(
                visible = uiState.selectedDiscoveredUsersCount > 0 || uiState.selectedNonUserContactsCount > 0,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.selectedDiscoveredUsersCount > 0) {
                            viewModel.sendFriendRequests()
                        }
                        if (uiState.selectedNonUserContactsCount > 0) {
                            sendInvitations(context, uiState, viewModel)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    BadgedBox(
                        badge = {
                            val totalSelected = uiState.selectedDiscoveredUsersCount + uiState.selectedNonUserContactsCount
                            if (totalSelected > 0) {
                                Badge {
                                    Text(totalSelected.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.selectedDiscoveredUsersCount > 0) Icons.Default.PersonAdd else Icons.Default.Share,
                            contentDescription = "Send invitations"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !uiState.hasContactsPermission -> {
                    PermissionRequestContent(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.isProcessing -> {
                    LoadingContent(
                        progress = uiState.currentProgress,
                        isDiscovering = uiState.isDiscovering,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                !uiState.hasContacts -> {
                    EmptyContactsContent(
                        onRetry = { viewModel.retryImport() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    InviteFriendsContent(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onDiscoveredUserToggle = { viewModel.selectDiscoveredUser(it) },
                        onNonUserContactToggle = { viewModel.selectNonUserContact(it) },
                        onSendFriendRequest = { user ->
                            // Send individual friend request
                            Timber.d("🎯 UI: Sending friend request to ${user.displayName}")
                            viewModel.sendSingleFriendRequest(user.userId)
                        },
                        onInviteContact = { contact ->
                            Timber.d("📤 UI: Inviting contact ${contact.displayName}")
                            sendSingleInvitation(context, contact)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Access Your Contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll help you find friends who are already using FFinder and invite others to join.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Access to Contacts")
        }
    }
}

@Composable
private fun LoadingContent(
    progress: String?,
    isDiscovering: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = progress ?: if (isDiscovering) "Finding friends..." else "Loading contacts...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        
        if (isDiscovering) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Searching for friends who are already on FFinder",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyContactsContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Contacts Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We couldn't find any contacts on your device. Make sure you have contacts saved and try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
private fun InviteFriendsContent(
    uiState: InviteFriendsUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDiscoveredUserToggle: (DiscoveredUser) -> Unit,
    onNonUserContactToggle: (Contact) -> Unit,
    onSendFriendRequest: (DiscoveredUser) -> Unit,
    onInviteContact: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search contacts...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )
        
        // Filter contacts based on search
        val filteredDiscoveredUsers = uiState.discoveredUsers.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.matchedContact.phoneNumbers.any { phone -> phone.contains(searchQuery) }
        }
        
        val filteredNonUserContacts = uiState.nonUserContacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumbers.any { phone -> phone.contains(searchQuery) }
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Friends on FFinder
            if (filteredDiscoveredUsers.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Friends on FFinder",
                        count = filteredDiscoveredUsers.size,
                        icon = Icons.Default.PersonAdd,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                items(filteredDiscoveredUsers) { user ->
                    DiscoveredUserItem(
                        user = user,
                        isSelected = uiState.selectedDiscoveredUsers.contains(user.userId),
                        onToggle = { onDiscoveredUserToggle(user) },
                        onSendFriendRequest = { onSendFriendRequest(user) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Section 2: Invite to FFinder
            if (filteredNonUserContacts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Invite to FFinder",
                        count = filteredNonUserContacts.size,
                        icon = Icons.Default.Share,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                items(filteredNonUserContacts) { contact ->
                    NonUserContactItem(
                        contact = contact,
                        isSelected = uiState.selectedNonUserContacts.contains(contact.id),
                        onToggle = { onNonUserContactToggle(contact) },
                        onInvite = { onInviteContact(contact) }
                    )
                }
            }
            
            // Empty state for search
            if (searchQuery.isNotEmpty() && filteredDiscoveredUsers.isEmpty() && filteredNonUserContacts.isEmpty()) {
                item {
                    EmptySearchContent(searchQuery = searchQuery)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DiscoveredUserItem(
    user: DiscoveredUser,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSendFriendRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(150),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            Box {
                if (user.profilePictureUrl != null) {
                    AsyncImage(
                        model = user.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Online indicator
                if (user.isOnline) {
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = Color(0xFF4CAF50)
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                user.primaryContactMethod?.let { contact ->
                    Text(
                        text = if (contact.startsWith("+")) {
                            PhoneNumberUtils.formatForDisplay(contact)
                        } else {
                            contact
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Friend request status
                when (user.friendRequestStatus) {
                    FriendRequestStatus.SENT -> {
                        Text(
                            text = "Request sent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FriendRequestStatus.RECEIVED -> {
                        Text(
                            text = "Request received",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    FriendRequestStatus.ACCEPTED -> {
                        Text(
                            text = "Already friends",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    else -> {}
                }
            }
            
            // Action button
            if (user.canSendFriendRequest) {
                Button(
                    onClick = onSendFriendRequest,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            } else if (user.friendRequestStatus == FriendRequestStatus.ACCEPTED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NonUserContactItem(
    contact: Contact,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(150),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact avatar
            if (contact.photoUri != null) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                contact.primaryPhoneNumber?.let { phone ->
                    Text(
                        text = PhoneNumberUtils.formatForDisplay(phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "Not on FFinder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Invite button
            OutlinedButton(
                onClick = onInvite,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Invite")
            }
        }
    }
}

@Composable
private fun EmptySearchContent(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No contacts found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No contacts match \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
/**
 * Send invitations to multiple selected contacts
 */
private fun sendInvitations(
    context: Context,
    uiState: InviteFriendsUiState,
    viewModel: InviteFriendsViewModel
) {
    val selectedContacts = uiState.nonUserContacts.filter { 
        it.id in uiState.selectedNonUserContacts 
    }
    
    Timber.d("📤 Sending invitations to ${selectedContacts.size} selected contacts")
    
    if (selectedContacts.isNotEmpty()) {
        // Create share intent for multiple contacts
        val shareText = createInviteMessage(selectedContacts.size > 1)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Join me on FFinder!")
        }
        
        try {
            Timber.d("🚀 Opening share chooser for ${selectedContacts.size} contacts")
            context.startActivity(Intent.createChooser(shareIntent, "Invite friends via"))
            viewModel.sendInvitations() // Clear selections
            Timber.d("✅ Share intent launched successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to launch share intent")
            // Handle error - could show a snackbar
        }
    } else {
        Timber.w("⚠️ No contacts selected for invitation")
    }
}

/**
 * Send invitation to a single contact
 */
private fun sendSingleInvitation(context: Context, contact: Contact) {
    Timber.d("📱 Sending single invitation to ${contact.displayName}")
    val shareText = createInviteMessage(false)
    
    // Try SMS first if phone number is available
    contact.primaryPhoneNumber?.let { phoneNumber ->
        Timber.d("📲 Attempting SMS to $phoneNumber")
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", shareText)
        }
        
        try {
            context.startActivity(smsIntent)
            Timber.d("✅ SMS intent launched successfully")
            return
        } catch (e: Exception) {
            Timber.w(e, "⚠️ SMS failed, falling back to general share")
            // Fall back to general share
        }
    }
    
    // Fall back to general share intent
    Timber.d("📤 Using general share intent")
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Join me on FFinder!")
    }
    
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Invite ${contact.displayName} via"))
        Timber.d("✅ Share intent launched successfully")
    } catch (e: Exception) {
        Timber.e(e, "❌ Failed to launch share intent")
        // Handle error
    }
}

/**
 * Create the invitation message
 */
private fun createInviteMessage(isMultiple: Boolean): String {
    return if (isMultiple) {
        """
        Hey! I'm using FFinder to share my location with friends and family. 
        
        It's super easy to stay connected and see where everyone is. Want to join me?
        
        Download FFinder: https://play.google.com/store/apps/details?id=com.ffinder.app
        
        See you on FFinder! 📍
        """.trimIndent()
    } else {
        """
        Hey! I'm using FFinder to share my location with friends and family. 
        
        It's a great way to stay connected and see where everyone is. Want to join me?
        
        Download FFinder: https://play.google.com/store/apps/details?id=com.ffinder.app
        
        See you on FFinder! 📍
        """.trimIndent()
    }
}

/**
 * Debug menu for testing different states and scenarios
 * Only available in debug builds
 */
@Composable
private fun DebugMenu(
    viewModel: InviteFriendsViewModel,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🧪 Debug Menu") },
        text = {
            Column {
                Text("Test different friend request states and scenarios:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        viewModel.addTestDiscoveredUsers()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Test Users")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        // Simulate network error
                        viewModel.clearError()
                        // This would need to be implemented to simulate errors
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Network Error")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        viewModel.retryImport()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Force Retry Import")
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}