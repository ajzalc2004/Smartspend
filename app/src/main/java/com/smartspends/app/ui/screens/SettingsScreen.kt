package com.smartspends.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val smsTrackingEnabled by viewModel.smsTrackingEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Launcher for SMS Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
            val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
            if (readGranted && receiveGranted) {
                viewModel.toggleSmsTracking(true)
                Toast.makeText(context, "SMS permission granted! Auto tracking enabled.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.toggleSmsTracking(false)
                Toast.makeText(context, "SMS permission denied. Auto tracking disabled.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Launcher for Notification Permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.toggleNotifications(granted)
        }
    )

    // SAF File creation/selection launchers
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) viewModel.exportTransactionsToCsv(context, uri)
        }
    )

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) viewModel.exportTransactionsToPdf(context, uri)
        }
    )

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            if (uri != null) viewModel.backupDatabase(context, uri)
        }
    )

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.restoreDatabase(context, uri)
        }
    )

    // Toast status updates
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Privacy First",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SmartSpends runs entirely offline. Your transaction logs, budgets, and SMS data remain securely locked on this device. No ads, trackers, or cloud storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category 1: Automatic SMS Tracking
            SettingsSectionHeader(title = "Auto-Tracking Options")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsToggleRow(
                        title = "SMS Auto-Reading",
                        description = "Parse transactional alerts from your banks",
                        icon = Icons.Default.Sms,
                        checked = smsTrackingEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                                val receivePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                                if (readPermission == PackageManager.PERMISSION_GRANTED && receivePermission == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.toggleSmsTracking(true)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                    )
                                }
                            } else {
                                viewModel.toggleSmsTracking(false)
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsToggleRow(
                        title = "Local Notifications",
                        description = "Notify when SMS transactions are logged",
                        icon = Icons.Default.Notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                if (permission == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.toggleNotifications(true)
                                } else {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.toggleNotifications(checked)
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Inbox historical Scan
                    SettingsClickableRow(
                        title = "Scan Historical SMS Inbox",
                        description = "Scan past bank alerts to import transactions",
                        icon = Icons.Default.ManageSearch,
                        onClick = {
                            val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                            if (readPermission == PackageManager.PERMISSION_GRANTED) {
                                viewModel.importPastSms(context)
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                )
                            }
                        }
                    )
                }
            }

            // Category 2: Regional Defaults
            SettingsSectionHeader(title = "App Customizations")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsClickableRow(
                    title = "Default Currency",
                    description = "Current currency symbol: $currencyCode",
                    icon = Icons.Default.CurrencyExchange,
                    onClick = { showCurrencyDialog = true }
                )
            }

            // Category 3: Export & Report Generation
            SettingsSectionHeader(title = "Export Data")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsClickableRow(
                        title = "Export CSV Report",
                        description = "Save transactions history to comma-separated file",
                        icon = Icons.Default.Description,
                        onClick = { createCsvLauncher.launch("smartspends_report_${System.currentTimeMillis()}.csv") }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsClickableRow(
                        title = "Export PDF Summary",
                        description = "Generate print-ready statement pages",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = { createPdfLauncher.launch("smartspends_statement_${System.currentTimeMillis()}.pdf") }
                    )
                }
            }

            // Category 4: Backup & Restore
            SettingsSectionHeader(title = "Storage & Database")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsClickableRow(
                        title = "Backup Database",
                        description = "Backup your local SQLite data file to memory",
                        icon = Icons.Default.Backup,
                        onClick = { createBackupLauncher.launch("smartspends_backup.db") }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsClickableRow(
                        title = "Restore Database",
                        description = "Restore transactions from an existing backup file",
                        icon = Icons.Default.Restore,
                        onClick = { openRestoreLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Currency Choice Dialog
        if (showCurrencyDialog) {
            val currencies = listOf("INR" to "Indian Rupee (₹)", "USD" to "US Dollar ($)", "EUR" to "Euro (€)", "GBP" to "British Pound (£)")
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Default Currency") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencies.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.changeCurrency(code)
                                        showCurrencyDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currencyCode == code,
                                    onClick = {
                                        viewModel.changeCurrency(code)
                                        showCurrencyDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
