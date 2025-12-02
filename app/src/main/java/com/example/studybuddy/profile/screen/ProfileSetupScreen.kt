package com.example.studybuddy.profile.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studybuddy.ProfileSetupState
import com.example.studybuddy.ProfileSetupViewModel
import com.example.studybuddy.Routes
import com.example.studybuddy.ui.StudyBuddyTopBar


// The profile setup page (will include everyinformation that will be used later on for filtering to find the studyBUddy match)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileSetupViewModel
) {
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val state by viewModel.state.collectAsState()
    val profileSaved by viewModel.profileSaved.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }


    //Reactively navigate once Firestore save completes
    LaunchedEffect(profileSaved) {
        if (profileSaved) {
            navController.navigate(Routes.Home.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            StudyBuddyTopBar(
                title = "Step $currentStep of 4",
                showBack = currentStep > 1,
                onBack = { currentStep -= 1 }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            when (currentStep) {
                1 -> Step1Content(
                    state = state,
                    setupVM = viewModel,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it }
                )

                2 -> Step2Content(
                    state = state,
                    setupVM = viewModel,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it },
                    onComplete = { viewModel.completeProfile() }
                )

                3 -> Step3Content(
                    state = state,
                    setupVM = viewModel,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it }
                )

                4 -> Step4Content(
                    state = state,
                    setupVM = viewModel,
                    onComplete = { viewModel.completeProfile() }
                )
            }
        }
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Setup?") },
            text = { Text("Your progress will be saved. Do you want to return to Login?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }) { Text("Exit", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

}

@Composable
fun Step1Content(
    state: ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit
) {
    // Validate required fields
    val isStep1Valid = state.name.isNotBlank() && state.major.isNotBlank() && state.year.isNotBlank()

    Column {
        Text("Basic Information", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = setupVM::updateName,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.major,
            onValueChange = setupVM::updateMajor,
            label = { Text("Major") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Use the reusable YearDropdown from EditProfileScreen for consistency
        YearDropdown(
            selectedYear = state.year,
            onYearSelected = { setupVM.updateYear(it) }
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (isStep1Valid) onNextStep(currentStep + 1) },
            enabled = isStep1Valid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Continue", color = Color.White)
        }
    }
}

@Composable
fun Step2Content(
    state: ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var course by rememberSaveable { mutableStateOf("") }

    // Validate that at least one course is added before continuing
    val isStep2Valid = state.courses.isNotEmpty()

    Column {
        Text("Your Courses", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("e.g., CS 501") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (course.isNotBlank()) {
                        setupVM.addCourse(course.trim())
                        course = ""
                    }
                })
            )
            IconButton(onClick = {
                // Add course on clicking plus button, clear input afterward
                if (course.isNotBlank()) {
                    setupVM.addCourse(course.trim())
                    course = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
            }
        }

        Spacer(Modifier.height(12.dp))
        if (state.courses.isEmpty()) {
            Text("No courses added yet.", color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.courses.size) { index ->
                    val courseName = state.courses[index]
                    // Allow removing a course by clicking its chip
                    AssistChip(
                        onClick = { setupVM.removeCourse(courseName) }, // Remove course on click
                        label = { Text(courseName) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                // Add current course if any before continuing
                if (course.isNotBlank()) {
                    setupVM.addCourse(course.trim())
                    course = ""
                }
                if (isStep2Valid) {
                    if (currentStep < 4) onNextStep(currentStep + 1)
                    else onComplete()
                }
            },
            enabled = isStep2Valid, // Disable button if no courses added
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = if (currentStep < 4) "Continue" else "Complete Profile", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3Content(
    state: ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val times = listOf("Morning", "Afternoon", "Evening")

    var selectedDay by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timeMenuExpanded by remember { mutableStateOf(false) }

    val isStepValid = state.availability.isNotEmpty()

    Column(Modifier.fillMaxWidth()) {

        Text("Availability", style = MaterialTheme.typography.titleLarge)
        Text(
            "Choose your free study times:",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

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

        // --- TIME DROPDOWN ---
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

        // --- ADD AVAILABILITY BUTTON ---
        Button(
            onClick = {
                if (selectedDay.isNotBlank() && selectedTime.isNotBlank()) {
                    val label = "$selectedDay $selectedTime"
                    setupVM.toggleAvailability(label)
                    selectedDay = ""
                    selectedTime = ""
                }
            },
            enabled = selectedDay.isNotBlank() && selectedTime.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("Add Time", color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        // --- SHOW SELECTED AVAILABILITY AS CHIPS ---
        if (state.availability.isNotEmpty()) {
            Text("Selected Times:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.availability.forEach { label ->
                    AssistChip(
                        onClick = { setupVM.toggleAvailability(label) },
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { if (isStepValid) onNextStep(currentStep + 1) },
            enabled = isStepValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun Step4Content(
    state: ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    onComplete: () -> Unit
) {
    val prefs = listOf(
        "Quiet Study", "Group Discussion", "Library",
        "Coffee Shops", "Online / Virtual", "Morning Person", "Night Owl"
    )

    // State to show confirmation dialog before submitting profile
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Text("Study Preferences", style = MaterialTheme.typography.titleLarge)
        Text("How do you like to study?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        prefs.forEach { pref ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.preferences.contains(pref),
                    onCheckedChange = { setupVM.togglePref(pref) }
                )
                Text(pref)
            }
        }

        Spacer(Modifier.height(12.dp))
        // Bio field is optional, so no validation needed
        OutlinedTextField(
            value = state.bio,
            onValueChange = setupVM::updateBio,
            label = { Text("Bio (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Create Profile", color = Color.White)
        }
    }

    // Confirmation Dialog before final profile submission
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Profile") },
            text = { Text("Review your information before submitting.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onComplete()
                }) {
                    Text("Yes, continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
