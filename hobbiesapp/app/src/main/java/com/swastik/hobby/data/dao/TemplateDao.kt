package com.swastik.hobby.data.dao

import androidx.room.*
import com.swastik.hobby.data.entity.ProjectTemplate
import com.swastik.hobby.data.entity.TemplateStep
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM project_templates ORDER BY name")
    fun getAllTemplates(): Flow<List<ProjectTemplate>>

    @Query("SELECT * FROM project_templates WHERE category = :category ORDER BY name")
    fun getTemplatesByCategory(category: String): Flow<List<ProjectTemplate>>

    @Query("SELECT * FROM project_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): ProjectTemplate?

    @Query("SELECT * FROM template_steps WHERE templateId = :templateId ORDER BY orderIndex")
    fun getTemplateSteps(templateId: Long): Flow<List<TemplateStep>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ProjectTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateStep(step: TemplateStep): Long

    @Update
    suspend fun updateTemplate(template: ProjectTemplate)

    @Delete
    suspend fun deleteTemplate(template: ProjectTemplate)

    @Delete
    suspend fun deleteTemplateStep(step: TemplateStep)
}