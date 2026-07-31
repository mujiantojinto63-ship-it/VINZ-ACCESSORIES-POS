package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.POStatus
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItem
import com.example.ui.viewmodel.PosViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalAndPoScreen(viewModel: PosViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Jurnal Keuangan, 1: Pesanan Pembelian (PO)

    val journalEntries by viewModel.journalEntries.collectAsState()
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()
    val products by viewModel.products.collectAsState()

    var showAddJournalModal by remember { mutableStateOf(false) }
    var showCreatePoModal by remember { mutableStateOf(false) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddJournalModal = true else showCreatePoModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Jurnal Keuangan", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pesanan Pembelian (PO)", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Jurnal Keuangan Tab
                val totalIncome = remember(journalEntries) {
                    journalEntries.filter { it.type == JournalType.INCOME }.sumOf { it.amount }
                }
                val totalExpense = remember(journalEntries) {
                    journalEntries.filter { it.type == JournalType.EXPENSE }.sumOf { it.amount }
                }
                val netProfit = totalIncome - totalExpense

                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Summary Financial Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ringkasan Kas & Jurnal Keuangan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Text(" Total Pemasukan", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(currencyFormat.format(totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                        Text(" Total Pengeluaran", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(currencyFormat.format(totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }

                                Column {
                                    Text("Saldo Net", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        currencyFormat.format(netProfit),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netProfit >= 0) MaterialTheme.colorScheme.primary else Color.Red
                                    )
                                }
                            }
                        }
                    }

                    // Journal Entry Logs
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(journalEntries, key = { it.id }) { entry ->
                            val isIncome = entry.type == JournalType.INCOME
                            val formattedTime = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Kategori: ${entry.category} • $formattedTime", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Text(
                                        text = (if (isIncome) "+" else "-") + currencyFormat.format(entry.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Pesanan Pembelian (PO) Tab
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Text(
                        text = "Daftar Pesanan Pembelian (PO Supplier)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(purchaseOrders, key = { it.id }) { po ->
                            val items = viewModel.parsePOItems(po.itemsJson)
                            val isReceived = po.status == POStatus.DITERIMA
                            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(po.timestamp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(po.poNumber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                            Text("Supplier: ${po.supplierName} • $formattedDate", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isReceived) Color(0xFF2E7D32) else Color(0xFFFF8F00),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = po.status.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• ${item.productName} (${item.quantity} pcs)", fontSize = 12.sp)
                                            Text(currencyFormat.format(item.subtotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total PO: ${currencyFormat.format(po.totalCost)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (!isReceived) {
                                            Button(
                                                onClick = { viewModel.receivePO(po) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("TERIMA STOK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Stok Sudah Masuk", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add Manual Journal Entry
    if (showAddJournalModal) {
        var jType by remember { mutableStateOf(JournalType.EXPENSE) }
        var category by remember { mutableStateOf("Operasional") }
        var amountStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddJournalModal = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catat Jurnal Manual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { jType = JournalType.INCOME },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jType == JournalType.INCOME) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pemasukan")
                        }
                        Button(
                            onClick = { jType = JournalType.EXPENSE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jType == JournalType.EXPENSE) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pengeluaran")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (e.g. Listrik, Gaji, Sewa)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Jumlah Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Detail") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddJournalModal = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0 && description.isNotBlank()) {
                                    viewModel.addManualJournal(jType, category, amt, description)
                                    showAddJournalModal = false
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Simpan Jurnal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Create Purchase Order (PO)
    if (showCreatePoModal) {
        var supplierName by remember { mutableStateOf("") }
        var supplierPhone by remember { mutableStateOf("") }
        var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
        var poQtyStr by remember { mutableStateOf("10") }
        var poCostStr by remember { mutableStateOf(selectedProduct?.costPrice?.toLong()?.toString() ?: "10000") }
        var poItems by remember { mutableStateOf<List<PurchaseOrderItem>>(emptyList()) }

        Dialog(onDismissRequest = { showCreatePoModal = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Buat Pesanan Pembelian (PO)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Nama Supplier / Distributor") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = supplierPhone,
                        onValueChange = { supplierPhone = it },
                        label = { Text("No. WA Supplier") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Tambah Item ke PO:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Product Selector dropdown or simple list
                    products.take(5).forEach { p ->
                        Card(
                            onClick = {
                                selectedProduct = p
                                poCostStr = p.costPrice.toLong().toString()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedProduct?.id == p.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text(p.name, modifier = Modifier.padding(8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = poQtyStr,
                            onValueChange = { poQtyStr = it },
                            label = { Text("Jumlah Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = poCostStr,
                            onValueChange = { poCostStr = it },
                            label = { Text("Harga Beli / Pcs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val prod = selectedProduct ?: return@Button
                            val qty = poQtyStr.toIntOrNull() ?: 1
                            val cost = poCostStr.toDoubleOrNull() ?: prod.costPrice
                            poItems = poItems + PurchaseOrderItem(prod.id, prod.name, cost, qty)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambah Item ke Daftar PO")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Item list in PO preview
                    poItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} (${item.quantity} x ${currencyFormat.format(item.costPrice)})", fontSize = 11.sp)
                            Text(currencyFormat.format(item.subtotal), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCreatePoModal = false }, modifier = Modifier.weight(1f)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (supplierName.isNotBlank() && poItems.isNotEmpty()) {
                                    viewModel.createPO(supplierName, supplierPhone, poItems, "PO Stok Aksesoris")
                                    showCreatePoModal = false
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("SIMPAN PO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
