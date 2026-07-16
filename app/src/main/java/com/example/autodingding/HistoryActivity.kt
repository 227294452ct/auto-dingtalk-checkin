package com.example.autodingding

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recycler = findViewById<RecyclerView>(R.id.recycler_history)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty_history)

        val records = CheckInRecordManager.getAll(this)

        if (records.isEmpty()) {
            tvEmpty.visibility = android.view.View.VISIBLE
            recycler.visibility = android.view.View.GONE
        } else {
            tvEmpty.visibility = android.view.View.GONE
            recycler.visibility = android.view.View.VISIBLE
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = CheckInRecordAdapter(records)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}