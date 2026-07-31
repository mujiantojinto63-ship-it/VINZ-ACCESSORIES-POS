package com.example.data.model

enum class PaymentMethod(val displayName: String) {
    TUNAI("Tunai (Cash)"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank")
}

enum class PaymentStatus(val displayName: String) {
    LUNAS("PAID / LUNAS"),
    BELUM_LUNAS("BELUM LUNAS (BON)")
}

enum class POStatus(val displayName: String) {
    PENDING("Menunggu"),
    DITERIMA("Diterima (Stok Masuk)"),
    DIBATALKAN("Dibatalkan")
}

enum class JournalType(val displayName: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}
