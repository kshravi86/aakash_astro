package com.swastik.hobby.data.dao

import androidx.room.*
import com.swastik.hobby.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: ProjectStatus): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE category = :category ORDER BY updatedAt DESC")
    fun getProjectsByCategory(category: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Query("SELECT * FROM project_steps WHERE projectId = :projectId ORDER BY orderIndex")
    fun getProjectSteps(projectId: Long): Flow<List<ProjectStep>>

    @Query("SELECT * FROM project_photos WHERE projectId = :projectId ORDER BY orderIndex, takenAt")
    fun getProjectPhotos(projectId: Long): Flow<List<ProjectPhoto>>

    @Query("SELECT * FROM project_materials WHERE projectId = :projectId")
    fun getProjectMaterials(projectId: Long): Flow<List<ProjectMaterial>>

    @Query("SELECT * FROM time_entries WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getProjectTimeEntries(projectId: Long): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTimeEntry(): TimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectStep(step: ProjectStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectPhoto(photo: ProjectPhoto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectMaterial(material: ProjectMaterial): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeEntry(timeEntry: TimeEntry): Long

    @Update
    suspend fun updateProject(project: Project)

    @Update
    suspend fun updateProjectStep(step: ProjectStep)

    @Update
    suspend fun updateTimeEntry(timeEntry: TimeEntry)

    @Delete
    suspend fun deleteProject(project: Project)

    @Delete
    suspend fun deleteProjectStep(step: ProjectStep)

    @Delete
    suspend fun deleteProjectPhoto(photo: ProjectPhoto)

    @Query("UPDATE project_steps SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :stepId")
    suspend fun updateStepCompletion(stepId: Long, isCompleted: Boolean, completedAt: Long?)
}