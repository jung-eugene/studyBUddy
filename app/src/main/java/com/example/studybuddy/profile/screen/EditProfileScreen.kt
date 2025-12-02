package com.example.studybuddy.profile.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studybuddy.Routes
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.studybuddy.ui.StudyBuddyTopBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    userVM: UserViewModel
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val uiState by userVM.uiState.collectAsState()
    val user = uiState.user

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val BU_RED = Color(0xFFD32F2F)

    LaunchedEffect(uid) {
        userVM.loadUserProfile(uid)
    }

    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var courses by remember { mutableStateOf(listOf<String>()) }
    var availability by remember { mutableStateOf(listOf<String>()) }
    var studyPreferences by remember { mutableStateOf(listOf<String>()) }

    var initialized by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null && !initialized) {
            name = user.name
            major = user.major
            year = user.year
            bio = user.bio
            courses = user.courses
            availability = user.availability.split(", ").filter { it.isNotBlank() }
            studyPreferences = user.studyPreferences
            initialized = true
        }
    }

    val hasUnsavedChanges by remember(name, major, year, bio, courses, availability, studyPreferences) {
        derivedStateOf {
            user?.let { old ->
                name != old.name ||
                        major != old.major ||
                        year != old.year ||
                        bio != old.bio ||
                        courses != old.courses ||
                        availability != old.availability.split(", ") ||
                        studyPreferences != old.studyPreferences
            } ?: false
        }
    }

    BackHandler {
        if (hasUnsavedChanges) showCancelDialog = true
        else navController.navigate(Routes.Profile.route)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudyBuddyTopBar(
                title = "Edit Profile",
                showBack = true,
                onBack = {
                    if (hasUnsavedChanges) showCancelDialog = true
                    else navController.navigate(Routes.Profile.route)
                },
                containerColor = BU_RED
            )
        }
    ) { pad ->

        if (uiState.isLoading || user == null) {
            Box(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BU_RED)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Major") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            YearDropdown(selectedYear = year, onYearSelected = { year = it })

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(250) },
                label = { Text("Bio (max 250 chars)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                supportingText = { Text("${bio.length}/250") }
            )

            Text("Courses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            CourseEditor(
                courses = courses,
                onAddCourse = { courses = courses + it },
                onRemoveCourse = { courses = courses - it },
                accentColor = BU_RED
            )

            Text("Availability", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            AvailabilityEditor(
                selected = availability,
                onToggle = { label ->
                    availability = if (label in availability) availability - label else availability + label
                },
                accentColor = BU_RED
            )

            Text("Study Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            PreferencesEditor(
                selected = studyPreferences,
                onToggle = { pref ->
                    studyPreferences =
                        if (pref in studyPreferences) studyPreferences - pref else studyPreferences + pref
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        if (hasUnsavedChanges) showCancelDialog = true
                        else navController.navigate(Routes.Profile.route)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BU_RED)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val original = user

                        val updated = original.copy(
                            name = name,
                            major = major,
                            year = year,
                            bio = bio,
                            courses = courses,
                            availability = availability.joinToString(", "),
                            studyPreferences = studyPreferences,
                            profileSetupComplete = true
                        )

                        scope.launch {
                            isSaving = true
                            try {
                                userVM.saveUserProfile(uid, updated)
                                snackbarHostState.showSnackbar("Profile updated!")

                                navController.navigate(Routes.Profile.route) {
                                    popUpTo(Routes.Profile.route) { inclusive = false }
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BU_RED)
                ) {
                    if (isSaving)
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    else
                        Text("Save", color = Color.White)
                }
            }
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Discard changes?") },
                text = { Text("You have unsaved edits. Do you want to discard them?") },
                confirmButton = {
                    TextButton(onClick = {
                        showCancelDialog = false
                        navController.navigate(Routes.Profile.route) {
                            popUpTo(Routes.Profile.route) { inclusive = false }
                        }
                    }) { Text("Discard", color = BU_RED) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Editing")
                    }
                }
            )
        }
    }
}

@Composable
fun CourseEditor(
    courses: List<String>,
    onAddCourse: (String) -> Unit,
    onRemoveCourse: (String) -> Unit,
    accentColor: Color
) {
    var newCourse by remember { mutableStateOf("") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newCourse,
            onValueChange = { newCourse = it },
            label = { Text("e.g., CS 501") },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                if (newCourse.isNotBlank()) {
                    onAddCourse(newCourse.trim())
                    newCourse = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Add", color = Color.White)
        }
    }

    Spacer(Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(courses) { course ->
            AssistChip(
                onClick = { onRemoveCourse(course) },
                label = { Text(course) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = accentColor.copy(alpha = 0.15f),
                    labelColor = accentColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityEditor(
    selected: List<String>,
    onToggle: (String) -> Unit,
    accentColor: Color
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val times = listOf("Morning", "Afternoon", "Evening")

    var selectedDay by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timeMenuExpanded by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(
            expanded = dayMenuExpanded,
            onExpandedChange = { dayMenuExpanded = !dayMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedDay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Day") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = dayMenuExpanded,
                onDismissRequest = { dayMenuExpanded = false }
            ) {
                days.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day) },
                        onClick = {
                            selectedDay = day
                            dayMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = timeMenuExpanded,
            onExpandedChange = { timeMenuExpanded = !timeMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedTime,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Time") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = timeMenuExpanded,
                onDismissRequest = { timeMenuExpanded = false }
            ) {
                times.forEach { time ->
                    DropdownMenuItem(
                        text = { Text(time) },
                        onClick = {
                            selectedTime = time
                            timeMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val label = "$selectedDay $selectedTime"
                onToggle(label)
                selectedDay = ""
                selectedTime = ""
            },
            enabled = selectedDay.isNotBlank() && selectedTime.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Add Time", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        if (selected.isNotEmpty()) {
            Text("Selected Times:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selected.forEach { label ->
                    AssistChip(
                        onClick = { onToggle(label) },
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accentColor.copy(alpha = 0.15f),
                            labelColor = accentColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PreferencesEditor(
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    val allPrefs = listOf(
        "Quiet Study",
        "Group Discussion",
        "Library",
        "Coffee Shops",
        "Online / Virtual",
        "Morning Person",
        "Night Owl"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        allPrefs.forEach { pref ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = pref in selected,
                    onCheckedChange = { onToggle(pref) }
                )
                Text(pref)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearDropdown(
    selectedYear: String,
    onYearSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Freshman", "Sophomore", "Junior", "Senior", "Graduate", "Other")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedYear,
            onValueChange = {},
            label = { Text("Year") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}
