package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.DateRangeFilter
import com.example.ui.viewmodel.PosViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(viewModel: PosViewModel) {
    val reportTransactions by viewModel.filteredTransactionsForReport.collectAsState()
    val activeFilter by viewModel.salesDateFilter.collectAsState()

    var paymentStatusFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }

    // Aggregate Calculations
    val totalRevenue = reportTransactions.sumOf { it.total }
    val totalTransactionCount = reportTransactions.size

    val lunasTransactions = remember(reportTransactions) { reportTransactions.filter { it.paymentStatus == PaymentStatus.LUNAS } }
    val belumLunasTransactions = remember(reportTransactions) { reportTransactions.filter { it.paymentStatus == PaymentStatus.BELUM_LUNAS } }

    val totalLunasRevenue = lunasTransactions.sumOf { it.total }
    val totalBelumLunasAmount = belumLunasTransactions.sumOf { (it.total - it.amountPaid).coerceAtLeast(0.0) }

    val displayedTransactions = remember(reportTransactions, paymentStatusFilter) {
        if (paymentStatusFilter == null) reportTransactions
        else reportTransactions.filter { it.paymentStatus == paymentStatusFilter }
    }

    // Parse all sold items across filtered transactions
    val allSoldItems = remember(reportTransactions) {
        reportTransactions.flatMap { trans ->
            viewModel.parseCartItems(trans.itemsJson)
        }
    }

    val totalItemsSold = allSoldItems.sumOf { it.quantity }

    // Estimated Profit Calculation: total revenue - total cost of products sold
    val totalCost = allSoldItems.sumOf { it.costPrice * it.quantity }
    val totalProfit = totalRevenue - totalCost

    // Top Selling Products Calculation
    val topSellingProducts = remember(allSoldItems) {
        allSoldItems
            .groupBy { it.productId to it.productName }
            .map { (key, items) ->
                val totalQty = items.sumOf { it.quantity }
                val totalSales = items.sumOf { it.totalPrice }
                TopProductInfo(
                    productId = key.first,
                    productName = key.second,
                    quantitySold = totalQty,
                    revenueGenerated = totalSales
                )
            }
            .sortedByDescending { it.quantitySold }
            .take(5)
    }

    val maxQtySold = topSellingProducts.firstOrNull()?.quantitySold ?: 1

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sales_report_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Report",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Laporan Penjualan Aksesoris",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analisis omset pendapatan, jumlah transaksi, dan produk terlaris toko",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Date Range Filter Chips Bar
            item {
                Column {
                    Text(
                        text = "Filter Rentang Waktu",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DateRangeFilter.values()) { filter ->
                            FilterChip(
                                selected = filter == activeFilter,
                                onClick = { viewModel.setSalesDateFilter(filter) },
                                label = { Text(filter.displayName, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (filter == activeFilter) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.height(14.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("filter_chip_${filter.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Filter Status Pembayaran",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = paymentStatusFilter == null,
                                onClick = { paymentStatusFilter = null },
                                label = { Text("Semua Status (${reportTransactions.size})", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (paymentStatusFilter == null) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.height(14.dp))
                                    }
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = paymentStatusFilter == PaymentStatus.LUNAS,
                                onClick = { paymentStatusFilter = PaymentStatus.LUNAS },
                                label = { Text("PAID / LUNAS (${lunasTransactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8F5E9)),
                                leadingIcon = {
                                    if (paymentStatusFilter == PaymentStatus.LUNAS) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.height(14.dp))
                                    }
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = paymentStatusFilter == PaymentStatus.BELUM_LUNAS,
                                onClick = { paymentStatusFilter = PaymentStatus.BELUM_LUNAS },
                                label = { Text("BELUM LUNAS / BON (${belumLunasTransactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE)),
                                leadingIcon = {
                                    if (paymentStatusFilter == PaymentStatus.BELUM_LUNAS) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.height(14.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // KPI Overview Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricCard(
                            title = "Omset Paid / Lunas",
                            value = formatRupiah(totalLunasRevenue),
                            subtitle = "${lunasTransactions.size} Transaksi PAID",
                            icon = Icons.Default.AttachMoney,
                            containerColor = Color(0xFFE8F5E9),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "Belum Lunas (Piutang)",
                            value = formatRupiah(totalBelumLunasAmount),
                            subtitle = "${belumLunasTransactions.size} Transaksi BON",
                            icon = Icons.Default.Receipt,
                            containerColor = Color(0xFFFFEBEE),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricCard(
                            title = "Estimasi Laba Bersih",
                            value = formatRupiah(totalProfit),
                            subtitle = "Laba dari ${totalItemsSold} Pcs",
                            icon = Icons.Default.TrendingUp,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "Total Transaksi",
                            value = "$totalTransactionCount Struk",
                            subtitle = "Omset: ${formatRupiah(totalRevenue)}",
                            icon = Icons.Default.LocalOffer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Top Selling Products Section (Produk Terlaris)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("top_selling_products_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Produk Terlaris (Top Selling)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Aksesoris paling banyak dibeli pelanggan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (topSellingProducts.isEmpty()) {
                            Text(
                                text = "Belum ada transaksi penjualan pada rentang waktu ini.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                topSellingProducts.forEachIndexed { index, item ->
                                    val progress = item.quantitySold.toFloat() / maxQtySold.toFloat()
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = item.productName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            }

                                            Text(
                                                text = "${item.quantitySold} Pcs (${formatRupiah(item.revenueGenerated)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Transaction History List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Transaksi Penjualan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${displayedTransactions.size} Transaksi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (displayedTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada transaksi ditemukan untuk filter ini.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { trans ->
                    TransactionReportCardItem(
                        transaction = trans,
                        onClick = { selectedTransactionForDetail = trans }
                    )
                }
            }
        }
    }

    // Transaction Detail Modal
    if (selectedTransactionForDetail != null) {
        TransactionDetailDialog(
            transaction = selectedTransactionForDetail!!,
            items = viewModel.parseCartItems(selectedTransactionForDetail!!.itemsJson),
            onDismiss = { selectedTransactionForDetail = null },
            onReprint = {
                viewModel.setTransactionForReceipt(selectedTransactionForDetail!!)
                selectedTransactionForDetail = null
            }
        )
    }
}

@Composable
fun ReportMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TransactionReportCardItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_transaction_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.receiptNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (transaction.paymentStatus == PaymentStatus.LUNAS) Color(0xFF2E7D32) else Color(0xFFC62828),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = transaction.paymentStatus.displayName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${transaction.customerName} • ${transaction.paymentMethod.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (transaction.notes.isNotEmpty()) {
                    Text(
                        text = "Catatan: ${transaction.notes}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(transaction.total),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (transaction.discount > 0) {
                        Text(
                            text = "Disc: ${formatRupiah(transaction.discount)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    items: List<CartItem>,
    onDismiss: () -> Unit,
    onReprint: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date(transaction.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Detail Struk Transaksi", fontWeight = FontWeight.Bold)
                Text(transaction.receiptNo, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Waktu: $dateStr", fontSize = 12.sp)
                Text("Pelanggan: ${transaction.customerName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Metode: ${transaction.paymentMethod.displayName} (${transaction.paymentStatus.displayName})", fontSize = 12.sp)

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Item Dibeli (${items.sumOf { it.quantity }} Pcs):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                LazyColumn(
                    modifier = Modifier.height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${item.quantity} x ${formatRupiah(item.unitPrice)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatRupiah(item.totalPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 12.sp)
                    Text(formatRupiah(transaction.subtotal), fontSize = 12.sp)
                }
                if (transaction.discount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Text("-${formatRupiah(transaction.discount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Akhir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatRupiah(transaction.total), fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }

                if (transaction.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Catatan / Keterangan Manual Struk:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(transaction.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onReprint,
                modifier = Modifier.testTag("reprint_receipt_button")
            ) {
                Text("Cetak / Lihat Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

data class TopProductInfo(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val revenueGenerated: Double
)

private fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace(",00", "")
}
