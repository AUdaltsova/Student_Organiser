package com.example.thesis

import android.content.Context
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TaskAdapter(private val context: Context, private var tasks: ArrayList<Task>): RecyclerView.Adapter<TaskAdapter.TaskViewHolder>(){

    inner class TaskViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var deadlineTV: TextView = itemView.findViewById(R.id.task_deadline)
        var nameTV: TextView = itemView.findViewById(R.id.task_name)
        var descriptionTV: TextView = itemView.findViewById(R.id.task_description)
        var doneCB: CheckBox = itemView.findViewById(R.id.task_done)
        var editIV: ImageView = itemView.findViewById(R.id.image_edit_task)
        var deleteIV: ImageView = itemView.findViewById(R.id.image_delete_task)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.task_cell,
                parent,
                false
            ))
    }

    private fun strikeThrough(tvTask: TextView, isChecked: Boolean) {
        if(isChecked) {
            tvTask.paintFlags = tvTask.paintFlags or STRIKE_THRU_TEXT_FLAG
        } else {
            tvTask.paintFlags = tvTask.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        val taskName = holder.nameTV
        val taskDeadline = holder.deadlineTV
        val taskDescription = holder.descriptionTV
        val taskDone = holder.doneCB

        taskName.text = task.name
        taskDeadline.text = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(task.deadline)
        taskDescription.text = task.description
        taskDone.isChecked = task.done

        val databaseHandler: TaskDatabaseHandler = TaskDatabaseHandler(context)

        strikeThrough(taskName, task.done)
        taskDone.setOnCheckedChangeListener { _, done ->
            strikeThrough(taskName, done)
            task.done = !task.done
            databaseHandler.updateTask(task)
        }

        val eventEdit = holder.editIV
        val eventDelete = holder.deleteIV

        eventEdit.setOnClickListener { view ->
            if (context is TodoActivity) {
                context.updateTaskDialog(task)
            }
        }

        eventDelete.setOnClickListener { view ->
            if (context is TodoActivity) {
                context.deleteTaskAlertDialog(task)
            }
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

}