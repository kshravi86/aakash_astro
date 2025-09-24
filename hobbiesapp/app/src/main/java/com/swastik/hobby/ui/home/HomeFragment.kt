package com.swastik.hobby.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.swastik.hobby.data.database.HobbyDatabase
import com.swastik.hobby.data.repository.HobbyRepository
import com.swastik.hobby.databinding.FragmentHomeBinding
import com.swastik.hobby.ui.adapter.ProjectAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        // Initialize database and repository
        val database = HobbyDatabase.getDatabase(requireContext())
        val repository = HobbyRepository(
            database.projectDao(),
            database.materialDao(),
            database.templateDao()
        )
        
        // Create ViewModel with repository
        homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(repository)
        )[HomeViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        projectAdapter = ProjectAdapter(
            onProjectClick = { project ->
                homeViewModel.onProjectClicked(project)
            },
            onStartTimer = { project ->
                homeViewModel.startTimer(project)
            }
        )
        
        binding.recyclerActiveProjects.apply {
            adapter = projectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        homeViewModel.activeProjects.observe(viewLifecycleOwner) { projects ->
            projectAdapter.submitList(projects)
            binding.emptyState.visibility = if (projects.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.textActiveProjects.text = projects.size.toString()
        }
        
        homeViewModel.completedProjects.observe(viewLifecycleOwner) { projects ->
            binding.textCompletedProjects.text = projects.size.toString()
        }
        
        homeViewModel.lowStockMaterials.observe(viewLifecycleOwner) { materials ->
            binding.textLowStockItems.text = materials.size.toString()
        }
        
        binding.btnAddProject.setOnClickListener {
            // TODO: Navigate to create project screen
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}