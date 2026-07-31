package com.example.data.model

enum class PriceLevel(val displayName: String, val code: String) {
    ECERAN("Eceran", "ECR"),
    GROSIR("Grosir", "GSR"),
    RESELLER("Reseller", "RSL"),
    VIP("VIP / Member", "VIP");

    companion object {
        fun fromName(name: String?): PriceLevel {
            if (name == null) return ECERAN
            return try {
                valueOf(name)
            } catch (e: Exception) {
                ECERAN
            }
        }
    }
}
