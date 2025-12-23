package com.example.swand.presentation.fragment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swand.R
import com.example.swand.presentation.viewmodel.PatternViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListPatternsFragment(
    viewModel: PatternViewModel,
    navController: NavController
) {
    val patterns by viewModel.patterns.observeAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    var patternToDelete by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_list_patterns_fragment)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Text(stringResource(R.string.back_list_fragment))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(patterns) { pattern ->
                val patternName = pattern.substringBefore(": ")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                patternToDelete = patternName
                                showDeleteDialog = true
                            }
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = patternName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = pattern.substringAfter(": "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.deleting_list_fragment)) },
            text = { Text(
                stringResource(
                    R.string.do_you_want_to_delete_list_fragment,
                    patternToDelete
                )
            ) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePattern(patternToDelete)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete_button_list_fragment))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(stringResource(R.string.cancel_button_list_fragment))
                }
            }
        )
    }
}