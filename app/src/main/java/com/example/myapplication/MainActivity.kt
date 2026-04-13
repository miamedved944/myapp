// FINAL VERSION - Notes App (for assignment)
// Clean, simple, matches requirements (2 screens + add/edit)

package com.example.myapplication

import android.R.attr.defaultValue
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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

// DATA CLASS
data class Note(
    val id: Int,
    val title: String,
    val description: String
)

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

    // STATE (list of notes)
    var notes by remember {
        mutableStateOf(
            mutableListOf(
                Note(1, "OOTD Planner", "Plan your outfits."),
                Note(2, "Nail Ideas", "Save nail designs."),
                Note(3, "Journal", "Write your thoughts.")
            )
        )
    }

    NavHost(navController, startDestination = "list") {

        // SCREEN 1
        composable("list") {
            ListScreen(
                notes = notes,
                onAdd = { navController.navigate("editor") },
                onClick = { note ->
                    navController.navigate("editor/${note.id}")
                }
            )
        }

        // SCREEN 2 (EDIT / ADD)
        composable(
            "editor/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStack ->

            val id = backStack.arguments?.getInt("id") ?: -1
            val existing = notes.find { it.id == id }

            EditorScreen(
                note = existing,
                onSave = { title, desc ->
                    if (existing == null) {
                        val newId = (notes.maxOfOrNull { it.id } ?: 0) + 1
                        notes = (notes + Note(newId, title, desc)).toMutableList()
                    } else {
                        notes = notes.map {
                            if (it.id == id) Note(id, title, desc) else it
                        }.toMutableList()
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("editor") {
            EditorScreen(
                note = null,
                onSave = { title, desc ->
                    val newId = (notes.maxOfOrNull { it.id } ?: 0) + 1
                    notes = (notes + Note(newId, title, desc)).toMutableList()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// SCREEN 1
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
                        Text(note.description)
                    }
                }
            }
        }
    }
}

// SCREEN 2
@Composable
fun EditorScreen(
    note: Note?,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }

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
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    onSave(title, description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}
