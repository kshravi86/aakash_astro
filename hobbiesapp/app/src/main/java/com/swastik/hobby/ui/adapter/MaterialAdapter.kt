package com.swastik.hobby.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.swastik.hobby.R
import com.swastik.hobby.data.entity.Material
import com.swastik.hobby.databinding.ItemMaterialBinding

class MaterialAdapter(
    private val onMaterialClick: (Material) -> Unit,
    private val onAddStock: (Material) -> Unit,
    private val onRemoveStock: (Material) -> Unit
) : ListAdapter<Material, MaterialAdapter.MaterialViewHolder>(MaterialDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding = ItemMaterialBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MaterialViewHolder(
        private val binding: ItemMaterialBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(material: Material) {
            binding.apply {
                textMaterialName.text = material.name
                textMaterialCategory.text = material.category
                textCurrentQuantity.text = material.currentQuantity.toString()
                textUnit.text = material.unit

                // Load material image if available
                if (!material.imagePath.isNullOrEmpty()) {
                    Glide.with(binding.root.context)
                        .load(material.imagePath)
                        .placeholder(R.drawable.ic_home_black_24dp)
                        .into(imageMaterial)
                } else {
                    imageMaterial.setImageResource(R.drawable.ic_home_black_24dp)
                }

                // Set stock status
                val isLowStock = material.currentQuantity <= material.minQuantity
                if (isLowStock) {
                    textStockStatus.text = "● Low Stock"
                    textStockStatus.setTextColor(
                        binding.root.context.getColor(android.R.color.holo_red_dark)
                    )
                    textStockStatus.visibility = View.VISIBLE
                } else {
                    val stockLevel = when {
                        material.maxQuantity != null && material.currentQuantity >= material.maxQuantity -> "● Full Stock"
                        material.currentQuantity > material.minQuantity * 2 -> "● Good Stock"
                        else -> "● Normal"
                    }
                    textStockStatus.text = stockLevel
                    textStockStatus.setTextColor(
                        binding.root.context.getColor(android.R.color.holo_green_dark)
                    )
                    textStockStatus.visibility = View.VISIBLE
                }

                // Click listeners
                root.setOnClickListener { onMaterialClick(material) }
                btnAddStock.setOnClickListener { onAddStock(material) }
                btnRemoveStock.setOnClickListener { onRemoveStock(material) }
            }
        }
    }

    private class MaterialDiffCallback : DiffUtil.ItemCallback<Material>() {
        override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
            return oldItem == newItem
        }
    }
}