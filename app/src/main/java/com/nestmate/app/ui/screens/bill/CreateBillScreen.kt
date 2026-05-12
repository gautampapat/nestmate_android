package com.nestmate.app.ui.screens.bill

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.BillItem
import com.nestmate.app.data.model.Participant
import com.nestmate.app.data.model.PaymentStatus
import com.nestmate.app.data.model.RoommateProfile
import com.nestmate.app.data.model.SplitMethod
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.ConnectionPickerSheet
import com.nestmate.app.utils.CurrencyFormatter
import com.nestmate.app.utils.bill.BillCalculator
import com.nestmate.app.utils.bill.ParticipantShare
import com.nestmate.app.utils.bill.SplitResult
import kotlinx.coroutines.launch
import java.util.UUID
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBillScreen(
    onNavigateBack: () -> Unit,
    prefilledUserId: String? = null
) {
    val viewModel: BillSplitterViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val connectedRoommates by viewModel.connectedRoommates.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val participantNames = remember { mutableStateListOf<String>() }
    val participantIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(connectedRoommates, prefilledUserId) {
        if (prefilledUserId != null && connectedRoommates.isNotEmpty()) {
            if (!participantIds.contains(prefilledUserId)) {
                val profile = connectedRoommates.find { it.userId == prefilledUserId }
                if (profile != null) {
                    val displayName = profile.name.ifBlank { "Roommate" }
                    participantNames.add(displayName)
                    participantIds.add(profile.userId)
                }
            }
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalRs by remember { mutableStateOf("") }
    var taxRs by remember { mutableStateOf("") }
    var serviceRs by remember { mutableStateOf("") }
    var tipRs by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var splitMethod by remember { mutableStateOf(SplitMethod.EQUAL) }

    var newParticipant by remember { mutableStateOf("") }

    // Bottom sheet state for roommate picker
    var showRoommateSheet by remember { mutableStateOf(false) }
    val roommateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val customSharesInput = remember { mutableStateMapOf<String, String>() }
    val items = remember { mutableStateListOf<BillItem>() }

    val previewBill by remember {
        derivedStateOf {
            BillGroup(
                totalAmount = (totalRs.toLongOrNull() ?: 0L) * 100L,
                taxAmount = (taxRs.toLongOrNull() ?: 0L) * 100L,
                serviceChargeAmount = (serviceRs.toLongOrNull() ?: 0L) * 100L,
                tipAmount = (tipRs.toLongOrNull() ?: 0L) * 100L,
                splitMethod = splitMethod,
                participants = participantNames.mapIndexed { i, n ->
                    Participant(id = participantIds.getOrElse(i) { UUID.randomUUID().toString() }, name = n)
                },
                items = items.toList(),
            )
        }
    }
    val customPaiseMap = remember(customSharesInput.toMap(), participantIds.toList()) {
        participantIds.associateWith { id ->
            (customSharesInput[id]?.toLongOrNull() ?: 0L) * 100L
        }
    }

    val previewResult = remember(previewBill, customPaiseMap) {
        BillCalculator.computeShares(previewBill, customPaiseMap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text("New bill") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Bill info")
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth().height(80.dp))
            OutlinedTextField(
                totalRs, { totalRs = it.filter { ch -> ch.isDigit() } },
                label = { Text("Total (Rs)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Section("Extras")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    taxRs, { taxRs = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tax (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    serviceRs, { serviceRs = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Service (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    tipRs, { tipRs = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tip (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f),
                )
            }

            Section("Participants (min 2)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newParticipant, { newParticipant = it },
                    label = { Text("Name") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                NestMatePrimaryButton(
                    text = "Add",
                    onClick = {
                        if (newParticipant.isNotBlank()) {
                            participantNames.add(newParticipant.trim())
                            participantIds.add(UUID.randomUUID().toString())
                            newParticipant = ""
                        }
                    },
                    modifier = Modifier.weight(0.3f),
                )
            }

            // Add from Roommates button — visible only if there are connected roommates
            if (connectedRoommates.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        showRoommateSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.People, contentDescription = "Icon", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add from Roommates")
                }
            }

            participantNames.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                    IconButton(onClick = {
                        participantNames.removeAt(index)
                        if (index < participantIds.size) participantIds.removeAt(index)
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }

            Section("Split method")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SplitMethod.values().toList()) { m ->
                    FilterChip(
                        selected = m == splitMethod,
                        onClick = { splitMethod = m },
                        label = { Text(m.name) },
                    )
                }
            }

            when (splitMethod) {
                SplitMethod.EQUAL -> {
                    // Nothing extra; preview shows shares.
                }
                SplitMethod.CUSTOM -> {
                    Section("Custom shares (Rs per person)")
                    participantNames.forEachIndexed { i, name ->
                        val id = participantIds.getOrElse(i) { "" }
                        OutlinedTextField(
                            value = customSharesInput[id].orEmpty(),
                            onValueChange = { customSharesInput[id] = it.filter { ch -> ch.isDigit() } },
                            label = { Text(name) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                SplitMethod.ITEM_WISE -> {
                    ItemWiseEditor(
                        participants = participantNames.mapIndexed { i, n ->
                            Participant(id = participantIds.getOrElse(i) { UUID.randomUUID().toString() }, name = n)
                        },
                        items = items,
                    )
                }
            }

            Section("Preview")
            when (val r = previewResult) {
                is SplitResult.Invalid -> Text(
                    text = r.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                is SplitResult.Success -> SharesTable(r.shares)
            }

            Spacer(Modifier.height(8.dp))
            NestMatePrimaryButton(
                text = "Create Bill",
                enabled = participantNames.size >= 2 && title.isNotBlank() &&
                    (totalRs.toLongOrNull() ?: 0L) > 0L &&
                    previewResult is SplitResult.Success,
                onClick = submit@{
                    val builtParticipants = participantNames.mapIndexed { i, name ->
                        Participant(id = participantIds.getOrElse(i) { UUID.randomUUID().toString() }, name = name)
                    }
                    val input = BillSplitterViewModel.BillInput(
                        title = title.trim(),
                        description = description.trim().takeIf { it.isNotBlank() },
                        totalAmount = (totalRs.toLongOrNull() ?: 0L) * 100L,
                        taxAmount = (taxRs.toLongOrNull() ?: 0L) * 100L,
                        serviceChargeAmount = (serviceRs.toLongOrNull() ?: 0L) * 100L,
                        tipAmount = (tipRs.toLongOrNull() ?: 0L) * 100L,
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        splitMethod = splitMethod,
                        participants = builtParticipants,
                        items = items.toList(),
                    )
                    val customMap = when (splitMethod) {
                        SplitMethod.CUSTOM -> participantIds.associateWith {
                            (customSharesInput[it]?.toLongOrNull() ?: 0L) * 100L
                        }
                        else -> emptyMap()
                    }
                    viewModel.createBill(input, customMap) { result ->
                        result.onSuccess { onNavigateBack() }
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Roommate Picker Bottom Sheet ──────────────────────────────────────────
    if (showRoommateSheet) {
        ConnectionPickerSheet(
            sheetState = roommateSheetState,
            connectedRoommates = connectedRoommates,
            onDismiss = { showRoommateSheet = false },
            onAddSelected = { selectedProfiles ->
                val existingNames = participantNames.toSet()
                selectedProfiles
                    .filter { profile ->
                        val displayName = profile.name.ifBlank { "Roommate" }
                        displayName !in existingNames
                    }
                    .forEach { profile ->
                        val displayName = profile.name.ifBlank { "Roommate" }
                        participantNames.add(displayName)
                        participantIds.add(profile.userId)
                    }
            }
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SharesTable(shares: List<ParticipantShare>) {
    NestMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            shares.forEach { s ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        s.name,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        CurrencyFormatter.formatPaise(s.share),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemWiseEditor(
    participants: List<Participant>,
    items: androidx.compose.runtime.snapshots.SnapshotStateList<BillItem>,
) {
    var itemName by remember { mutableStateOf("") }
    var itemPriceRs by remember { mutableStateOf("") }
    val selectedAssignees = remember { mutableStateListOf<String>() }

    Section("Items")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            itemName, { itemName = it },
            label = { Text("Item name") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            itemPriceRs, { itemPriceRs = it.filter { ch -> ch.isDigit() } },
            label = { Text("Price (Rs)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Text("Assign to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(participants) { p ->
                FilterChip(
                    selected = p.id in selectedAssignees,
                    onClick = {
                        if (p.id in selectedAssignees) selectedAssignees.remove(p.id)
                        else selectedAssignees.add(p.id)
                    },
                    label = { Text(p.name) },
                )
            }
        }
        NestMatePrimaryButton(
            text = "Add Item",
            enabled = itemName.isNotBlank() &&
                (itemPriceRs.toLongOrNull() ?: 0L) > 0L &&
                selectedAssignees.isNotEmpty(),
            onClick = {
                items.add(
                    BillItem(
                        id = UUID.randomUUID().toString(),
                        name = itemName.trim(),
                        price = (itemPriceRs.toLongOrNull() ?: 0L) * 100L,
                        assignedParticipantIds = selectedAssignees.toList(),
                    ),
                )
                itemName = ""
                itemPriceRs = ""
                selectedAssignees.clear()
            },
        )
        items.forEachIndexed { index, it ->
            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(it.name, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${CurrencyFormatter.formatPaise(it.price)}  •  ${it.assignedParticipantIds.size} ppl",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { items.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
