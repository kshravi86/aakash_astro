package com.swastik.hobby.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val status: ProjectStatus,
    val createdAt: Date,
    val updatedAt: Date,
    val completedAt: Date? = null,
    val estimatedTimeHours: Int? = null,
    val actualTimeMinutes: Long = 0,
    val difficulty: ProjectDifficulty,
    val thumbnailPath: String? = null,
    val templateId: Long? = null
)

enum class ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED
}

enum class ProjectDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}