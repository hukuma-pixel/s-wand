package com.example.swand.presentation.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swand.presentation.viewmodel.PatternViewModel

@Composable
fun ConnectionFragment(
    navController: NavController,
    viewModel: PatternViewModel
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.observeAsState(initial = false)
    val status by viewModel.status.observeAsState(initial = "Initial S-Pen...")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "S Wand",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                viewModel.connect(context)
            },
            enabled = !isConnected,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        ) {
            Text(text = if (isConnected) "Connected" else "Connect S Pen")
        }

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (status?.contains("Error", ignoreCase = true) == true)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            navController.navigate("pattern_fragment") {
                popUpTo("connection_fragment") { inclusive = true }
            }
        }
    }
}