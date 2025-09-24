package com.swastik.hobby.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "project_templates")
data class ProjectTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val difficulty: ProjectDifficulty,
    val estimatedTimeHours: Int? = null,
    val createdAt: Date,
    val isPublic: Boolean = false,
    val thumbnailPath: String? = null
)