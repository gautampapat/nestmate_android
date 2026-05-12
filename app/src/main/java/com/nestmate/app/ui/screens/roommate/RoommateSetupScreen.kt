package com.nestmate.app.ui.screens.roommate

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.FoodPreference
import com.nestmate.app.data.model.Gender
import com.nestmate.app.data.model.HabitPreference
import com.nestmate.app.data.model.RoomType
import com.nestmate.app.data.model.SleepSchedule
import com.nestmate.app.data.model.StudyHabit
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.imageupload.rememberImagePicker
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateSetupScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: RoommateViewModel = hiltViewModel()
    val existing by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.PREFER_NOT_TO_SAY) }
    var college by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var minBudget by remember { mutableStateOf("") }
    var maxBudget by remember { mutableStateOf("") }
    var roomType by remember { mutableStateOf(RoomType.SHARED_ROOM) }
    var sleep by remember { mutableStateOf(SleepSchedule.NIGHT_OWL) }
    var cleanliness by remember { mutableStateOf(3) }
    var studyHabit by remember { mutableStateOf(StudyHabit.QUIET_STUDIER) }
    var food by remember { mutableStateOf(FoodPreference.VEG) }
    var smoke by remember { mutableStateOf(HabitPreference.NO) }
    var drink by remember { mutableStateOf(HabitPreference.NO) }
    var bio by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(true) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var currentPhotoUrl by remember { mutableStateOf<String?>(null) }

    val picker = rememberImagePicker(maxCount = 1, onPicked = { uris -> photoUri = uris.firstOrNull() })

    LaunchedEffect(existing) {
        val p = existing ?: return@LaunchedEffect
        if (name.isBlank()) name = p.name
        if (age.isBlank()) age = p.age.takeIf { it > 0 }?.toString().orEmpty()
        gender = p.gender
        if (college.isBlank()) college = p.collegeName
        if (course.isBlank()) course = p.course
        if (location.isBlank()) location = p.preferredLocation
        if (minBudget.isBlank()) minBudget = p.minBudget.takeIf { it > 0 }?.toString().orEmpty()
        if (maxBudget.isBlank()) maxBudget = p.maxBudget.takeIf { it > 0 }?.toString().orEmpty()
        roomType = p.roomTypePreference
        sleep = p.sleepingSchedule
        cleanliness = p.cleanlinessLevel.takeIf { it in 1..5 } ?: 3
        studyHabit = p.studyHabits
        food = p.foodPreference
        smoke = p.smokingHabit
        drink = p.drinkingHabit
        if (bio.isBlank()) bio = p.bio
        searching = p.isActivelySearching
        currentPhotoUrl = p.photoUrl
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text(if (existing == null) "Create roommate profile" else "Edit roommate profile") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val model: Any? = photoUri ?: currentPhotoUrl
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                com.nestmate.app.ui.components.NestMateOutlinedButton(
                    text = "Change photo",
                    onClick = { picker.launch() },
                )
            }

            SectionLabel("Personal info")
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                age, { age = it.filter { ch -> ch.isDigit() } },
                label = { Text("Age") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceChips(
                items = Gender.values().toList(),
                selected = gender,
                label = { it.label },
                onChange = { gender = it },
            )

            SectionLabel("Academic")
            OutlinedTextField(college, { college = it }, label = { Text("College name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(course, { course = it }, label = { Text("Course / branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SectionLabel("Location & budget")
            OutlinedTextField(location, { location = it }, label = { Text("Preferred location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    minBudget, { minBudget = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Min budget (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    maxBudget, { maxBudget = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Max budget (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Room preference")
            SingleChoiceChips(
                items = RoomType.values().toList(),
                selected = roomType,
                label = { it.label },
                onChange = { roomType = it },
            )

            SectionLabel("Sleep schedule")
            SingleChoiceChips(
                items = SleepSchedule.values().toList(),
                selected = sleep,
                label = { it.label },
                onChange = { sleep = it },
            )

            SectionLabel("Cleanliness (1–5)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { n ->
                    FilterChip(
                        selected = n == cleanliness,
                        onClick = { cleanliness = n },
                        label = { Text(n.toString()) },
                    )
                }
            }

            SectionLabel("Study habits")
            SingleChoiceChips(
                items = StudyHabit.values().toList(),
                selected = studyHabit,
                label = { it.label },
                onChange = { studyHabit = it },
            )

            SectionLabel("Food preference")
            SingleChoiceChips(
                items = FoodPreference.values().toList(),
                selected = food,
                label = { it.label },
                onChange = { food = it },
            )

            SectionLabel("Smoking")
            SingleChoiceChips(
                items = HabitPreference.values().toList(),
                selected = smoke,
                label = { it.label },
                onChange = { smoke = it },
            )

            SectionLabel("Drinking")
            SingleChoiceChips(
                items = HabitPreference.values().toList(),
                selected = drink,
                label = { it.label },
                onChange = { drink = it },
            )

            SectionLabel("About you")
            OutlinedTextField(bio, { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = searching, onCheckedChange = { searching = it })
                Spacer(Modifier.width(12.dp))
                Text("Actively searching for a roommate", color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NestMatePrimaryButton(
                    text = if (existing == null) "Create Profile" else "Save Changes",
                    onClick = submit@{
                        val ageNum = age.toIntOrNull() ?: 0
                        val minB = minBudget.toLongOrNull() ?: 0L
                        val maxB = maxBudget.toLongOrNull() ?: 0L
                        if (name.isBlank() || ageNum <= 0 || college.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Name, age and college are required") }
                            return@submit
                        }
                        if (minB <= 0L || maxB <= 0L || maxB < minB) {
                            scope.launch { snackbar.showSnackbar("Enter a valid budget range") }
                            return@submit
                        }
                        val profile = (existing ?: viewModel.profileTemplateFromUser()).copy(
                            name = name.trim(),
                            age = ageNum,
                            gender = gender,
                            collegeName = college.trim(),
                            course = course.trim(),
                            preferredLocation = location.trim(),
                            minBudget = minB,
                            maxBudget = maxB,
                            roomTypePreference = roomType,
                            sleepingSchedule = sleep,
                            cleanlinessLevel = cleanliness,
                            studyHabits = studyHabit,
                            foodPreference = food,
                            smokingHabit = smoke,
                            drinkingHabit = drink,
                            bio = bio.trim(),
                            isActivelySearching = searching,
                        )
                        viewModel.saveProfile(
                            context = context,
                            profile = profile,
                            newPhotoUri = photoUri,
                        ) { result -> result.onSuccess { onNavigateBack() } }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun <T> SingleChoiceChips(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onChange: (T) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onChange(item) },
                label = { Text(label(item)) },
            )
        }
    }
}
