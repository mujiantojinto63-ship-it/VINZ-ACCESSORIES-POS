package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BluetoothDeviceSim(val name: String, val address: String, val isConnected: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterDialog(
    transaction: TransactionEntity,
    cartItems: List<CartItem>,
    storeSettings: StoreSettings?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Preview Struk, 1: Bluetooth Printer
    var paperWidthMm by remember { mutableIntStateOf(58) } // 58mm or 80mm

    var hasBtPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasBtPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    val simulatedPrinters = remember {
        listOf(
            BluetoothDeviceSim("Thermal Printer RPP02N (58mm)", "00:11:22:33:44:55"),
            BluetoothDeviceSim("Bluetooth POS Printer 80mm", "AA:BB:CC:DD:EE:FF"),
            BluetoothDeviceSim("EPN Bluetooth Printer", "12:34:56:78:90:AB")
        )
    }

    var selectedPrinter by remember { mutableStateOf<BluetoothDeviceSim?>(simulatedPrinters.first()) }
    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }
    var isPrinting by remember { mutableStateOf(false) }
    var printSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Preview & Cetak Struk",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Struk #${transaction.receiptNo}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs (0: Preview Struk, 1: Bluetooth Printer)
                PrimaryTabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Struk", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Printer BT", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == 0) {
                    // TAB 0: PREVIEW STRUK THERMAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ukuran Kertas:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(58, 80).forEach { size ->
                                val selected = paperWidthMm == size
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { paperWidthMm = size }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${size}mm",
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Printable Receipt Frame Simulation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currencyFormat = remember {
                                NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                    maximumFractionDigits = 0
                                }
                            }
                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")) }

                            val storeName = storeSettings?.storeName ?: "VINZ ACCESSORIES"
                            val storeAddress = storeSettings?.address ?: "Jl. Aksesoris No. 88, City Center"
                            val storePhone = storeSettings?.phone ?: "0812-3456-7890"
                            val footerNotes = storeSettings?.footerNotes ?: "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar."

                            Text(storeName, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(storeAddress, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                            Text("Telp: $storePhone", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (paperWidthMm == 58) "------------------------" else "--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)

                            // Metadata
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("No: ${transaction.receiptNo}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Tgl: ${dateFormat.format(Date(transaction.timestamp))}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Kasir: Kasir Utama", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Pelanggan: ${transaction.customerName}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Metode: ${transaction.paymentMethod.displayName}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                // Status Badge
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status: ", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = transaction.paymentStatus.displayName,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.paymentStatus == PaymentStatus.LUNAS) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (paperWidthMm == 58) "------------------------" else "--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)

                            // Items Table
                            cartItems.forEach { item ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                    Text(item.productName, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.quantity} x ${currencyFormat.format(item.unitPrice)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                                        Text(currencyFormat.format(item.totalPrice), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (paperWidthMm == 58) "------------------------" else "--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)

                            // Totals
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(currencyFormat.format(transaction.subtotal), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (transaction.discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diskon:", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Red)
                                    Text("-${currencyFormat.format(transaction.discount)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Red)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL:", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text(currencyFormat.format(transaction.total), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            if (transaction.paymentMethod == PaymentMethod.TUNAI) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bayar (Tunai):", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(currencyFormat.format(transaction.amountPaid), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kembali:", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(currencyFormat.format(transaction.change), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else if (transaction.paymentStatus == PaymentStatus.BELUM_LUNAS) {
                                val sisa = (transaction.total - transaction.amountPaid).coerceAtLeast(0.0)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("DP / Bayar:", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(currencyFormat.format(transaction.amountPaid), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sisa Piutang:", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Red, fontWeight = FontWeight.Bold)
                                    Text(currencyFormat.format(sisa), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (transaction.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF8E1), RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("Catatan Kasir:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                        Text(transaction.notes, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(footerNotes, fontSize = 9.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, color = Color.DarkGray)
                        }
                    }
                } else {
                    // TAB 1: BLUETOOTH PRINTER MANAGEMENT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isConnected) Color(0xFF2E7D32) else Color.Red,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConnected) "Terhubung ke: ${selectedPrinter?.name}" else "Printer Belum Terhubung",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isConnected) "MAC: ${selectedPrinter?.address}" else "Pilih printer thermal dari daftar",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Daftar Perangkat Thermal Bluetooth:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(simulatedPrinters) { device ->
                            val isThisSelected = selectedPrinter?.address == device.address
                            Card(
                                onClick = {
                                    selectedPrinter = device
                                    isConnecting = true
                                    scope.launch {
                                        delay(1000)
                                        isConnecting = false
                                        isConnected = true
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isThisSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isThisSelected && isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (isThisSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = device.address, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    if (isThisSelected && isConnecting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else if (isThisSelected && isConnected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isPrinting) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("Mencetak data ke printer Bluetooth...", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (printSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Struk #${transaction.receiptNo} Sukses Dicetak!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Batal")
                    }

                    // Native Android Print Manager Trigger (Samsung / Standard Print Preview)
                    OutlinedButton(
                        onClick = {
                            printReceiptViaSystem(context, transaction, cartItems, storeSettings)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview System", fontSize = 12.sp)
                    }

                    // Direct Thermal Print
                    Button(
                        onClick = {
                            if (!isConnected) return@Button
                            isPrinting = true
                            printSuccess = false
                            scope.launch {
                                delay(1800)
                                isPrinting = false
                                printSuccess = true
                            }
                        },
                        enabled = isConnected && !isPrinting,
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPrinting) "Mencetak..." else "Cetak BT", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun printReceiptViaSystem(
    context: Context,
    transaction: TransactionEntity,
    cartItems: List<CartItem>,
    storeSettings: StoreSettings?
) {
    try {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

        val storeName = storeSettings?.storeName ?: "VINZ ACCESSORIES"
        val storeAddress = storeSettings?.address ?: "Jl. Aksesoris No. 88, City Center"
        val storePhone = storeSettings?.phone ?: "0812-3456-7890"
        val footerNotes = storeSettings?.footerNotes ?: "Terima Kasih Atas Kunjungan Anda!"

        val itemsHtml = cartItems.joinToString("") { item ->
            """
            <tr>
                <td style="padding: 2px 0;"><b>${item.productName}</b><br><small>${item.quantity} x ${currencyFormat.format(item.unitPrice)}</small></td>
                <td style="text-align: right; vertical-align: bottom;"><b>${currencyFormat.format(item.totalPrice)}</b></td>
            </tr>
            """.trimIndent()
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: monospace; font-size: 12px; margin: 0; padding: 10px; width: 280px; }
                    .center { text-align: center; }
                    .bold { font-weight: bold; }
                    .line { border-top: 1px dashed #000; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; font-size: 11px; }
                </style>
            </head>
            <body>
                <div class="center bold" style="font-size: 16px;">$storeName</div>
                <div class="center">$storeAddress</div>
                <div class="center">Telp: $storePhone</div>
                <div class="line"></div>
                <div>No: ${transaction.receiptNo}</div>
                <div>Tgl: ${dateFormat.format(Date(transaction.timestamp))}</div>
                <div>Kasir: Kasir Utama</div>
                <div>Pelanggan: ${transaction.customerName}</div>
                <div>Status: <b>${transaction.paymentStatus.displayName}</b></div>
                <div class="line"></div>
                <table>
                    $itemsHtml
                </table>
                <div class="line"></div>
                <table>
                    <tr><td>Subtotal:</td><td style="text-align:right;">${currencyFormat.format(transaction.subtotal)}</td></tr>
                    ${if (transaction.discount > 0) "<tr><td>Diskon:</td><td style=\"text-align:right; color:red;\">-${currencyFormat.format(transaction.discount)}</td></tr>" else ""}
                    <tr class="bold"><td style="font-size:14px;">TOTAL:</td><td style="text-align:right; font-size:14px;">${currencyFormat.format(transaction.total)}</td></tr>
                    ${if (transaction.paymentMethod == PaymentMethod.TUNAI) "<tr><td>Bayar:</td><td style=\"text-align:right;\">${currencyFormat.format(transaction.amountPaid)}</td></tr><tr><td>Kembali:</td><td style=\"text-align:right;\">${currencyFormat.format(transaction.change)}</td></tr>" else ""}
                </table>
                ${if (transaction.notes.isNotEmpty()) "<div style=\"margin-top:6px; background:#fff8e1; padding:4px;\"><b>Catatan:</b> ${transaction.notes}</div>" else ""}
                <div class="line"></div>
                <div class="center" style="font-size:10px;">$footerNotes</div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Struk_${transaction.receiptNo}")
                printManager?.print("Struk_${transaction.receiptNo}", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
