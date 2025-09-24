package com.swastik.hobby.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swastik.hobby.data.entity.Project
import com.swastik.hobby.data.entity.ProjectStatus
import com.swastik.hobby.databinding.ItemProjectBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectAdapter(
    private val onProjectClick: (Project) -> Unit,
    private val onStartTimer: (Project) -> Unit
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.apply {
                textProjectName.text = project.name
                textProjectCategory.text = project.category
                textProjectStatus.text = project.status.name.replace("_", " ")
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textProjectDate.text = "Updated: ${dateFormat.format(project.updatedAt)}"
                
                // Set status color
                val statusColor = when (project.status) {
                    ProjectStatus.PLANNING -> android.R.color.holo_blue_light
                    ProjectStatus.IN_PROGRESS -> android.R.color.holo_orange_light
                    ProjectStatus.ON_HOLD -> android.R.color.darker_gray
                    ProjectStatus.COMPLETED -> android.R.color.holo_green_light
                    ProjectStatus.CANCELLED -> android.R.color.holo_red_light
                }
                textProjectStatus.setTextColor(
                    binding.root.context.getColor(statusColor)
                )
                
                // Progress indicator
                val progress = if (project.estimatedTimeHours != null && project.estimatedTimeHours > 0) {
                    val actualHours = project.actualTimeMinutes / 60.0
                    ((actualHours / project.estimatedTimeHours) * 100).toInt().coerceAtMost(100)
                } else {
                    0
                }
                progressProject.progress = progress
                textProgress.text = "$progress% Complete"
                
                // Click listeners
                root.setOnClickListener { onProjectClick(project) }
                btnStartTimer.setOnClickListener { onStartTimer(project) }
                
                // Show/hide timer button based on status
                btnStartTimer.visibility = if (project.status == ProjectStatus.IN_PROGRESS) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem == newItem
        }
    }
}