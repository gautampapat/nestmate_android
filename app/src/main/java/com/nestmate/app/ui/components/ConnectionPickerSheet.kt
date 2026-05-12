package com.nestmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nestmate.app.data.model.RoommateProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionPickerSheet(
    sheetState: SheetState,
    connectedRoommates: List<RoommateProfile>,
    onDismiss: () -> Unit,
    onAddSelected: (List<RoommateProfile>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedRoommateIds = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Add Roommates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Select connected roommates to add as bill participants.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            
            if (connectedRoommates.isEmpty()) {
                Text(
                    "You have no connected roommates yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                connectedRoommates.forEach { profile ->
                    val isSelected = profile.userId in selectedRoommateIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedRoommateIds.add(profile.userId)
                                else selectedRoommateIds.remove(profile.userId)
                            }
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = profile.name.ifBlank { "Roommate (${profile.userId.take(6)})" },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NestMatePrimaryButton(
                text = "Add Selected (${selectedRoommateIds.size})",
                enabled = selectedRoommateIds.isNotEmpty(),
                onClick = {
                    val selectedProfiles = connectedRoommates.filter { it.userId in selectedRoommateIds }
                    onAddSelected(selectedProfiles)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                },
            )
            TextButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancel") }
        }
    }
}
