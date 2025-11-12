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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.studybuddy.ProfileSetupViewModel
import com.example.studybuddy.Routes


// The profile setup page (will include everyinformation that will be used later on for filtering to find the studyBUddy match)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    setupVM: ProfileSetupViewModel = viewModel()
) {
    var currentStep by rememberSaveable { mutableStateOf(1) }
    val state by setupVM.state.collectAsState()
    val profileSaved by setupVM.profileSaved.collectAsState()

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
            TopAppBar(
                title = { Text("Step $currentStep of 4", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
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
                    setupVM = setupVM,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it }
                )

                2 -> Step2Content(
                    state = state,
                    setupVM = setupVM,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it },
                    onComplete = { setupVM.completeProfile() }
                )

                3 -> Step3Content(
                    state = state,
                    setupVM = setupVM,
                    currentStep = currentStep,
                    onNextStep = { currentStep = it }
                )

                4 -> Step4Content(
                    state = state,
                    setupVM = setupVM,
                    onComplete = { setupVM.completeProfile() }
                )
            }
        }
    }
}

@Composable
fun Step1Content(
    state: com.example.studybuddy.ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit
) {
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
        OutlinedTextField(
            value = state.year,
            onValueChange = setupVM::updateYear,
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onNextStep(currentStep + 1) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Continue", color = Color.White)
        }
    }
}

@Composable
fun Step2Content(
    state: com.example.studybuddy.ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var course by rememberSaveable { mutableStateOf("") }

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
                    AssistChip(
                        onClick = {},
                        label = { Text(state.courses[index]) },
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
                if (course.isNotBlank()) {
                    setupVM.addCourse(course.trim())
                    course = ""
                }
                if (currentStep < 4) onNextStep(currentStep + 1)
                else onComplete()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (currentStep < 4) "Continue" else "Complete Profile",
                color = Color.White
            )
        }
    }
}

@Composable
fun Step3Content(
    state: com.example.studybuddy.ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    currentStep: Int,
    onNextStep: (Int) -> Unit
) {
    val days = listOf(
        "Monday Morning", "Monday Afternoon", "Tuesday Evening",
        "Wednesday Morning", "Thursday Evening", "Friday Afternoon", "Weekend"
    )

    Column {
        Text("Availability", style = MaterialTheme.typography.titleLarge)
        Text("When are you free to study?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        days.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.availability.contains(day),
                    onCheckedChange = { setupVM.toggleAvailability(day) }
                )
                Text(day)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onNextStep(currentStep + 1) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Continue", color = Color.White)
        }
    }
}

@Composable
fun Step4Content(
    state: com.example.studybuddy.ProfileSetupState,
    setupVM: ProfileSetupViewModel,
    onComplete: () -> Unit
) {
    val prefs = listOf(
        "Quiet Study", "Group Discussion", "Library",
        "Coffee Shops", "Online / Virtual", "Morning Person", "Night Owl"
    )

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
        OutlinedTextField(
            value = state.bio,
            onValueChange = setupVM::updateBio,
            label = { Text("Bio (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Complete Profile", color = Color.White)
        }
    }
}
