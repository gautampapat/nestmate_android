package com.nestmate.app.ui.screens.safety

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SafetyViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    val policeBlue = Color(0xFF1976D2)
    val medicalGreen = Color(0xFF388E3C)

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Emergency Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Phone, "Back")
                    }
                },
                actions = {
                    if (contacts.size < 5) {
                        IconButton(onClick = { showAddForm = !showAddForm }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).imePadding()) {

            // Quick Dials Section
            Text("National Helplines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickDialButton(
                    label = "Police",
                    number = "100",
                    icon = Icons.Default.LocalPolice,
                    color = policeBlue,
                    modifier = Modifier.weight(1f),
                    context = context
                )
                QuickDialButton(
                    label = "Ambulance",
                    number = "108",
                    icon = Icons.Default.LocalHospital,
                    color = medicalGreen,
                    modifier = Modifier.weight(1f),
                    context = context
                )
                QuickDialButton(
                    label = "Women (1091)",
                    number = "1091",
                    icon = Icons.Default.Phone,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f),
                    context = context
                )
            }

            // Add Contact Form (appears/hides when '+' tapped)
            if (showAddForm && contacts.size < 5) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showAddForm = false; newName = ""; newPhone = "" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newPhone.isNotBlank()) {
                                        viewModel.addContact(newName, newPhone)
                                        newName = ""
                                        newPhone = ""
                                        showAddForm = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = newName.isNotBlank() && newPhone.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            // Saved Contacts List
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Contacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${contacts.size}/5", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No contacts saved. Tap + to add your parents or trusted friends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar initials
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Call button
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call ${contact.name}", tint = Color(0xFF2E7D32))
                                }
                                // Delete button
                                IconButton(onClick = { viewModel.removeContact(contact) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove ${contact.name}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickDialButton(
    label: String,
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    context: android.content.Context
) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = 90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold, 
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = number, 
                style = MaterialTheme.typography.labelSmall, 
                color = color.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
