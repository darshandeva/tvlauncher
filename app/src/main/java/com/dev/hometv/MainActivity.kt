package com.dev.hometv

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var favRow: RecyclerView
    private lateinit var appsRow: RecyclerView
    private lateinit var hint: TextView
    private lateinit var favAdapter: TileAdapter
    private lateinit var appsAdapter: TileAdapter

    private var all: List<AppEntry> = emptyList()
    private val pinned = ArrayList<String>()
    private var firstLoad = true
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("home", MODE_PRIVATE)

        favRow = findViewById(R.id.favRow)
        appsRow = findViewById(R.id.appsRow)
        hint = findViewById(R.id.hint)

        favAdapter = TileAdapter(184, 104, true, ::open) { togglePin(it, true) }
        appsAdapter = TileAdapter(88, 88, false, ::open) { togglePin(it, false) }

        for ((row, adapter) in listOf(favRow to favAdapter, appsRow to appsAdapter)) {
            row.layoutManager = CenterLayoutManager(this)
            row.adapter = adapter
            row.itemAnimator = null
            row.setHasFixedSize(true)
            row.setItemViewCacheSize(6)
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true) {
            favRow.scrollToPosition(0)
            appsRow.scrollToPosition(0)
            focusAt(if (favAdapter.itemCount > 0) favRow else appsRow, 0)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* a launcher has nowhere to go back to */ }

    private fun reload() {
        if (loading) return
        loading = true
        Thread {
            val list = queryApps()
            runOnUiThread {
                loading = false
                if (isDestroyed) return@runOnUiThread
                val changed = list.map { it.pkg } != all.map { it.pkg }
                if (!changed && !firstLoad) return@runOnUiThread
                all = list
                if (firstLoad) initPinned(list)
                appsAdapter.submit(all)
                favAdapter.submit(favList())
                updateHint()
                if (firstLoad) {
                    firstLoad = false
                    focusAt(if (favAdapter.itemCount > 0) favRow else appsRow, 0)
                }
            }
        }.start()
    }

    private fun queryApps(): List<AppEntry> {
        val pm = packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (cat in arrayOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == packageName || !seen.add(ai.packageName)) continue
                out.add(
                    AppEntry(
                        ai.packageName,
                        ComponentName(ai.packageName, ai.name),
                        ri.loadLabel(pm).toString(),
                        ri.loadIcon(pm),
                        ai.loadBanner(pm)
                    )
                )
            }
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    private fun initPinned(list: List<AppEntry>) {
        val saved = prefs.getString("pinned", null)
        pinned.clear()
        if (saved != null) {
            pinned.addAll(saved.split("\n").filter { it.isNotEmpty() })
            return
        }
        val have = list.map { it.pkg }.toSet()
        for (p in DEFAULT_APPS) if (p in have) pinned.add(p)
        for (e in list) {
            if (pinned.size >= 4) break
            if (e.pkg !in pinned) pinned.add(e.pkg)
        }
        SETTINGS_PKGS.firstOrNull { it in have && it !in pinned }?.let { pinned.add(it) }
        savePinned()
    }

    private fun savePinned() {
        prefs.edit().putString("pinned", pinned.joinToString("\n")).apply()
    }

    private fun favList(): List<AppEntry> {
        val map = all.associateBy { it.pkg }
        return pinned.mapNotNull { map[it] }
    }

    private fun updateHint() {
        hint.visibility = if (favAdapter.itemCount == 0) TextView.VISIBLE else TextView.GONE
    }

    private fun togglePin(e: AppEntry, fromFav: Boolean) {
        val idx = pinned.indexOf(e.pkg)
        if (idx >= 0) {
            pinned.removeAt(idx)
            toast("Removed from Favorites")
        } else {
            pinned.add(e.pkg)
            toast("Added to Favorites")
        }
        savePinned()
        favAdapter.submit(favList())
        updateHint()
        if (fromFav && idx >= 0) {
            if (favAdapter.itemCount > 0) {
                focusAt(favRow, idx.coerceAtMost(favAdapter.itemCount - 1))
            } else {
                focusAt(appsRow, 0)
            }
        }
    }

    private fun open(e: AppEntry) {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(e.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            )
        } catch (x: Exception) {
            toast("Can't open ${e.label}")
        }
    }

    private fun focusAt(rv: RecyclerView, pos: Int) {
        val vh = rv.findViewHolderForAdapterPosition(pos)
        if (vh != null) {
            vh.itemView.requestFocus()
            return
        }
        rv.scrollToPosition(pos)
        rv.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                rv.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
            }
        })
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private val DEFAULT_APPS = listOf(
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.spotify.tv.android",
            "org.videolan.vlc"
        )
        private val SETTINGS_PKGS = listOf("com.android.tv.settings", "com.android.settings")
    }
}
