package com.swastik.hobby.data.converter

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
}