package com.example.autodingding

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LogActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private var filterLevel: String? = null // null = ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler = findViewById(R.id.recycler_log)
        tvEmpty = findViewById(R.id.tv_empty_log)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LogAdapter(emptyList())

        setupFilterChips()
        refreshList()
    }

    private fun setupFilterChips() {
        val chips = listOf(
            findViewById<TextView>(R.id.tv_filter_all) to null,
            findViewById<TextView>(R.id.tv_filter_error) to "E",
            findViewById<TextView>(R.id.tv_filter_warn) to "W",
            findViewById<TextView>(R.id.tv_filter_info) to "I",
            findViewById<TextView>(R.id.tv_filter_debug) to "D"
        )

        for ((view, level) in chips) {
            view.setOnClickListener {
                filterLevel = level
                refreshList()
                updateChipStyles(chips, view)
            }
        }
    }

    private fun updateChipStyles(chips: List<Pair<TextView, String?>>, selected: TextView) {
        for ((view, _) in chips) {
            if (view == selected) {
                view.setTextColor(getColor(R.color.white))
                view.background = getDrawable(R.drawable.badge_enabled_bg)
            } else {
                view.setTextColor(getColor(R.color.teal_700))
                view.background = getDrawable(R.drawable.badge_disabled_bg)
            }
        }
    }

    private fun refreshList() {
        val all = LogManager.getAll()
        val filtered = if (filterLevel == null) {
            all
        } else {
            all.filter { it.level == filterLevel }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
            recycler.adapter = LogAdapter(filtered.reversed())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private inner class LogAdapter(
        private val items: List<LogManager.LogEntry>
    ) : RecyclerView.Adapter<LogAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val tvMeta: TextView = view.findViewById(R.id.tv_log_meta)
            val tvMessage: TextView = view.findViewById(R.id.tv_log_message)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log_entry, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = items[position]
            val ctx = holder.itemView.context

            holder.tvMeta.text = "${entry.formattedTime()}  ${entry.shortLabel()}/${entry.tag}"
            holder.tvMeta.setTextColor(when (entry.level) {
                "E" -> Color.parseColor("#D32F2F")
                "W" -> Color.parseColor("#F57F17")
                "I" -> Color.parseColor("#1976D2")
                "D" -> Color.parseColor("#616161")
                else -> ctx.getColor(R.color.text_secondary)
            })

            holder.tvMessage.text = entry.message
        }

        override fun getItemCount(): Int = items.size
    }
}
