package com.example.swand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.swand.ui.theme.SWandTheme

class MainActivity : ComponentActivity() {
    private lateinit var _viewModel: PatternViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _viewModel = ViewModelProvider(this)[PatternViewModel::class.java]

        setContent {
            SWandTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "connection_fragment"
                ) {
                    composable("connection_fragment") { _ ->
                        ConnectionFragment(navController = navController,
                        viewModel = _viewModel)
                    }
                    composable("pattern_fragment") { _ ->
                        PatternFragment(navController = navController,
                            viewModel = _viewModel)
                    }
                    composable("list_patterns_fragment") { _ ->
                        ListPatternsFragment(navController = navController,
                            viewModel = _viewModel)
                    }
                }
            }
        }
    }
}