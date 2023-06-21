import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.ui.tooling.preview.PreviewParameter
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "task_preferences")

data class Task(val id: Long, val text: String, var completed: Boolean)

@Composable
fun TodoApp() {
    val dataStore = LocalContext.current.dataStore
    val tasks = remember { mutableStateListOf<Task>() }
    val searchText = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val completedTasks = tasks.filter { it.completed }

    LaunchedEffect(Unit) {
        val storedTasks = loadTasksFromDataStore(dataStore)
        tasks.addAll(storedTasks)
    }

    Column(Modifier.padding(16.dp)) {
        TextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            label = { Text(stringResource(R.string.add_task_hint)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )

        Button(
            onClick = {
                val newTaskText = searchText.value.trim()
                if (newTaskText.isNotEmpty()) {
                    val newTaskId = System.currentTimeMillis()
                    tasks.add(Task(newTaskId, newTaskText, false))
                    searchText.value = ""
                    focusRequester.requestFocus()
                    saveTasksToDataStore(dataStore, tasks)
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.add_task_button))
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(tasks) { task ->
                TaskItem(task) {
                    task.completed = !task.completed
                    saveTasksToDataStore(dataStore, tasks)
                }
            }
        }

        if (completedTasks.isNotEmpty()) {
            Button(
                onClick = {
                    tasks.removeAll(completedTasks)
                    saveTasksToDataStore(dataStore, tasks)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.delete_completed_button))
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onTaskClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onTaskClick) {
            Icon(
                imageVector = if (task.completed) Icons.Default.Check else Icons.Default.Check,
                contentDescription = stringResource(R.string.task_complete)
            )
        }

        Text(
            text = task.text,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

suspend fun loadTasksFromDataStore(dataStore: DataStore<Preferences>): List<Task> {
    val taskList = mutableListOf<Task>()
    dataStore.data.collect { preferences ->
        val taskKeys = preferences.keys.filter { it.name.startsWith("task_") }
        for (taskKey in taskKeys) {
            val taskId = taskKey.name.substringAfter("_").toLong()
            val taskText = preferences[taskKey] ?: ""
            val taskCompleted = preferences[completedPreferencesKey(taskId)] ?: false
            taskList.add(Task(taskId, taskText, taskCompleted))
        }
    }
    return taskList
}

suspend fun saveTasksToDataStore(dataStore: DataStore<Preferences>, tasks: List<Task>) {
    dataStore.edit { preferences ->
        val existingTaskKeys = preferences.keys.filter { it.name.startsWith("task_") }
        for (taskKey in existingTaskKeys) {
            preferences.remove(taskKey)
            preferences.remove(completedPreferencesKey(taskKey.name.substringAfter("_").toLong()))
        }

        for (task in tasks) {
            preferences[taskPreferencesKey(task.id)] = task.text
            preferences[completedPreferencesKey(task.id)] = task.completed
        }
    }
}

fun taskPreferencesKey(taskId: Long) = preferencesKey<String>("task_$taskId")
fun completedPreferencesKey(taskId: Long) = booleanPreferencesKey("completed_$taskId")

@Preview(showBackground = true)
@Composable
fun DefaultPreview(@PreviewParameter(BooleanPreviewProvider::class) isDarkTheme: Boolean) {
    TodoApp()
}
