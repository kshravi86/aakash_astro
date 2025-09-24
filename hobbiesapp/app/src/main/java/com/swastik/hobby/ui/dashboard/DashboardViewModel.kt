package com.swastik.hobby.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.swastik.hobby.data.entity.Material
import com.swastik.hobby.data.repository.HobbyRepository
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: HobbyRepository) : ViewModel() {

    val allMaterials = repository.getAllMaterials().asLiveData()
    val lowStockMaterials = repository.getLowStockMaterials().asLiveData()

    private val _currentTab = MutableLiveData<Int>().apply { value = 0 }
    val currentTab: LiveData<Int> = _currentTab

    private val _searchQuery = MutableLiveData<String>().apply { value = "" }
    val searchQuery: LiveData<String> = _searchQuery

    fun setCurrentTab(position: Int) {
        _currentTab.value = position
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addStock(material: Material, quantity: Double) {
        viewModelScope.launch {
            repository.updateMaterial(
                material.copy(currentQuantity = material.currentQuantity + quantity)
            )
        }
    }

    fun removeStock(material: Material, quantity: Double) {
        viewModelScope.launch {
            val newQuantity = (material.currentQuantity - quantity).coerceAtLeast(0.0)
            repository.updateMaterial(
                material.copy(currentQuantity = newQuantity)
            )
        }
    }
}