package com.swastik.hobby.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_steps",
    foreignKeys = [
        ForeignKey(
            entity = ProjectTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TemplateStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val estimatedTimeMinutes: Int? = null
)