package com.aakash.astro.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.aakash.astro.R

/**
 * Immutable definition of a dashboard tile along with its presentation metadata.
 */
data class ActionTile(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val accentColor: Int,
    val category: String
)

/**
 * Heterogeneous list entries so headers and tiles can share one adapter.
 */
sealed class ActionGridItem {
    data class Header(val title: String) : ActionGridItem()
    data class Tile(val tile: ActionTile) : ActionGridItem()
}

/**
 * Adapter that shows grouped headers + actionable tiles inside the dashboard grid.
 */
class ActionTileAdapter(
    private val items: List<ActionGridItem>,
    private val onClick: (ActionTile) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TILE = 1
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ActionGridItem.Header -> TYPE_HEADER
        is ActionGridItem.Tile -> TYPE_TILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_action_header, parent, false)
            HeaderVH(view)
        } else {
            val view = inflater.inflate(R.layout.item_action_tile, parent, false)
            TileVH(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = items[position]) {
            is ActionGridItem.Header -> (holder as HeaderVH).bind(entry.title)
            is ActionGridItem.Tile -> (holder as TileVH).bind(entry.tile, onClick)
        }
    }

    class TileVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val categoryBadge: TextView = itemView.findViewById(R.id.categoryBadge)
        private val icon: ImageView = itemView.findViewById(R.id.icon)

        fun bind(tile: ActionTile, onClick: (ActionTile) -> Unit) {
            title.text = tile.title
            subtitle.text = tile.subtitle
            categoryBadge.text = tile.category
            icon.setImageResource(tile.iconRes)

            val accent = ContextCompat.getColor(itemView.context, tile.accentColor)
            DrawableCompat.setTint(icon.drawable, accent)
            val bg = DrawableCompat.wrap(
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_circle_orange)!!
            )
            DrawableCompat.setTint(bg, ColorUtils.setAlphaComponent(accent, 56))
            icon.background = bg

            itemView.setOnClickListener { onClick(tile) }
        }
    }

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTitle: TextView = itemView.findViewById(R.id.headerTitle)
        fun bind(title: String) {
            headerTitle.text = title
        }
    }
}
