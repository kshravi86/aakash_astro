package com.swastik.hobby.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.swastik.hobby.data.database.HobbyDatabase
import com.swastik.hobby.data.entity.Material
import com.swastik.hobby.data.repository.HobbyRepository
import com.swastik.hobby.databinding.FragmentDashboardBinding
import com.swastik.hobby.ui.adapter.MaterialAdapter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var materialAdapter: MaterialAdapter
    private var currentMaterials = emptyList<Material>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        
        // Initialize database and repository
        val database = HobbyDatabase.getDatabase(requireContext())
        val repository = HobbyRepository(
            database.projectDao(),
            database.materialDao(),
            database.templateDao()
        )
        
        // Create ViewModel with repository
        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(repository)
        )[DashboardViewModel::class.java]
        
        setupRecyclerView()
        setupTabs()
        setupSearch()
        setupObservers()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        materialAdapter = MaterialAdapter(
            onMaterialClick = { material ->
                // TODO: Navigate to material detail screen
            },
            onAddStock = { material ->
                showQuantityDialog(material, true)
            },
            onRemoveStock = { material ->
                showQuantityDialog(material, false)
            }
        )
        
        binding.recyclerMaterials.apply {
            adapter = materialAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    dashboardViewModel.setCurrentTab(it.position)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                dashboardViewModel.setSearchQuery(s.toString())
            }
        })
    }

    private fun setupObservers() {
        // Observe current tab and filter materials accordingly
        dashboardViewModel.currentTab.observe(viewLifecycleOwner) { tab ->
            filterMaterials(tab)
        }
        
        dashboardViewModel.allMaterials.observe(viewLifecycleOwner) { materials ->
            currentMaterials = materials
            filterMaterials(dashboardViewModel.currentTab.value ?: 0)
        }
        
        dashboardViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterMaterials(dashboardViewModel.currentTab.value ?: 0, query)
        }
        
        binding.btnAddMaterial.setOnClickListener {
            // TODO: Navigate to add material screen
        }
    }

    private fun filterMaterials(tab: Int, query: String = dashboardViewModel.searchQuery.value ?: "") {
        var filteredMaterials = when (tab) {
            0 -> currentMaterials // All materials
            1 -> currentMaterials.filter { it.currentQuantity <= it.minQuantity } // Low stock
            2 -> currentMaterials.groupBy { it.category }.keys.toList().let { categories ->
                // For categories tab, show one material per category
                categories.mapNotNull { category ->
                    currentMaterials.find { it.category == category }
                }
            }
            else -> currentMaterials
        }

        if (query.isNotBlank()) {
            filteredMaterials = filteredMaterials.filter { material ->
                material.name.contains(query, ignoreCase = true) ||
                material.category.contains(query, ignoreCase = true)
            }
        }

        materialAdapter.submitList(filteredMaterials)
        binding.emptyState.visibility = if (filteredMaterials.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showQuantityDialog(material: Material, isAdding: Boolean) {
        val title = if (isAdding) "Add Stock" else "Remove Stock"
        val hint = "Enter quantity to ${if (isAdding) "add" else "remove"}"
        
        val editText = EditText(requireContext()).apply {
            setHint(hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("${material.name} (${material.unit})")
            .setView(editText)
            .setPositiveButton("Confirm") { _, _ ->
                val quantity = editText.text.toString().toDoubleOrNull()
                if (quantity != null && quantity > 0) {
                    if (isAdding) {
                        dashboardViewModel.addStock(material, quantity)
                    } else {
                        dashboardViewModel.removeStock(material, quantity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}