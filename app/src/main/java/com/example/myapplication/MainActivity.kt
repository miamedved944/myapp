package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: String
)

class NoteRepository {
    private val notesList = mutableListOf(
        Note(1, "OOTD Planner", "Plan your outfits.", "26.04.2026."),
        Note(2, "Nail Ideas", "Save nail designs.", "26.04.2026."),
        Note(3, "Journal", "Write your thoughts.", "26.04.2026.")
    )

    private val _notes = MutableStateFlow(notesList.toList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun getAllNotes(): List<Note> = _notes.value

    fun getNoteById(id: Int): Note? = _notes.value.find { it.id == id }

    fun addNote(title: String, content: String) {
        val newId = (notesList.maxOfOrNull { it.id } ?: 0) + 1
        val newNote = Note(
            id = newId,
            title = title,
            content = content,
            createdAt = "26.04.2026."
        )
        notesList.add(newNote)
        _notes.value = notesList.toList()
    }

    fun updateNote(id: Int, newTitle: String, newContent: String) {
        val index = notesList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldNote = notesList[index]
            notesList[index] = oldNote.copy(
                title = newTitle,
                content = newContent
            )
            _notes.value = notesList.toList()
        }
    }
}

class ListViewModel(private val repository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.notes
}

class EditViewModel(private val repository: NoteRepository) : ViewModel() {
    fun getNote(id: Int): Note? = repository.getNoteById(id)

    fun saveNote(id: Int, title: String, content: String) {
        if (id == -1) {
            repository.addNote(title, content)
        } else {
            repository.updateNote(id, title, content)
        }
    }
}

class ListViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ListViewModel(repository) as T
    }
}

class EditViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(repository) as T
    }
}

class MainActivity : ComponentActivity() {

    private val noteRepository by lazy {
        NoteRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(noteRepository)
        }
    }
}

@Composable
fun App(repository: NoteRepository) {
    val navController = rememberNavController()

    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModelFactory(repository)
    )

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            val notes by listViewModel.notes.collectAsState()

            ListScreen(
                notes = notes,
                onAdd = {
                    navController.navigate("editor/-1")
                },
                onClick = { note ->
                    navController.navigate("editor/${note.id}")
                }
            )
        }

        composable(
            route = "editor/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("id") ?: -1

            val editViewModel: EditViewModel = viewModel(
                backStackEntry,
                factory = EditViewModelFactory(repository)
            )

            EditorScreen(
                note = if (noteId == -1) null else editViewModel.getNote(noteId),
                onSave = { title, content ->
                    editViewModel.saveNote(noteId, title, content)
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ListScreen(
    notes: List<Note>,
    onAdd: () -> Unit,
    onClick: (Note) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(note) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(note.content)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created: ${note.createdAt}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorScreen(
    note: Note?,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = note?.createdAt ?: "",
            onValueChange = {},
            label = { Text("Created at") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    onSave(title, content)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}
