package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.JournalType
import com.example.data.model.POStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PriceLevel

class Converters {
    @TypeConverter
    fun fromPriceLevel(value: PriceLevel?): String = value?.name ?: PriceLevel.ECERAN.name

    @TypeConverter
    fun toPriceLevel(value: String?): PriceLevel = PriceLevel.fromName(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String = value?.name ?: PaymentMethod.TUNAI.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod = try { PaymentMethod.valueOf(value ?: "") } catch (e: Exception) { PaymentMethod.TUNAI }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String = value?.name ?: PaymentStatus.LUNAS.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus = try { PaymentStatus.valueOf(value ?: "") } catch (e: Exception) { PaymentStatus.LUNAS }

    @TypeConverter
    fun fromPOStatus(value: POStatus?): String = value?.name ?: POStatus.PENDING.name

    @TypeConverter
    fun toPOStatus(value: String?): POStatus = try { POStatus.valueOf(value ?: "") } catch (e: Exception) { POStatus.PENDING }

    @TypeConverter
    fun fromJournalType(value: JournalType?): String = value?.name ?: JournalType.INCOME.name

    @TypeConverter
    fun toJournalType(value: String?): JournalType = try { JournalType.valueOf(value ?: "") } catch (e: Exception) { JournalType.INCOME }
}
