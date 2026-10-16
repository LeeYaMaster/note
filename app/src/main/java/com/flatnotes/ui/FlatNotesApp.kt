package com.flatnotes.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flatnotes.ui.editor.EditorRoute
import com.flatnotes.ui.notes.NotesRoute

private object Routes {
    const val Notes = "notes"
    const val Editor = "editor/{noteId}"

    fun editor(id: String) = "editor/$id"
}

@Composable
fun FlatNotesApp() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val viewModel: NotesViewModel = viewModel(viewModelStoreOwner = activity)

    NavHost(
        navController = navController,
        startDestination = Routes.Notes,
    ) {
        composable(Routes.Notes) {
            NotesRoute(
                viewModel = viewModel,
                onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                onCreateNote = {
                    val id = viewModel.create()
                    navController.navigate(Routes.editor(id))
                },
            )
        }
        composable(
            route = Routes.Editor,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
        ) { entry ->
            EditorRoute(
                viewModel = viewModel,
                noteId = entry.arguments?.getString("noteId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
