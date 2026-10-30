package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.preferences.EditorPreferencesRepository
import com.example.data.preferences.EditorSettings
import com.example.data.repository.DocumentRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.editor.EditorViewModel
import com.example.ui.editor.WorkspaceScreen
import com.example.ui.theme.CodePadTheme
import com.example.ui.theme.EditorPalettes
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodePadAppRoot()
        }
    }
}

@Composable
fun CodePadAppRoot(
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val prefsRepo = remember(context) { EditorPreferencesRepository(context) }
    val settings by prefsRepo.settingsFlow.collectAsStateWithLifecycle(
        initialValue = EditorSettings()
    )
    val palette = remember(settings.editorThemeId) {
        EditorPalettes.fromId(settings.editorThemeId)
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )

    CodePadTheme(darkTheme = palette.isDark) {
        val user = currentUser
        if (user == null) {
            // Strict navigation-level Auth Gate: feature ViewModels and Firestore listeners
            // are NEVER instantiated before Google Sign-In completes.
            AuthScreen(onAuthSuccess = {})
        } else {
            val uid = user.uid
            val editorViewModel: EditorViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        EditorViewModel(
                            repository = DocumentRepository(db),
                            preferencesRepository = EditorPreferencesRepository(app),
                            currentUserId = uid
                        )
                    }
                }
            )
            WorkspaceScreen(
                viewModel = editorViewModel,
                palette = palette,
                onSignedOut = {}
            )
        }
    }
}
