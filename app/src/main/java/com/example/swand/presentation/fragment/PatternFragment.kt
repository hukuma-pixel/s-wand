package com.example.swand.presentation.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swand.R
import com.example.swand.presentation.viewmodel.PatternViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternFragment(
    viewModel: PatternViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.observeAsState(initial = false)
    val isButtonPressed by viewModel.isButtonPressed.observeAsState(initial = false)
    val status by viewModel.status.observeAsState(initial = "")
    val mode by viewModel.mode.observeAsState(initial = "recognize")
    val result by viewModel.result.observeAsState(initial = "")

    var patternNameInput by remember { mutableStateOf("") }
    var recognitionResult by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        if (!isConnected)
        {
            viewModel.connect(context)
        }
    }

    LaunchedEffect(result) {
        if (result.isNotEmpty()) {
            recognitionResult = result
        }
    }

    LaunchedEffect(patternNameInput) {
        if (mode == "record") {
            viewModel.setPatternName(patternNameInput)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("S Wand") },
                actions = {
                    IconButton(onClick = { navController.navigate("list_patterns_fragment") }) {
                        Text(stringResource(R.string.list_button_pattern_fragment))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isConnected) stringResource(R.string.connected_state_pattern_fragment)
                else stringResource(R.string.disconnected_state_pattern_fragment),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (recognitionResult.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            recognitionResult.startsWith("Recognized:") ->
                                MaterialTheme.colorScheme.primaryContainer
                            recognitionResult == stringResource(R.string.saved_state_pattern_fragment) ->
                                MaterialTheme.colorScheme.tertiaryContainer
                            else ->
                                MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Text(
                        text = recognitionResult,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.instuction_pattern_fragment),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
            )

            if (mode == "record") {
                OutlinedTextField(
                    value = patternNameInput,
                    onValueChange = {
                        patternNameInput = it
                        viewModel.setPatternName(it)
                    },
                    label = { Text(stringResource(R.string.pattern_name_lable_pattern_fragment)) },
                    placeholder = { Text(stringResource(R.string.enter_name_lable_pattern_fragment)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "record",
                    onClick = {
                        viewModel.setMode("record")
                        recognitionResult = ""
                    },
                    label = { Text(stringResource(R.string.write_button_mode)) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = mode == "recognize",
                    onClick = {
                        viewModel.setMode("recognize")
                        recognitionResult = ""
                        patternNameInput = ""
                    },
                    label = { Text(stringResource(R.string.read_button_mode)) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = mode == "idle",
                    onClick = {
                        viewModel.setMode("idle")
                        recognitionResult = ""
                        patternNameInput = ""
                    },
                    label = { Text(stringResource(R.string.wait_button_mode)) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }

            if (isButtonPressed) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.listening_motions_indicate),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}