package com.example.stepcounter.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text


@Composable
fun WearFitnessApp(heartRateSensorValue: Int, hasHeartRateSensor: Boolean,
                   stepsGoalFromPhone: Int) {
    val navController = rememberNavController()
    val context = LocalContext.current

    var steps by remember { mutableIntStateOf(30) }
    var calories by remember { mutableIntStateOf(25) }
    var stepsGoal by remember { mutableIntStateOf(10000) }
    var caloriesGoal by remember { mutableIntStateOf(500) }

    var displayedStepsGoal by remember { mutableIntStateOf(stepsGoalFromPhone) }
    LaunchedEffect(stepsGoalFromPhone) {
        displayedStepsGoal = stepsGoalFromPhone
    }


    var manualHeartRate by remember { mutableIntStateOf(72) }
    val displayedHeartRate = if (hasHeartRateSensor) heartRateSensorValue else manualHeartRate


    var heartRateNotificationSent by remember { mutableStateOf(false) }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> notificationPermissionGranted = isGranted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    LaunchedEffect(displayedHeartRate, notificationPermissionGranted) {
        if (displayedHeartRate >= 100 && !heartRateNotificationSent && notificationPermissionGranted) {
            showNotification(
                context = context,
                notificationId = HEART_RATE_NOTIFICATION_ID,
                title = "High Heart Rate Detected",
                message = "Your heart rate reached $displayedHeartRate BPM."
            )
            heartRateNotificationSent = true
        }
        if (displayedHeartRate < 100) {
            heartRateNotificationSent = false
        }
    }


    SwipeNavigationContainer(navController = navController) {
        NavHost(navController = navController, startDestination = "progress") {
            composable("progress") {
                DailyProgressScreen(
                    steps = steps, calories = calories, stepsGoal = displayedStepsGoal, caloriesGoal = caloriesGoal,
                    onAddStep = { steps++; calories++ }
                )
            }
            composable("heart") {

                HeartRateScreen(
                    heartRate = displayedHeartRate,
                    hasHeartRateSensor = hasHeartRateSensor,
                    onDecreaseHeartRate = { manualHeartRate-- },
                    onIncreaseHeartRate = { manualHeartRate++ }
                )

            }
            composable("goals") {
                ModifyGoalScreen(
                    stepsGoal = displayedStepsGoal, caloriesGoal = caloriesGoal,
                    onDecreaseStepsGoal = { displayedStepsGoal -= 500 }, onIncreaseStepsGoal = { displayedStepsGoal += 500 },
                    onDecreaseCaloriesGoal = { caloriesGoal -= 50 }, onIncreaseCaloriesGoal = { caloriesGoal += 50 }
                )
            }
        }
    }
}

@Composable
fun SwipeNavigationContainer(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val routes = listOf("progress", "heart", "goals")
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "progress"
    val currentIndex = routes.indexOf(currentRoute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentRoute) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume(); totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -60 && currentIndex < routes.lastIndex) {
                            navController.navigate(routes[currentIndex + 1]) { launchSingleTop = true }
                        }
                        if (totalDrag > 60 && currentIndex > 0) {
                            navController.navigate(routes[currentIndex - 1]) { launchSingleTop = true }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun DailyProgressScreen(
    steps: Int, calories: Int, stepsGoal: Int, caloriesGoal: Int, onAddStep: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Daily Progress", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Steps", color = Color.White)
        Text(text = "$steps / $stepsGoal", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Calories", color = Color.White)
        Text(text = "$calories / $caloriesGoal", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onAddStep) { Text("Add") }
    }
}

@Composable
fun HeartRateScreen(
    heartRate: Int,
    hasHeartRateSensor: Boolean,
    onDecreaseHeartRate: () -> Unit,
    onIncreaseHeartRate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Heart Rate", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "$heartRate BPM", color = Color.White, style = MaterialTheme.typography.displaySmall)

        if (!hasHeartRateSensor) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Button(onClick = onDecreaseHeartRate) { Text("-") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onIncreaseHeartRate) { Text("+") }
            }
        }

    }
}

@Composable
fun ModifyGoalScreen(
    stepsGoal: Int, caloriesGoal: Int,
    onDecreaseStepsGoal: () -> Unit, onIncreaseStepsGoal: () -> Unit,
    onDecreaseCaloriesGoal: () -> Unit, onIncreaseCaloriesGoal: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Modify Goal", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Steps", color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Button(onClick = onDecreaseStepsGoal) { Text("-") }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stepsGoal.toString(), color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Button(onClick = onIncreaseStepsGoal) { Text("+") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Calories", color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Button(onClick = onDecreaseCaloriesGoal) { Text("-") }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = caloriesGoal.toString(), color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Button(onClick = onIncreaseCaloriesGoal) { Text("+") }
        }
    }
}