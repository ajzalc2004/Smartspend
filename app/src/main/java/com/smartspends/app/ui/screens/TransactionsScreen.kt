package com.smartspends.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.ui.theme.Emerald500
import com.smartspends.app.ui.theme.Rose500
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToAddTransaction: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedBank by viewModel.selectedBank.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val uniqueBanks by viewModel.uniqueBanks.collectAsState()

    var showFiltersSection by remember { mutableStateOf(false) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    val categories = remember {
        listOf(
            "Food", "Fuel", "Shopping", "Travel", "Bills", "Entertainment",
            "Healthcare", "Education", "Rent", "Salary", "Interest",
            "Freelance", "Gift", "Refund", "Investment", "Other"
        )
    }

    LaunchedEffect(Unit) {
        viewModel.updateCurrencySymbol()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFiltersSection = !showFiltersSection }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = if (selectedType != null || selectedCategory != null || selectedBank != null || startDate != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by description or bank...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Dynamic Filters Drawer
            AnimatedVisibility(visible = showFiltersSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type Filters (All, Income, Expense)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedType == null,
                                onClick = { viewModel.setFilterType(null) },
                                label = { Text("All Types") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedType == "INCOME",
                                onClick = { viewModel.setFilterType("INCOME") },
                                label = { Text("Income") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedType == "EXPENSE",
                                onClick = { viewModel.setFilterType("EXPENSE") },
                                label = { Text("Expense") }
                            )
                        }
                    }

                    // Sorting selector
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortOptions = listOf(
                            "DATE_DESC" to "Newest First",
                            "DATE_ASC" to "Oldest First",
                            "AMOUNT_DESC" to "Highest Value",
                            "AMOUNT_ASC" to "Lowest Value"
                        )
                        items(sortOptions) { (optionCode, optionLabel) ->
                            FilterChip(
                                selected = sortBy == optionCode,
                                onClick = { viewModel.setSortBy(optionCode) },
                                label = { Text(optionLabel) },
                                leadingIcon = {
                                    if (sortBy == optionCode) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Category Filters Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setFilterCategory(null) },
                                label = { Text("All Categories") }
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setFilterCategory(cat) },
                                label = { Text(cat) }
                            )
                        }
                    }

                    // Bank Filters Row (if banks exist)
                    if (uniqueBanks.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedBank == null,
                                    onClick = { viewModel.setFilterBank(null) },
                                    label = { Text("All Banks") }
                                )
                            }
                            items(uniqueBanks) { bank ->
                                FilterChip(
                                    selected = selectedBank == bank,
                                    onClick = { viewModel.setFilterBank(bank) },
                                    label = { Text(bank) }
                                )
                            }
                        }
                    }

                    // Date Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)
                                        viewModel.setDateRange(dateStr, endDate)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = startDate ?: "From Date",
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)
                                        viewModel.setDateRange(startDate, dateStr)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = endDate ?: "To Date",
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }

                        if (startDate != null || endDate != null) {
                            IconButton(onClick = { viewModel.setDateRange(null, null) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Dates")
                            }
                        }
                    }
                }
            }

            // Transactions list
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions match your search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionRow(
                            transaction = tx,
                            currencySymbol = currencySymbol,
                            onClick = { selectedTransactionForDetails = tx }
                        )
                    }
                }
            }
        }

        // Details Bottom Sheet
        if (selectedTransactionForDetails != null) {
            val tx = selectedTransactionForDetails!!
            ModalBottomSheet(
                onDismissRequest = { selectedTransactionForDetails = null },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isExpense = tx.type == "EXPENSE"
                    val statusColor = if (isExpense) Rose500 else Emerald500

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpense) "Expense Details" else "Income Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = {
                                viewModel.deleteTransaction(tx)
                                selectedTransactionForDetails = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Large Amount display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tx.description,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Detail items list
                    DetailItem(label = "Category", value = tx.category)
                    DetailItem(label = "Date & Time", value = "${tx.date} at ${tx.time}")
                    DetailItem(label = "Transaction Mode", value = tx.transactionMode)
                    DetailItem(label = "Record Source", value = if (tx.source == "SMS") "Auto SMS Tracking" else "Manual Entry")
                    
                    if (!tx.bank.isNullOrEmpty()) {
                        DetailItem(label = "Bank Name", value = tx.bank)
                    }
                    if (!tx.accountNumberMasked.isNullOrEmpty()) {
                        DetailItem(label = "Account Number", value = tx.accountNumberMasked)
                    }

                    // SMS Debug block
                    if (tx.source == "SMS" && !tx.smsBody.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "RAW SMS PayLoad (Debugging)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tx.smsBody,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = { selectedTransactionForDetails = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Details")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
