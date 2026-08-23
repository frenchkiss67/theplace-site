package com.theplace.receiptscanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.category_none
import com.theplace.receiptscanner.resources.detail_back
import com.theplace.receiptscanner.resources.stats_empty
import com.theplace.receiptscanner.resources.stats_month_section
import com.theplace.receiptscanner.resources.stats_title
import com.theplace.receiptscanner.resources.stats_year_section
import com.theplace.receiptscanner.util.formatAmount
import com.theplace.receiptscanner.util.monthShortLabel
import com.theplace.receiptscanner.util.nowMs
import com.theplace.receiptscanner.util.statsByCategoryForMonth
import com.theplace.receiptscanner.util.statsByLast12Months
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    receipts: List<Receipt>,
    onBack: () -> Unit,
) {
    val now = remember { nowMs() }
    val byCategory = remember(receipts, now) { receipts.statsByCategoryForMonth(now) }
    val byMonth = remember(receipts, now) { receipts.statsByLast12Months(now) }
    val totalMonth = byCategory.sumOf { it.totalCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.detail_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (totalMonth == 0L && byMonth.all { it.totalCents == 0L }) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.stats_month_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatAmount(totalMonth),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (byCategory.isNotEmpty() && totalMonth > 0) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            slices = byCategory.map { it.totalCents.toFloat() to colorFor(it.category) },
                            modifier = Modifier.size(160.dp).padding(8.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            byCategory.forEach { cs -> CategoryLegendLine(cs.category, cs.totalCents) }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.stats_year_section),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                MonthBarChart(
                    months = byMonth.map { ms -> monthShortLabel(ms.month) to ms.totalCents.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp),
                )
            }
            // Détail mensuel sous le bar chart.
            items(byMonth.reversed()) { ms ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = "${monthShortLabel(ms.month)} ${ms.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (ms.totalCents > 0) formatAmount(ms.totalCents) else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendLine(category: ReceiptCategory?, cents: Long) {
    val label = category?.let { stringResource(it.labelRes()) }
        ?: stringResource(Res.string.category_none)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colorFor(category)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = formatAmount(cents), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DonutChart(
    slices: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Canvas(modifier = modifier) {
        val stroke = 24.dp.toPx()
        val side = kotlin.math.min(size.width, size.height) - stroke
        var startAngle = -90f
        slices.forEach { (value, color) ->
            val sweep = (value / total) * 360f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                size = Size(side, side),
                style = Stroke(width = stroke),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun MonthBarChart(
    months: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
) {
    val max = months.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { (label, value) ->
            Column(
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height((value / max * 140f).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}

/** Palette stable par catégorie (Material 3 tonal). */
@Composable
private fun colorFor(category: ReceiptCategory?): Color = when (category) {
    ReceiptCategory.Groceries -> MaterialTheme.colorScheme.primary
    ReceiptCategory.Restaurant -> MaterialTheme.colorScheme.tertiary
    ReceiptCategory.Fuel -> MaterialTheme.colorScheme.secondary
    ReceiptCategory.Health -> MaterialTheme.colorScheme.error
    ReceiptCategory.Shopping -> MaterialTheme.colorScheme.outline
    ReceiptCategory.Other -> MaterialTheme.colorScheme.inverseSurface
    null -> MaterialTheme.colorScheme.surfaceVariant
}
