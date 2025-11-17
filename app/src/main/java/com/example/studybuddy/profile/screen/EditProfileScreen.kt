package com.example.studybuddy.profile.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.studybuddy.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen( navController: NavHostController, userVM: UserViewModel = viewModel()) {
    // Get current user ID from Firebase
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val userProfile by userVM.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    val BU_RED = Color(0xFFD32F2F)

    val snackbarHostState = remember { SnackbarHostState() }
    var isSaving by remember { mutableStateOf(false) }

    // Load user profile on entering screen
    LaunchedEffect(uid) {
        userVM.loadUserProfile(uid)
    }

    // Local form states
    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var courses by remember { mutableStateOf(listOf<String>()) }
    var availability by remember { mutableStateOf(listOf<String>()) }
    var studyPreferences by remember { mutableStateOf(listOf<String>()) }

    // Track if profile has already been loaded
    var initialized by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Fill in the fields once when userProfile loads
    LaunchedEffect(userProfile) {
        if (userProfile != null && !initialized) {
            val u = userProfile!!
            name = u.name
            major = u.major
            year = u.year
            bio = u.bio
            courses = u.courses
            availability = u.availability.split(", ").filter { it.isNotBlank() }
            studyPreferences = u.studyPreferences
            initialized = true
        }
    }

    // Detect unsaved changes
    val hasUnsavedChanges by remember(name, major, year, bio, courses, availability, studyPreferences) {
        derivedStateOf {
            userProfile?.let { old ->
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

    // Handle back button: prompt confirmation if user has unsaved changes
    BackHandler {
        if (hasUnsavedChanges) showCancelDialog = true
        else navController.navigate("profile")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },

        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showCancelDialog = true
                        else navController.navigate("profile")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BU_RED,
                    titleContentColor = Color.White,
                )
            )
        }
    ) { pad ->

        // Show loading spinner if profile data is missing
        if (userProfile == null) {
            Box(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BU_RED)
            }
        }

        // Main editable form
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Major field
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Major") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Year field
            YearDropdown(selectedYear = year, onYearSelected = { year = it })

            // Bio field
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(250) },
                label = { Text("Bio (max 250 chars)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                supportingText = { Text("${bio.length}/250") }
            )

            // Courses field
            Text("Courses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            CourseEditor(
                courses = courses,
                onAddCourse = { courses = courses + it },
                onRemoveCourse = { courses = courses - it }
            )

            // Availability field
            Text("Availability", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            AvailabilityEditor(
                selected = availability,
                onToggle = { day ->
                    availability = if (day in availability) availability - day else availability + day
                }
            )

            // Study preference field
            Text("Study Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            PreferencesEditor(
                selected = studyPreferences,
                onToggle = { pref ->
                    studyPreferences = if (pref in studyPreferences) studyPreferences - pref else studyPreferences + pref
                }
            )

            // Action buttons (Cancel and Save)
            Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                // Cancel button with confirmation logic
                OutlinedButton(
                    onClick = {
                        if (hasUnsavedChanges) showCancelDialog = true
                        else navController.navigate("profile")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BU_RED)
                ) {
                    Text("Cancel")
                }

                // Save button updates Firebase profile
                Button(
                    onClick = {
                        if (userProfile == null) {
                            scope.launch { snackbarHostState.showSnackbar("Still loading profile…") }
                            return@Button
                        }

                        // Create an updated version of the profile
                        val updated = userProfile!!.copy(
                            name = name.ifBlank { userProfile!!.name },
                            major = major.ifBlank { userProfile!!.major },
                            year = year.ifBlank { userProfile!!.year },
                            bio = bio.ifBlank { userProfile!!.bio },
                            courses = courses.ifEmpty { userProfile!!.courses },
                            availability =
                                if (availability.isNotEmpty())
                                    availability.joinToString(", ")
                                else
                                    userProfile!!.availability,
                            studyPreferences =
                                studyPreferences.ifEmpty { userProfile!!.studyPreferences },
                            profileSetupComplete = true
                        )

                        // Save changes asynchronously and navigate back to profile screen
                        scope.launch {
                            try {
                                isSaving = true
                                userVM.saveUserProfile(uid, updated)
                                snackbarHostState.showSnackbar("Profile updated!")

                                navController.navigate("profile") {
                                    popUpTo("profile") { inclusive = false }
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

        // Confirmation dialog for discarding changes
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Discard changes?") },
                text = { Text("You have unsaved edits. Do you want to discard them?") },
                confirmButton = {
                    TextButton(onClick = {
                        showCancelDialog = false
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = false }
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
    onRemoveCourse: (String) -> Unit
) {
    var newCourse by remember { mutableStateOf("") }
    val BU_RED = Color(0xFFD32F2F)


    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newCourse,
            onValueChange = { newCourse = it },
            label = { Text("e.g., CS501") },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                if (newCourse.isNotBlank()) {
                    onAddCourse(newCourse.trim())
                    newCourse = ""
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BU_RED,
                contentColor = Color.White
            )
        ) {
            Text("Add")
        }

    }

    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(courses) { course ->
            AssistChip(
                onClick = { onRemoveCourse(course) },
                label = { Text(course) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityEditor(
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val times = listOf("Morning", "Afternoon", "Evening")

    var selectedDay by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timeMenuExpanded by remember { mutableStateOf(false) }

    Column {

        // --- DAY DROPDOWN ---
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
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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

        // --- TIME DROPDOWN ---
        ExposedDropdownMenuBox(
            expanded = timeMenuExpanded,
            onExpandedChange = { timeMenuExpanded = !timeMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedDay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Day") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)  // Correct anchor type for read-only fields
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

        // --- ADD AVAILABILITY ---
        Button(
            onClick = {
                if (selectedDay.isNotBlank() && selectedTime.isNotBlank()) {
                    val label = "$selectedDay $selectedTime"
                    onToggle(label)
                    selectedDay = ""
                    selectedTime = ""
                }
            },
            enabled = selectedDay.isNotBlank() && selectedTime.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Add Time", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        // --- SELECTED AVAILABILITY CHIPS ---
        if (selected.isNotEmpty()) {
            Text("Selected Times:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selected.forEach { label ->
                    AssistChip(
                        onClick = { onToggle(label) }, // remove when tapped
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            labelColor = MaterialTheme.colorScheme.primary
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
        "Quiet Study", "Group Discussion", "Library",
        "Coffee Shops", "Online / Virtual", "Morning Person", "Night Owl"
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
    //internal state for open/closed menu
    var expanded by remember { mutableStateOf(false) }

    val yearOptions = listOf(
        "Freshman",
        "Sophomore",
        "Junior",
        "Senior",
        "Graduate",
        "Other"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedYear,
            onValueChange = {},
            label = { Text("Year") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            yearOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onYearSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
