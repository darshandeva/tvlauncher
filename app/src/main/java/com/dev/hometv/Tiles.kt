package com.dev.hometv

import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppEntry(
    val pkg: String,
    val component: ComponentName,
    val label: String,
    val icon: Drawable,
    val banner: Drawable?
)

/** Keeps the focused tile centered while the row scrolls, like tvOS. */
class CenterLayoutManager(context: Context) :
    LinearLayoutManager(context, HORIZONTAL, false) {

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean,
        focusedChildVisible: Boolean
    ): Boolean {
        val dx = (child.left + child.right) / 2 - parent.width / 2
        if (dx != 0) {
            if (immediate) parent.scrollBy(dx, 0) else parent.smoothScrollBy(dx, 0)
        }
        return true
    }
}

class TileAdapter(
    private val cardWdp: Int,
    private val cardHdp: Int,
    private val large: Boolean,
    private val onOpen: (AppEntry) -> Unit,
    private val onPin: (AppEntry) -> Unit
) : RecyclerView.Adapter<TileAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: FrameLayout = v.findViewById(R.id.card)
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val v = LayoutInflater.from(ctx).inflate(R.layout.item_tile, parent, false)
        val m = (10 * d).toInt()
        v.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(m, 0, m, 0) }

        val vh = VH(v)
        val w = (cardWdp * d).toInt()
        vh.card.layoutParams = LinearLayout.LayoutParams(w, (cardHdp * d).toInt())
        vh.label.layoutParams = LinearLayout.LayoutParams(
            w, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (10 * d).toInt() }
        vh.card.clipToOutline = true
        if (Build.VERSION.SDK_INT >= 26) v.defaultFocusHighlightEnabled = false

        v.setOnFocusChangeListener { view, has ->
            val s = if (has) 1.12f else 1f
            view.animate().scaleX(s).scaleY(s).setDuration(140).start()
            vh.label.animate().alpha(if (has) 1f else 0f).setDuration(140).start()
        }
        return vh
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        val d = h.itemView.resources.displayMetrics.density
        val banner = if (large) e.banner else null
        if (banner != null) {
            h.icon.scaleType = ImageView.ScaleType.CENTER_CROP
            h.icon.setPadding(0, 0, 0, 0)
            h.icon.setImageDrawable(fresh(banner))
        } else {
            val pad = ((if (large) 24 else 16) * d).toInt()
            h.icon.scaleType = ImageView.ScaleType.FIT_CENTER
            h.icon.setPadding(pad, pad, pad, pad)
            h.icon.setImageDrawable(fresh(e.icon))
        }
        h.label.text = e.label
        if (!h.itemView.hasFocus()) {
            h.itemView.scaleX = 1f
            h.itemView.scaleY = 1f
            h.label.alpha = 0f
        }
        h.itemView.setOnClickListener { onOpen(e) }
        h.itemView.setOnLongClickListener { onPin(e); true }
    }

    // The same Drawable can't safely back two ImageViews (favorites + apps rows).
    private fun fresh(d: Drawable): Drawable = d.constantState?.newDrawable()?.mutate() ?: d
}
