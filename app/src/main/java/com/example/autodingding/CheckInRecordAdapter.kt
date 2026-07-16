package com.example.autodingding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CheckInRecordAdapter(
    private val records: List<CheckInRecordManager.CheckInRecord>
) : RecyclerView.Adapter<CheckInRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_record_date)
        val tvType: TextView = view.findViewById(R.id.tv_record_type)
        val tvTime: TextView = view.findViewById(R.id.tv_record_time)
        val tvResult: TextView = view.findViewById(R.id.tv_record_result)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_in_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val ctx = holder.itemView.context

        holder.tvDate.text = CheckInRecordManager.formatDate(record.date)
        holder.tvType.text = record.type
        holder.tvTime.text = CheckInRecordManager.formatTime(record.actualTime)

        if (record.success) {
            holder.tvResult.text = "\u6210\u529F"
            holder.tvResult.setTextColor(ContextCompat.getColor(ctx, R.color.teal_700))
        } else {
            holder.tvResult.text = "\u5931\u8D25"
            holder.tvResult.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount(): Int = records.size
}