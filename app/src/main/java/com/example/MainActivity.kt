package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.screens.CategoryManagementScreen
import com.example.ui.screens.CustomerManagementScreen
import com.example.ui.screens.JournalAndPoScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductManagementScreen
import com.example.ui.screens.SalesReportScreen
import com.example.ui.screens.SettingsAndReceiptScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.theme.VinzAccessoriesTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VinzAccessoriesTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: PosViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = storeSettings?.storeName ?: "VINZ ACCESSORIES",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Aksesoris HP • Kasir & Stok",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    // Active User Badge
                    currentUser?.let { user ->
                        Surface(
                            color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (user.role == UserRole.ADMIN) Icons.Default.ManageAccounts else Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = "${user.fullName} (${user.role.displayName})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            val tabs = MainTab.values()
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                tabs.forEach { tab ->
                    val isLockedForKasir = currentUser?.role == UserRole.KASIR && (tab == MainTab.PENGGUNA || tab == MainTab.JURNAL_PO)
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (tab) {
                                        MainTab.KASIR -> "Kasir"
                                        MainTab.LAPORAN_SALES -> "Laporan"
                                        MainTab.PRODUK_STOK -> "Produk"
                                        MainTab.KATEGORI -> "Kategori"
                                        MainTab.PELANGGAN -> "Pelanggan"
                                        MainTab.PENGGUNA -> "Pengguna"
                                        MainTab.JURNAL_PO -> "Jurnal/PO"
                                        MainTab.STRUK_PRINTER -> "Struk"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isLockedForKasir) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.KASIR -> Icons.Default.PointOfSale
                                    MainTab.LAPORAN_SALES -> Icons.Default.TrendingUp
                                    MainTab.PRODUK_STOK -> Icons.Default.Inventory
                                    MainTab.KATEGORI -> Icons.Default.Category
                                    MainTab.PELANGGAN -> Icons.Default.Group
                                    MainTab.PENGGUNA -> Icons.Default.ManageAccounts
                                    MainTab.JURNAL_PO -> Icons.Default.Assessment
                                    MainTab.STRUK_PRINTER -> Icons.Default.ReceiptLong
                                },
                                contentDescription = tab.name
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isKasirRestricted = currentUser?.role == UserRole.KASIR && (selectedTab == MainTab.PENGGUNA)
            if (isKasirRestricted) {
                // Access restricted notice for Kasir role on User Management
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Akses Terbatas untuk Peran KASIR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Fitur Manajemen Pengguna hanya dapat diakses oleh Admin Toko. Silakan ganti akun ke Admin untuk mengakses fitur ini.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    MainTab.KASIR -> PosScreen(viewModel = viewModel)
                    MainTab.LAPORAN_SALES -> SalesReportScreen(viewModel = viewModel)
                    MainTab.PRODUK_STOK -> ProductManagementScreen(viewModel = viewModel)
                    MainTab.KATEGORI -> CategoryManagementScreen(viewModel = viewModel)
                    MainTab.PELANGGAN -> CustomerManagementScreen(viewModel = viewModel)
                    MainTab.PENGGUNA -> UserManagementScreen(viewModel = viewModel)
                    MainTab.JURNAL_PO -> JournalAndPoScreen(viewModel = viewModel)
                    MainTab.STRUK_PRINTER -> SettingsAndReceiptScreen(viewModel = viewModel)
                }
            }
        }
    }
}

