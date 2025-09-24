package com.swastik.hobby.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val unit: String,
    val currentQuantity: Double,
    val minQuantity: Double,
    val maxQuantity: Double? = null,
    val costPerUnit: BigDecimal? = null,
    val supplier: String? = null,
    val notes: String? = null,
    val imagePath: String? = null
)