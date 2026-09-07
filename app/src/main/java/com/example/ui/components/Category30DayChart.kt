package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkLog
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.SarabunFontFamily

@Composable
fun Category30DayChart(
    allLogs: List<WorkLog>,
    modifier: Modifier = Modifier
) {
    val thirtyDaysAgo = remember {
        System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    }

    val logsLast30Days = remember(allLogs) {
        allLogs.filter { it.timestamp >= thirtyDaysAgo }
    }

    val total30Days = logsLast30Days.size

    val categoryColors = remember {
        mapOf(
            "ซ่อมบำรุง" to Color(0xFFEC4899),   // Pink / Rose
            "ติดตั้ง" to Color(0xFF3B82F6),     // Blue
            "ส่งสินค้า" to Color(0xFFF97316),   // Orange
            "บริการลูกค้า" to Color(0xFF10B981), // Emerald Green
            "งานทั่วไป" to Color(0xFF8B5CF6)   // Purple
        )
    }

    val categoryStats = remember(logsLast30Days) {
        WorkLog.CATEGORIES.map { category ->
            val count = logsLast30Days.count { it.category == category }
            val percentage = if (total30Days > 0) (count.toFloat() / total30Days) * 100f else 0f
            CategoryChartData(
                category = category,
                count = count,
                percentage = percentage,
                color = categoryColors[category] ?: Color(0xFF6B7280)
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_30day_chart_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
        border = BorderStroke(1.dp, Color(0xFF374151))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📊 การกระจายหมวดหมู่งาน 30 วันล่าสุด",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = SarabunFontFamily
                )
                Text(
                    text = "สัดส่วนประเภทงานสะสมย้อนหลัง 30 วัน (รวมทั้งหมด $total30Days เคส)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = SarabunFontFamily
                )
            }

            // Donut Chart & Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Compose Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 22.dp.toPx()
                        if (total30Days == 0) {
                            drawArc(
                                color = Color(0xFF374151),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            var startAngle = -90f
                            categoryStats.forEach { stat ->
                                val sweepAngle = (stat.count.toFloat() / total30Days) * 360f
                                if (sweepAngle > 0f) {
                                    drawArc(
                                        color = stat.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }
                    }

                    // Center Stats in Donut
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total30Days",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = SarabunFontFamily
                        )
                        Text(
                            text = "เคส (30วัน)",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = SarabunFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Compact Legend Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryStats.forEach { stat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(stat.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stat.category,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = SarabunFontFamily
                                )
                            }
                            Text(
                                text = "${stat.count} (${String.format("%.0f", stat.percentage)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
            }

            // Detailed Progress Bar Breakdown per Category
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryStats.forEach { stat ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0),
                                fontFamily = SarabunFontFamily
                            )
                            Text(
                                text = "${stat.count} เคส  |  ${String.format("%.1f", stat.percentage)}%",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = SarabunFontFamily
                            )
                        }
                        LinearProgressIndicator(
                            progress = if (total30Days > 0) stat.count.toFloat() / total30Days else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = stat.color,
                            trackColor = Color(0xFF111827)
                        )
                    }
                }
            }
        }
    }
}

private data class CategoryChartData(
    val category: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)
