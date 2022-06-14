package com.example.thesis

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class EventAdapter(private val context: Context, private var events: ArrayList<Event>): RecyclerView.Adapter<EventAdapter.EventViewHolder>(){

    inner class EventViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var startTV: TextView = itemView.findViewById(R.id.time_start)
        var endTV: TextView = itemView.findViewById(R.id.time_end)
        var nameTV: TextView = itemView.findViewById(R.id.event_name)
        var editIV: ImageView = itemView.findViewById(R.id.image_edit)
        var deleteIV: ImageView = itemView.findViewById(R.id.image_delete)
        var layoutLO: ConstraintLayout = itemView.findViewById(R.id.event_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.event_cell,
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: EventAdapter.EventViewHolder, position: Int) {
        val event = events[position]

        val eventStart = holder.startTV
        val eventEnd = holder.endTV
        val eventName = holder.nameTV

        eventStart.text = DateTimeFormatter.ofPattern("HH:mm").format(event.start)
        eventEnd.text = DateTimeFormatter.ofPattern("HH:mm").format(event.end)
        eventName.text = event.name
        if (event.study) {
            holder.layoutLO.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.pale_purple
                )
            )
        }

        val eventEdit = holder.editIV
        val eventDelete = holder.deleteIV

        eventEdit.setOnClickListener { view ->
            if (context is MainActivity) {
                context.updateEventDialog(event)
            }
        }

        eventDelete.setOnClickListener { view ->
            if (context is MainActivity) {
                context.deleteEventAlertDialog(event)
            }
        }
    }

    override fun getItemCount(): Int {
        return events.size
    }

}