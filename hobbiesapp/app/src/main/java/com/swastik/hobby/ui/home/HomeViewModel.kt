package com.swastik.hobby.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.swastik.hobby.data.entity.Project
import com.swastik.hobby.data.entity.ProjectStatus
import com.swastik.hobby.data.repository.HobbyRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HobbyRepository) : ViewModel() {

    val activeProjects = repository.getProjectsByStatus(ProjectStatus.IN_PROGRESS).asLiveData()
    val completedProjects = repository.getProjectsByStatus(ProjectStatus.COMPLETED).asLiveData()
    val lowStockMaterials = repository.getLowStockMaterials().asLiveData()

    private val _navigateToProject = MutableLiveData<Long?>()
    val navigateToProject: LiveData<Long?> = _navigateToProject

    fun onProjectClicked(project: Project) {
        _navigateToProject.value = project.id
    }

    fun onProjectNavigated() {
        _navigateToProject.value = null
    }

    fun startTimer(project: Project) {
        viewModelScope.launch {
            repository.startTimeEntry(project.id)
        }
    }
}