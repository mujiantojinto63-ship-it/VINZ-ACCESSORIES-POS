package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.CartItem
import com.example.data.model.PaymentStatus
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    cartItems: List<CartItem>,
    storeSettings: StoreSettings?,
    onDismiss: () -> Unit,
    onOpenPrinterDialog: () -> Unit
) {
    val context = LocalContext.current
    var showBluetoothPrinter by remember { mutableStateOf(false) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val storeName = storeSettings?.storeName ?: "VINZ ACCESSORIES"
    val storeAddress = storeSettings?.address ?: "Jl. Accessories HP No. 88, Plaza Seluler"
    val storePhone = storeSettings?.phone ?: "0812-3456-7890"
    val footerNotes = storeSettings?.footerNotes ?: "Terima kasih telah berbelanja di VINZ ACCESSORIES!"

    val formattedDate = remember(transaction.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(transaction.timestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Struk Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Printable Receipt Scroll View (Styled like thermal paper)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Header
                        Text(
                            text = storeName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = storeAddress,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Telp: $storePhone",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "================================",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        // Transaction Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("No Struk:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(transaction.receiptNo, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tgl / Waktu:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(formattedDate, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pelanggan:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(transaction.customerName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Level Harga:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(transaction.priceLevelUsed.displayName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Metode Bayar:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(transaction.paymentMethod.displayName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }

                        // Status Badge (Lunas / Belum Lunas)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (transaction.paymentStatus == PaymentStatus.LUNAS) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = transaction.paymentStatus.displayName.uppercase(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Itemized Table
                        cartItems.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    text = item.productName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${currencyFormat.format(item.unitPrice)}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = currencyFormat.format(item.totalPrice),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Calculation Totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(currencyFormat.format(transaction.subtotal), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        if (transaction.discount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Diskon:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Red)
                                Text("-${currencyFormat.format(transaction.discount)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Red)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL:", fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(currencyFormat.format(transaction.total), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        if (transaction.paymentMethod == com.example.data.model.PaymentMethod.TUNAI) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dibayar (Tunai):", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text(currencyFormat.format(transaction.amountPaid), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kembalian:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text(currencyFormat.format(transaction.change), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        if (transaction.paymentStatus == PaymentStatus.BELUM_LUNAS) {
                            val sisaPiutang = (transaction.total - transaction.amountPaid).coerceAtLeast(0.0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sisa Piutang:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Red)
                                Text(currencyFormat.format(sisaPiutang), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }

                        if (transaction.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF8E1), shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFFFB300), shape = RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Catatan Kasir / Keterangan Manual:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = transaction.notes,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Text(
                            text = "================================",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Footer Note (Catatan di bawah struk)
                        Text(
                            text = footerNotes,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons (Print Bluetooth, Share Text, Done)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareTextReceipt(context, transaction, cartItems, storeName, footerNotes, currencyFormat)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan")
                    }

                    Button(
                        onClick = {
                            showBluetoothPrinter = true
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Bluetooth")
                    }
                }
            }
        }
    }

    if (showBluetoothPrinter) {
        BluetoothPrinterDialog(
            transaction = transaction,
            cartItems = cartItems,
            storeSettings = storeSettings,
            onDismiss = { showBluetoothPrinter = false }
        )
    }
}

private fun shareTextReceipt(
    context: Context,
    t: TransactionEntity,
    items: List<CartItem>,
    storeName: String,
    footer: String,
    fmt: NumberFormat
) {
    val sb = StringBuilder()
    sb.append("*$storeName*\n")
    sb.append("No: ${t.receiptNo}\n")
    sb.append("Pelanggan: ${t.customerName}\n")
    sb.append("Status: ${t.paymentStatus.displayName}\n")
    sb.append("----------------------------\n")
    items.forEach { item ->
        sb.append("${item.productName}\n")
        sb.append("${item.quantity} x ${fmt.format(item.unitPrice)} = ${fmt.format(item.totalPrice)}\n")
    }
    sb.append("----------------------------\n")
    sb.append("TOTAL: ${fmt.format(t.total)}\n")
    sb.append("Bayar: ${fmt.format(t.amountPaid)}\n")
    if (t.paymentMethod == com.example.data.model.PaymentMethod.TUNAI) {
        sb.append("Kembalian: ${fmt.format(t.change)}\n")
    }
    if (t.notes.isNotEmpty()) {
        sb.append("----------------------------\n")
        sb.append("Catatan Kasir: ${t.notes}\n")
    }
    sb.append("----------------------------\n")
    sb.append(footer)

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk VINZ ACCESSORIES"))
}
