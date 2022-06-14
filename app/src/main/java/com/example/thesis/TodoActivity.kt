package com.example.thesis

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList

class TodoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        setTasks()
    }

    private fun getTasks(): ArrayList<Task> {

        val databaseHandler = TaskDatabaseHandler(this)
        var tasks = databaseHandler.viewTasks()
        tasks.sortBy { it.deadline }
        return tasks
    }

    private fun setTasks() {
        val taskView = findViewById<RecyclerView>(R.id.recyclerTasks)
        val taskList = getTasks()

        taskView.layoutManager = LinearLayoutManager(this)
        taskView.adapter = TaskAdapter(this, taskList)
    }

    fun MainAction(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun updateTaskDialog(task: Task) {
        val updateDialog = Dialog(this)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.task_edit)

        val nameView = updateDialog.findViewById<EditText>(R.id.task_name_edit)
        val deadlineView = updateDialog.findViewById<TextView>(R.id.task_deadlineTV)
        val durationView = updateDialog.findViewById<EditText>(R.id.task_duration_edit)
        val descriptionView = updateDialog.findViewById<EditText>(R.id.task_description_edit)
        val startView = updateDialog.findViewById<TextView>(R.id.task_startTV)

        val nonDiv = updateDialog.findViewById<CheckBox>(R.id.task_nonDiv_cb)
        val nonEarly = updateDialog.findViewById<CheckBox>(R.id.task_nonEarly_cb)

        val name = nameView.setText(task.name)
        val duration = durationView.setText(task.duration.toString())
        val description = descriptionView.setText(task.description)

        var deadline = task.deadline
        var start = task.start


        nonDiv.isChecked = task.nonDiv
        nonEarly.isChecked = task.nonEarly

        val calendar = Calendar.getInstance()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        var currentDateTime = LocalDateTime.now()

        deadlineView.text = formatter.format(task.deadline)
        startView.text = formatter.format(task.start)


        var changing_dl = false

        fun calendar_date(): LocalDateTime? {
            val year: Int = calendar.get(Calendar.YEAR)
            val month: Int = calendar.get(Calendar.MONTH)
            val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
            val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
            val minute: Int = calendar.get(Calendar.MINUTE)
            return LocalDateTime.of(year, month, day, hour, minute)
        }

        val timePicker = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            currentDateTime = calendar_date()
            currentDateTime = currentDateTime.plusMonths(1)

            if (changing_dl) {
                deadline = currentDateTime
                deadlineView.text = formatter.format(currentDateTime)
            } else {
                start = currentDateTime
                startView.text = formatter.format(currentDateTime)
            }
            changing_dl = false
        }

        val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            currentDateTime = calendar_date()
            currentDateTime = currentDateTime.plusMonths(1)

            TimePickerDialog(
                this,
                timePicker,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        deadlineView.setOnClickListener {
            changing_dl = true
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        startView.setOnClickListener {
            changing_dl = false
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        updateDialog.findViewById<Button>(R.id.bt_task_save).setOnClickListener(View.OnClickListener {
            val name = nameView.text.toString()
            val duration = durationView.text.toString()
            val description = descriptionView.text.toString()

            val databaseHandler: TaskDatabaseHandler = TaskDatabaseHandler(this)

            if (name.isNotEmpty()) {

                var task = Task(task.id, name, deadline, nonDiv = nonDiv.isChecked,
                    nonEarly = nonEarly.isChecked, start = start)
                if (duration.isNotEmpty()) task.duration = duration.toDouble()
                if (description.isNotEmpty()) task.description = description

                val status = databaseHandler.updateTask(task)

                if (status > -1) {
                    Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_LONG).show()
                    nameView.text.clear()
                    durationView.text.clear()
                    descriptionView.text.clear()

                    setTasks()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name and deadline cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        updateDialog.findViewById<Button>(R.id.bt_task_cancel).setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })
        updateDialog.show()
    }



    fun addTaskDialog(view: View) {
        val updateDialog = Dialog(this)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.task_edit)

        val nameView = updateDialog.findViewById<EditText>(R.id.task_name_edit)
        val deadlineView = updateDialog.findViewById<TextView>(R.id.task_deadlineTV)
        val durationView = updateDialog.findViewById<EditText>(R.id.task_duration_edit)
        val descriptionView = updateDialog.findViewById<EditText>(R.id.task_description_edit)
        val startView = updateDialog.findViewById<TextView>(R.id.task_startTV)

        val nonDiv = updateDialog.findViewById<CheckBox>(R.id.task_nonDiv_cb)
        val nonEarly = updateDialog.findViewById<CheckBox>(R.id.task_nonEarly_cb)

        val calendar = Calendar.getInstance()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        var currentDateTime = LocalDateTime.now()

        var deadline: LocalDateTime = currentDateTime.plusDays(1)
        var start = currentDateTime

        var changing_dl = false

        fun calendar_date(): LocalDateTime? {
            val year: Int = calendar.get(Calendar.YEAR)
            val month: Int = calendar.get(Calendar.MONTH)
            val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
            val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
            val minute: Int = calendar.get(Calendar.MINUTE)
            return LocalDateTime.of(year, month, day, hour, minute)
        }

        val timePicker = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            currentDateTime = calendar_date()
            currentDateTime = currentDateTime.plusMonths(1)

            if (changing_dl) {
                deadline = currentDateTime
                deadlineView.text = formatter.format(currentDateTime)
            } else {
                start = currentDateTime
                startView.text = formatter.format(currentDateTime)
            }
            changing_dl = false
        }

        val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            currentDateTime = calendar_date()
            currentDateTime = currentDateTime.plusMonths(1)

            TimePickerDialog(
                this,
                timePicker,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        deadlineView.setOnClickListener {
            changing_dl = true
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        startView.setOnClickListener {
            changing_dl = false
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        updateDialog.findViewById<Button>(R.id.bt_task_save).setOnClickListener(View.OnClickListener {
            val name = nameView.text.toString()
            val duration = durationView.text.toString()
            val description = descriptionView.text.toString()

            val databaseHandler: TaskDatabaseHandler = TaskDatabaseHandler(this)

            if (name.isNotEmpty()) {

                var task = Task(0, name, deadline, nonDiv = nonDiv.isChecked,
                    nonEarly = nonEarly.isChecked, start = start)
                if (duration.isNotEmpty()) task.duration = duration.toDouble()
                if (description.isNotEmpty()) task.description = description

                val status = databaseHandler.addTask(task)

                if (status > -1) {
                    Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_LONG).show()
                    nameView.text.clear()
                    durationView.text.clear()
                    descriptionView.text.clear()

                    setTasks()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name and deadline cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        updateDialog.findViewById<Button>(R.id.bt_task_cancel).setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })
        updateDialog.show()
    }

    fun schedule(view: android.view.View) {
        makeTimetable(this)
        MainAction(view)

    }

    fun deleteTaskAlertDialog(task: Task) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you wants to delete ${task.name}.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes") { dialogInterface, which ->

            val databaseHandler: TaskDatabaseHandler = TaskDatabaseHandler(this)
            val status = databaseHandler.deleteTask(task)
            if (status > -1) {
                Toast.makeText(
                    applicationContext,
                    "Record deleted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                setTasks()
            }

            dialogInterface.dismiss()
        }
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}