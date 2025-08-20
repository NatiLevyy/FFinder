package com.locationsharing.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.locationsharing.app.ui.theme.FFinderTheme
import androidx.compose.material3.DropdownMenuItem

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onPrivacyChange = viewModel::setPrivacy,
        onBackgroundSharingChange = viewModel::setBackgroundSharing,
        onMapStyleChange = viewModel::setMapStyle,
        onUnitsChange = viewModel::setUnits,
        onA11yLargeLabelsChange = viewModel::setA11yLargeLabels,
        onEnableContactDiscovery = viewModel::enableContactDiscovery
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onPrivacyChange: (String) -> Unit,
    onBackgroundSharingChange: (Boolean) -> Unit,
    onMapStyleChange: (String) -> Unit,
    onUnitsChange: (String) -> Unit,
    onA11yLargeLabelsChange: (Boolean) -> Unit,
    onEnableContactDiscovery: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Section
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = uiState.privacyVisibility == "everyone",
                    onClick = { onPrivacyChange("everyone") },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Everyone")
                }
                SegmentedButton(
                    selected = uiState.privacyVisibility == "contacts",
                    onClick = { onPrivacyChange("contacts") },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Contacts")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Map & App Section
            Text("Map & App", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Background Sharing")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.backgroundSharingEnabled,
                    onCheckedChange = onBackgroundSharingChange
                )
            }
            var mapStyleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = mapStyleExpanded,
                onExpandedChange = { mapStyleExpanded = !mapStyleExpanded }
            ) {
                TextField(
                    value = uiState.mapStyle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Map Style") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mapStyleExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = mapStyleExpanded,
                    onDismissRequest = { mapStyleExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System") },
                        onClick = {
                            onMapStyleChange("system")
                            mapStyleExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            onMapStyleChange("light")
                            mapStyleExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = {
                            onMapStyleChange("dark")
                            mapStyleExpanded = false
                        }
                    )
                }
            }
            var unitsExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitsExpanded,
                onExpandedChange = { unitsExpanded = !unitsExpanded }
            ) {
                TextField(
                    value = uiState.units,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Units") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitsExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = unitsExpanded,
                    onDismissRequest = { unitsExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Kilometers") },
                        onClick = {
                            onUnitsChange("km")
                            unitsExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Miles") },
                        onClick = {
                            onUnitsChange("miles")
                            unitsExpanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Accessibility Section
            Text("Accessibility", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Large Labels")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.a11yLargeLabels,
                    onCheckedChange = onA11yLargeLabelsChange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Account Section
            Text("Account", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.isPhoneNumberLinked) "Linked (+${uiState.phoneNumber})" else "Anonymous",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onEnableContactDiscovery) {
                        Text(if (uiState.isPhoneNumberLinked) "Manage" else "Enable Contact Discovery")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FFinderTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onPrivacyChange = {},
            onBackgroundSharingChange = {},
            onMapStyleChange = {},
            onUnitsChange = {},
            onA11yLargeLabelsChange = {},
            onEnableContactDiscovery = {}
        )
    }
}