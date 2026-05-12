package com.nestmate.app.ui.screens.marketplace

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.ui.components.NestMatePrimaryButton
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWantedPostScreen(onNavigateBack: () -> Unit) {
    val viewModel: MarketplaceViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var description by remember { mutableStateOf("") }
    var maxBudgetRupees by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ItemCategory?>(null) }

    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text("Post Wanted") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Describe what you're looking for",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What do you need?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            OutlinedTextField(
                value = maxBudgetRupees,
                onValueChange = { maxBudgetRupees = it.filter { ch -> ch.isDigit() } },
                label = { Text("Max budget (Rs)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
            )
            Text(
                text = "Category (optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = category == null,
                        onClick = { category = null },
                        label = { Text("Any") },
                    )
                }
                items(ItemCategory.values().toList()) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = if (category == cat) null else cat },
                        label = { Text(cat.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NestMatePrimaryButton(
                text = "Post Request",
                onClick = submit@{
                    val budgetRs = maxBudgetRupees.toLongOrNull() ?: 0L
                    if (description.isBlank() || budgetRs <= 0L) {
                        scope.launch { snackbar.showSnackbar("Fill description and budget") }
                        return@submit
                    }
                    viewModel.createWantedPost(
                        itemDescription = description.trim(),
                        maxBudgetPaise = budgetRs * 100L,
                        category = category,
                    ) { result ->
                        result.onSuccess { onNavigateBack() }
                    }
                },
            )
        }
    }
}
