package com.swastik.hobby.data.repository

import com.swastik.hobby.data.dao.*
import com.swastik.hobby.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

class HobbyRepository(
    private val projectDao: ProjectDao,
    private val materialDao: MaterialDao,
    private val templateDao: TemplateDao
) {
    
    // Project operations
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
    
    fun getProjectsByStatus(status: ProjectStatus): Flow<List<Project>> = 
        projectDao.getProjectsByStatus(status)
    
    suspend fun getProjectById(id: Long): Project? = projectDao.getProjectById(id)
    
    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)
    
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    
    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)
    
    // Project steps
    fun getProjectSteps(projectId: Long): Flow<List<ProjectStep>> = 
        projectDao.getProjectSteps(projectId)
    
    suspend fun insertProjectStep(step: ProjectStep): Long = 
        projectDao.insertProjectStep(step)
    
    suspend fun updateProjectStep(step: ProjectStep) = projectDao.updateProjectStep(step)
    
    suspend fun completeStep(stepId: Long) {
        projectDao.updateStepCompletion(stepId, true, Date().time)
    }
    
    suspend fun uncompleteStep(stepId: Long) {
        projectDao.updateStepCompletion(stepId, false, null)
    }
    
    // Materials
    fun getAllMaterials(): Flow<List<Material>> = materialDao.getAllMaterials()
    
    fun getLowStockMaterials(): Flow<List<Material>> = materialDao.getLowStockMaterials()
    
    suspend fun insertMaterial(material: Material): Long = materialDao.insertMaterial(material)
    
    suspend fun updateMaterial(material: Material) = materialDao.updateMaterial(material)
    
    suspend fun consumeMaterial(materialId: Long, quantity: Double) = 
        materialDao.consumeMaterial(materialId, quantity)
    
    // Photos
    fun getProjectPhotos(projectId: Long): Flow<List<ProjectPhoto>> = 
        projectDao.getProjectPhotos(projectId)
    
    suspend fun insertProjectPhoto(photo: ProjectPhoto): Long = 
        projectDao.insertProjectPhoto(photo)
    
    // Time tracking
    suspend fun startTimeEntry(projectId: Long, stepId: Long? = null): Long {
        // End any active time entry
        getActiveTimeEntry()?.let { activeEntry ->
            val duration = System.currentTimeMillis() - activeEntry.startTime.time
            projectDao.updateTimeEntry(
                activeEntry.copy(
                    endTime = Date(),
                    durationMinutes = duration / 60000,
                    isActive = false
                )
            )
        }
        
        // Start new time entry
        val timeEntry = TimeEntry(
            projectId = projectId,
            stepId = stepId,
            startTime = Date(),
            isActive = true
        )
        return projectDao.insertTimeEntry(timeEntry)
    }
    
    suspend fun stopTimeEntry(): TimeEntry? {
        val activeEntry = getActiveTimeEntry()
        return activeEntry?.let { entry ->
            val endTime = Date()
            val duration = (endTime.time - entry.startTime.time) / 60000
            val updatedEntry = entry.copy(
                endTime = endTime,
                durationMinutes = duration,
                isActive = false
            )
            projectDao.updateTimeEntry(updatedEntry)
            updatedEntry
        }
    }
    
    private suspend fun getActiveTimeEntry(): TimeEntry? = projectDao.getActiveTimeEntry()
    
    // Templates
    fun getAllTemplates(): Flow<List<ProjectTemplate>> = templateDao.getAllTemplates()
    
    suspend fun createProjectFromTemplate(template: ProjectTemplate): Long {
        val project = Project(
            name = template.name,
            description = template.description,
            category = template.category,
            status = ProjectStatus.PLANNING,
            createdAt = Date(),
            updatedAt = Date(),
            difficulty = template.difficulty,
            estimatedTimeHours = template.estimatedTimeHours
        )
        
        val projectId = insertProject(project)
        
        // Copy template steps to project steps
        templateDao.getTemplateSteps(template.id).collect { templateSteps ->
            templateSteps.forEach { templateStep ->
                val projectStep = ProjectStep(
                    projectId = projectId,
                    title = templateStep.title,
                    description = templateStep.description,
                    orderIndex = templateStep.orderIndex,
                    estimatedTimeMinutes = templateStep.estimatedTimeMinutes
                )
                insertProjectStep(projectStep)
            }
        }
        
        return projectId
    }
}