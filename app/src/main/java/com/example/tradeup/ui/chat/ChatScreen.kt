import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    otherUserName: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    // FR-4.1.1: Secure chat interface
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUserName) },
                actions = {
                    // FR-4.1.3: Menu for block/report options
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report User") },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block User") },
                                onClick = {
                                    showMenu = false
                                    showBlockDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages will go here (LazyColumn)
            Text(
                "Chat messages will appear here",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // FR-4.1.3: Report dialog
    if (showReportDialog) {
        ReportUserDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                viewModel.reportUser(otherUserId, reason)
                showReportDialog = false
            }
        )
    }

    // FR-4.1.3: Block confirmation dialog
    if (showBlockDialog) {
        BlockUserDialog(
            userName = otherUserName,
            onDismiss = { showBlockDialog = false },
            onConfirm = {
                viewModel.blockUser(otherUserId)
                showBlockDialog = false
            }
        )
    }
}

// FR-4.1.3: Report dialog component
@Composable
fun ReportUserDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }

    val reportReasons = listOf(
        "Inappropriate content",
        "Spam messages",
        "Harassment",
        "Scam/Fraud",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report User") },
        text = {
            Column {
                Text("Why are you reporting this user?")
                Spacer(modifier = Modifier.height(16.dp))

                reportReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        onReport(selectedReason)
                    }
                },
                enabled = selectedReason.isNotEmpty()
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// FR-4.1.3: Block confirmation dialog
@Composable
fun BlockUserDialog(
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block User") },
        text = {
            Text("Are you sure you want to block $userName? You won't receive messages from them anymore.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}