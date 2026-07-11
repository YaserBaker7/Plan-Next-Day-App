package com.bakersoftware.plannextday

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakersoftware.plannextday.data.TaskEntity
import com.bakersoftware.plannextday.data.ThemeChoice
import com.bakersoftware.plannextday.data.WeekEntity
import com.bakersoftware.plannextday.ui.theme.PlanNextDaySimpleTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val plannerViewModel: PlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openedFromReminder =
            intent?.getStringExtra(EXTRA_DESTINATION) == TOMORROW_DESTINATION
        setContent {
            val state by plannerViewModel.uiState.collectAsState()
            PlanNextDaySimpleTheme(state.settings.theme) {
                PlannerApp(state, openedFromReminder)
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val TOMORROW_DESTINATION = "tomorrow"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation model
// ─────────────────────────────────────────────────────────────────────────────

private enum class Destination(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Today),
    TOMORROW("Tomorrow", Icons.Outlined.CalendarMonth),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlannerApp(state: PlannerUiState, openedFromReminder: Boolean) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<PlannerViewModel>()
    var destination by rememberSaveable {
        mutableStateOf(if (openedFromReminder) Destination.TOMORROW else Destination.TODAY)
    }
    var confirmCopy by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshForToday() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            val contentModifier = Modifier
                .padding(padding)

            when (destination) {
                Destination.TODAY -> TodayScreen(
                    state = state,
                    modifier = contentModifier,
                    onCheckedChange = viewModel::setCompleted
                )

                Destination.TOMORROW -> TomorrowScreen(
                    state = state,
                    modifier = contentModifier,
                    onAdd = viewModel::addTomorrowTask,
                    onCopy = {
                        if (state.tomorrowTasks.isEmpty()) viewModel.copyTodayToTomorrow()
                        else confirmCopy = true
                    },
                    onDelete = { task ->
                        viewModel.deleteTomorrowTask(task)
                        scope.launch {
                            val result = snackbar.showSnackbar("Task removed", "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreTomorrowTask(task)
                            }
                        }
                    }
                )

                Destination.SETTINGS -> SettingsScreen(
                    state = state,
                    modifier = contentModifier
                )
            }
        }

        /* Snackbar sits above the dock so it doesn't peek behind it */
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )

        FloatingDock(
            selected = destination,
            onSelected = { destination = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    /* Copy-today confirmation dialog */
    if (confirmCopy) {
        AlertDialog(
            onDismissRequest = { confirmCopy = false },
            title = { Text("Replace tomorrow's plan?") },
            text = { Text("Tomorrow's current tasks will be replaced with today's task names.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.copyTodayToTomorrow()
                    confirmCopy = false
                }) { Text("Replace plan") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCopy = false }) { Text("Keep current plan") }
            }
        )
    }

    /* Weekly recap dialog */
    state.pendingRecap?.let { recap ->
        WeeklyRecap(recap, viewModel::acknowledgeRecap)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Today screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayScreen(
    state: PlannerUiState,
    modifier: Modifier,
    onCheckedChange: (TaskEntity, Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "heading") {
            Heading(
                "Today",
                state.today.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
                )
            )
        }

        state.week?.let { week ->
            item(key = "weekly") { WeeklyProgress(week) }
        }

        if (state.todayTasks.isEmpty()) {
            item(key = "empty") {
                EmptyToday(
                    firstUse = !state.settings.hasPlannedBefore,
                    tomorrowReady = state.tomorrowTasks.isNotEmpty()
                )
            }
        } else {
            items(state.todayTasks, key = { it.id }) { task ->
                TodayTaskItem(
                    task = task,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Today task item — satisfying check animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayTaskItem(
    task: TaskEntity,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    /* Animated card background */
    val cardColor by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "task card color"
    )
    /* Slight shrink when completed so unchecked tasks dominate visually */
    val cardScale by animateFloatAsState(
        targetValue = if (task.completed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "task scale"
    )
    /* Bouncy check icon */
    val checkScale by animateFloatAsState(
        targetValue = if (task.completed) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check bounce"
    )
    val checkColor by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "check color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox) {
                    onCheckedChange(task, !task.completed)
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onCheckedChange(task, !task.completed) }) {
                Icon(
                    imageVector = if (task.completed) Icons.Outlined.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.completed) "Completed" else "Not completed",
                    modifier = Modifier
                        .scale(checkScale)
                        .size(26.dp),
                    tint = checkColor
                )
            }
            Text(
                text = task.title,
                modifier = Modifier.padding(start = 4.dp),
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly progress card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyProgress(week: WeekEntity) {
    val limitReached = week.maxMisses > 0 && week.missedCount >= week.maxMisses
    val fill by animateFloatAsState(
        targetValue = if (week.maxMisses == 0) 0f
        else (week.missedCount.toFloat() / week.maxMisses).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "weekly fill"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limitReached) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp).animateContentSize()) {
            Text(
                text = if (limitReached) "A rough week is still a week you showed up for."
                else "Weekly promise",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (limitReached) "Keep going. You get a fresh start on Monday."
                else "${week.missedCount} of ${week.maxMisses} missed tasks used",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (limitReached) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty today card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyToday(firstUse: Boolean, tomorrowReady: Boolean) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.CheckCircle, null,
                Modifier.size(34.dp),
                MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (firstUse) "Welcome. Start with tomorrow."
                else "Today is intentionally clear.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (firstUse) "Your first small promise begins on the Tomorrow tab."
                else if (tomorrowReady) "Your next plan is ready. Let today stay simple."
                else "No plan was made yesterday. Use Tomorrow to make the next day easier."
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tomorrow screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TomorrowScreen(
    state: PlannerUiState,
    modifier: Modifier,
    onAdd: (String) -> Unit,
    onCopy: () -> Unit,
    onDelete: (TaskEntity) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "heading") {
            Heading(
                "Tomorrow",
                state.today.plusDays(1).format(
                    DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
                )
            )
        }

        /* Task composer card */
        item(key = "composer") {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .animateContentSize()
                ) {
                    Text("Make it easy to begin.", fontWeight = FontWeight.Bold)
                    Text(
                        "This list becomes tomorrow's fixed Today plan at midnight.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a task") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (draft.isNotBlank()) {
                                    onAdd(draft)
                                    draft = ""
                                }
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = draft.isNotBlank(),
                            onClick = {
                                onAdd(draft)
                                draft = ""
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Add, "Add task")
                        }
                    }
                }
            }
        }

        /* Copy today's tasks */
        if (state.todayTasks.isNotEmpty()) {
            item(key = "copy") {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use today's tasks for tomorrow")
                }
            }
        }

        /* Empty hint or task list */
        if (state.tomorrowTasks.isEmpty()) {
            item(key = "empty_hint") {
                Text(
                    "A tiny plan is enough. Add the first thing you want to finish.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
        } else {
            items(state.tomorrowTasks, key = { it.id }) { task ->
                TomorrowTaskItem(
                    task = task,
                    onDelete = onDelete,
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tomorrow task item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TomorrowTaskItem(
    task: TaskEntity,
    onDelete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(state: PlannerUiState, modifier: Modifier) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<PlannerViewModel>()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setReminderEnabled(granted) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "heading") {
            Heading("Settings", "Small settings. A calmer plan.")
        }

        /* Theme */
        item(key = "theme") {
            SettingsCard("Appearance") {
                ThemeChoice.entries.forEach { choice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setTheme(choice) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.settings.theme == choice,
                            onClick = { viewModel.setTheme(choice) }
                        )
                        Text(
                            text = when (choice) {
                                ThemeChoice.SYSTEM -> "Use device setting"
                                ThemeChoice.LIGHT -> "Light"
                                ThemeChoice.DARK -> "Dark"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        /* Reminder */
        item(key = "reminder") {
            SettingsCard("Tomorrow reminder") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("One planning reminder", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Only when tomorrow is still empty",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = state.settings.remindersEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setReminderEnabled(enabled)
                            }
                        }
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    viewModel.setReminderMinutes(hour * 60 + minute)
                                },
                                state.settings.reminderMinutes / 60,
                                state.settings.reminderMinutes % 60,
                                true
                            ).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder time", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Best effort to protect battery",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        formatMinutes(state.settings.reminderMinutes),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
        }

        /* Weekly miss limit */
        item(key = "weekly_limit") {
            SettingsCard("Weekly promise") {
                Text("Maximum missed tasks", fontWeight = FontWeight.SemiBold)
                Text(
                    "Changes apply next Monday, never in the middle of a week.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = state.settings.nextWeekMissLimit > 0,
                        onClick = {
                            viewModel.setNextWeekMissLimit(state.settings.nextWeekMissLimit - 1)
                        }
                    ) { Text("−", fontSize = 20.sp) }

                    Text(
                        text = state.settings.nextWeekMissLimit.toString(),
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            viewModel.setNextWeekMissLimit(state.settings.nextWeekMissLimit + 1)
                        }
                    ) { Text("+", fontSize = 20.sp) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating pill-shaped navigation dock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingDock(
    selected: Destination,
    onSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(Modifier.padding(6.dp)) {
            Destination.entries.forEach { item ->
                val active = selected == item
                val iconColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dock icon"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dock bg"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(bgColor)
                        .clickable { onSelected(item) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(item.icon, item.title, tint = iconColor)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.title,
                        color = iconColor,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly recap dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyRecap(week: WeekEntity, onStart: (WeekEntity) -> Unit) {
    val keptPromise = week.maxMisses == 0 || week.missedCount <= week.maxMisses
    AlertDialog(
        onDismissRequest = { /* not dismissable — must acknowledge */ },
        title = { Text("Your week, honestly") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (keptPromise) "You kept your promise. Nice work."
                    else "You kept going, even when the week got messy."
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                week.completedCount.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("done")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                week.missedCount.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text("missed")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(week) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start a new week") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Heading(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(Locale.getDefault(), minutes / 60, minutes % 60)
