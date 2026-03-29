package com.aakash.astro.ui

import android.content.res.ColorStateList
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
import com.google.android.material.card.MaterialCardView

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
    data class Header(
        val title: String,
        val subtitle: String,
        val count: Int,
        @ColorRes val accentColor: Int
    ) : ActionGridItem()

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
            is ActionGridItem.Header -> (holder as HeaderVH).bind(entry)
            is ActionGridItem.Tile -> (holder as TileVH).bind(entry.tile, onClick)
        }
    }

    class TileVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tileCard: MaterialCardView = itemView.findViewById(R.id.tileCard)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val categoryBadge: TextView = itemView.findViewById(R.id.categoryBadge)
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val tileGlow: View = itemView.findViewById(R.id.tileGlow)
        private val openLabel: TextView = itemView.findViewById(R.id.openLabel)

        fun bind(tile: ActionTile, onClick: (ActionTile) -> Unit) {
            title.text = tile.title
            subtitle.text = tile.subtitle
            categoryBadge.text = tile.category
            icon.setImageResource(tile.iconRes)

            val accent = ContextCompat.getColor(itemView.context, tile.accentColor)
            tileCard.strokeColor = ColorUtils.setAlphaComponent(accent, 76)
            tileGlow.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 110))

            DrawableCompat.setTint(icon.drawable, accent)
            val bg = DrawableCompat.wrap(
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_circle_orange)!!.mutate()
            )
            DrawableCompat.setTint(bg, ColorUtils.setAlphaComponent(accent, 56))
            icon.background = bg

            categoryBadge.setTextColor(accent)
            categoryBadge.background = DrawableCompat.wrap(categoryBadge.background.mutate()).also {
                DrawableCompat.setTint(it, ColorUtils.setAlphaComponent(accent, 32))
            }

            openLabel.setTextColor(accent)
            openLabel.background = DrawableCompat.wrap(openLabel.background.mutate()).also {
                DrawableCompat.setTint(it, ColorUtils.setAlphaComponent(accent, 28))
            }

            itemView.setOnClickListener { onClick(tile) }
        }
    }

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTitle: TextView = itemView.findViewById(R.id.headerTitle)
        private val headerSummary: TextView = itemView.findViewById(R.id.headerSummary)
        private val headerCount: TextView = itemView.findViewById(R.id.headerCount)
        private val headerAccent: View = itemView.findViewById(R.id.headerAccent)

        fun bind(header: ActionGridItem.Header) {
            val accent = ContextCompat.getColor(itemView.context, header.accentColor)
            headerTitle.text = header.title
            headerTitle.setTextColor(accent)
            headerSummary.text = header.subtitle
            headerCount.text = itemView.context.getString(R.string.category_count_format, header.count)
            headerCount.setTextColor(accent)
            headerCount.background = DrawableCompat.wrap(headerCount.background.mutate()).also {
                DrawableCompat.setTint(it, ColorUtils.setAlphaComponent(accent, 28))
            }
            headerAccent.backgroundTintList = ColorStateList.valueOf(accent)
        }
    }
}
