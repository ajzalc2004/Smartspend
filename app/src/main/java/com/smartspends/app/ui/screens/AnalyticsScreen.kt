package com.smartspends.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartspends.app.ui.components.BezierLineChart
import com.smartspends.app.ui.components.ChartLegendItem
import com.smartspends.app.ui.components.DonutChart
import com.smartspends.app.ui.components.DoubleBarChart
import com.smartspends.app.ui.theme.Emerald500
import com.smartspends.app.ui.theme.Indigo600
import com.smartspends.app.ui.theme.Rose500
import java.util.Locale

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val statsState by viewModel.stats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    val categoryColors = listOf(
        Color(0.96f, 0.42f, 0.42f), // Food - Rose/Red
        Color(1.00f, 0.62f, 0.26f), // Fuel - Orange
        Color(0.33f, 0.63f, 1.00f), // Shopping - Blue
        Color(0.11f, 0.82f, 0.63f), // Travel - Mint
        Color(0.37f, 0.15f, 0.80f), // Bills - Violet
        Color(0.95f, 0.41f, 0.88f), // Entertainment - Pink
        Color(0.06f, 0.67f, 0.52f), // Healthcare - Green
        Color(0.00f, 0.82f, 0.83f), // Education - Cyan
        Color(0.93f, 0.32f, 0.33f), // Rent - Soft Red
        Color(0.51f, 0.58f, 0.65f)  // Other - Grey
    )

    LaunchedEffect(Unit) {
        viewModel.updateCurrencySymbol()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Analytics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        val stats = statsState
        if (stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Category Expenses (Donut Chart)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Distribution of your expenses by categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val categoryMap = stats.categoryExpenses.associate { it.category to it.total }

                        if (categoryMap.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No expense records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            DonutChart(
                                data = categoryMap,
                                colors = categoryColors,
                                modifier = Modifier
                                    .size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Category Legend
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryMap.keys.forEachIndexed { index, cat ->
                                    val col = categoryColors.getOrElse(index) { Color.Gray }
                                    val totalAmt = categoryMap[cat] ?: 0.0
                                    ChartLegendItem(
                                        color = col,
                                        label = "$cat ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalAmt)})"
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Daily Spending Trend (Bezier Curve)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Daily Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your daily expenses over the last 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val dailySums = stats.dailyExpenses
                        if (dailySums.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No spending records in last 30 days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            // Extract dates (e.g. format YYYY-MM-DD -> DD MMM)
                            val labels = dailySums.map {
                                try {
                                    val date = java.time.LocalDate.parse(it.date)
                                    date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
                                } catch (e: Exception) {
                                    it.date
                                }
                            }
                            val values = dailySums.map { it.total }

                            BezierLineChart(
                                labels = labels,
                                values = values,
                                lineColor = Indigo600,
                                gradientColor = Indigo600,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }

                // Section 3: Monthly Income vs Expenses (Bar Chart)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Income vs Expense comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monthly comparison over the last 6 months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChartLegendItem(color = Emerald500, label = "Income")
                            ChartLegendItem(color = Rose500, label = "Expenses")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val monthlyData = stats.monthlyData
                        if (monthlyData.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No historical records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            val labels = monthlyData.map { it.monthLabel }
                            val incValues = monthlyData.map { it.income }
                            val expValues = monthlyData.map { it.expense }

                            DoubleBarChart(
                                labels = labels,
                                valuesIncome = incValues,
                                valuesExpense = expValues,
                                incomeColor = Emerald500,
                                expenseColor = Rose500,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
