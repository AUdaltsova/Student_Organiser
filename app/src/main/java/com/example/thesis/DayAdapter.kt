package com.example.thesis

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalDateTime


class DayAdapter(private val mcontext: Context,
                 private var week: List<LocalDate>,
                 private val onItemListener: OnItemListener):
    RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    inner class DayViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var dayTV: TextView = itemView.findViewById(R.id.date_tag)
        var weekdayTV: TextView = itemView.findViewById(R.id.weekday_tag)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            var now = week[adapterPosition]
            var newDate = LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, 0, 0)
            onItemListener.onItemClick(adapterPosition, newDate)
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(LayoutInflater.from(mcontext).inflate(R.layout.calendar_week_cell, parent, false))
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {

        val weekdayTextView = holder.dayTV
        val dateTextView = holder.weekdayTV

        dateTextView.text = weekdays[position]
        weekdayTextView.text = week[position].dayOfMonth.toString()

        if (CalendarUtils.selectedDate.dayOfMonth.toString() == week[position].dayOfMonth.toString()) {
            dateTextView.setTextColor(Color.parseColor("#FF3700B3"))
            weekdayTextView.setTextColor(Color.parseColor("#FF3700B3"))
        }


    }
    override fun getItemCount(): Int {
        return 7
    }

    public interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDateTime)
    }

}