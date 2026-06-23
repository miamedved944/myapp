

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: String
)

class NoteRepository {
    private val notes = mutableStateListOf(
        Note(1, "OOTD Planner", "Plan your outfits.", currentDate()),
        Note(2, "Nail Ideas", "Save nail designs.", currentDate()),
        Note(3, "Journal", "Write your thoughts.", currentDate())
    )

    fun getNotes(): List<Note> = notes

    fun getNoteById(id: Int): Note? {
        return notes.find { it.id == id }
    }

    fun addNote(title: String, content: String) {
        val newId = (notes.maxOfOrNull { it.id } ?: 0) + 1
        notes.add(Note(newId, title, content, currentDate()))
    }

    fun updateNote(id: Int, title: String, content: String) {
        val index = notes.indexOfFirst { it.id == id }

        if (index != -1) {
            val oldNote = notes[index]
            notes[index] = oldNote.copy(
                title = title,
                content = content
            )
        }
    }

    companion object {
        fun currentDate(): String {
            val formatter = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
            return formatter.format(Date())
        }
    }
}

val noteRepository: NoteRepository by lazy {
    NoteRepository()
}

class ListViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    val notes: List<Note>
        get() = repository.getNotes()
}

class EditViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    fun getNote(id: Int): Note? {
        return repository.getNoteById(id)
    }

    fun saveNote(id: Int, title: String, content: String) {
        if (id == -1) {
            repository.addNote(title, content)
        } else {
            repository.updateNote(id, title, content)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()

    val listViewModel = remember {
        ListViewModel(noteRepository)
    }

    val editViewModel = remember {
        EditViewModel(noteRepository)
    }

    NavHost(
        navController = navController,
        startDestination = "list"
    ) {
        composable("list") {
            ListScreen(
                viewModel = listViewModel,
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

            val id = backStackEntry.arguments?.getInt("id") ?: -1

            EditorScreen(
                id = id,
                viewModel = editViewModel,
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onAdd: () -> Unit,
    onClick: (Note) -> Unit
) {
    val notes = viewModel.notes

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
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = note.content)

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
    id: Int,
    viewModel: EditViewModel,
    onDone: () -> Unit
) {
    val existingNote = viewModel.getNote(id)

    var title by remember {
        mutableStateOf(existingNote?.title ?: "")
    }

    var content by remember {
        mutableStateOf(existingNote?.content ?: "")
    }

    val createdAt = existingNote?.createdAt ?: NoteRepository.currentDate()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (id == -1) "New Note" else "Edit Note",
            style = MaterialTheme.typography.titleLarge
        )

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

        Text(
            text = "Created: $createdAt",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    viewModel.saveNote(id, title, content)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}