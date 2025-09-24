package com.swastik.hobby.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "project_steps",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProjectStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val estimatedTimeMinutes: Int? = null,
    val actualTimeMinutes: Long = 0,
    val notes: String? = null
)