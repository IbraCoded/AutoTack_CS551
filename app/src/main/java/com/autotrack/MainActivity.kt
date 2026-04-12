package com.autotrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.autotrack.navigation.AutoTrackNavGraph
import com.autotrack.ui.theme.AutoTrackTheme
import com.autotrack.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val prefs by vm.preferences.collectAsStateWithLifecycle()

            AutoTrackTheme(darkTheme = prefs.darkTheme) {
                val navController = rememberNavController()
                AutoTrackNavGraph(navController = navController)
            }
        }
    }
}