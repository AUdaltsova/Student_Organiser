package com.example.thesis

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class MainActivity : AppCompatActivity(), DayAdapter.OnItemListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var now = LocalDateTime.now()
        CalendarUtils.selectedDate = LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, 0, 0)

        setDates()

    }

    fun getDates(date: LocalDateTime): List<LocalDate> {

        var weekday = date.dayOfWeek.value

        var monday: LocalDate = LocalDate.from(date.minusDays((weekday - 2).toLong()))

        var week = mutableListOf(monday.minusDays(1))

        for (i in 0..6) {
            week.add(monday.plusDays(i.toLong()))
        }

        return week
    }

    fun getEvents(context: Context): java.util.ArrayList<Event> {
        val databaseHandler = EventDatabaseHandler(context)

        var eventList = databaseHandler.viewEvents(CalendarUtils.selectedDate)

        eventList.sortBy { it.start }

        return eventList
    }

    private fun setDates(){
        var month: TextView = findViewById(R.id.Month)
        month.text = CalendarUtils.selectedDate.format(DateTimeFormatter.ofPattern("MMMM"))
        var week = getDates(CalendarUtils.selectedDate)

        val dayView = findViewById<RecyclerView>(R.id.recyclerDays)
        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(applicationContext, 7)
        dayView.layoutManager = layoutManager
        dayView.adapter = DayAdapter(this, week, this)

        setEvents()
    }


    private fun setEvents() {
        val eventView = findViewById<RecyclerView>(R.id.recyclerEvents)
        eventView.layoutManager = LinearLayoutManager(this)

        val eventList = getEvents(this)

        eventView.adapter = EventAdapter(this, eventList)
    }



    override fun onItemClick(position: Int, date: LocalDateTime) {
        CalendarUtils.selectedDate = date
        setDates()
    }


    fun nextWeek(view: View) {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.plusWeeks(1)
        setDates()
    }

    fun previousWeek(view: View) {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.minusWeeks(1)
        setDates()
    }

    fun updateEventDialog(event: Event) {
        val updateDialog = Dialog(this)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.event_edit)

        var nameView = updateDialog.findViewById<EditText>(R.id.event_name_edit)
        var startView = updateDialog.findViewById<TextView>(R.id.event_startTV)
        var endView = updateDialog.findViewById<TextView>(R.id.event_endTV)

        val name = nameView.setText(event.name)
        val calendar = Calendar.getInstance()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        var currentDateTime = LocalDateTime.now()

        startView.text = formatter.format(event.start)
        endView.text = formatter.format(event.end)

        var start = event.start
        var end = event.end

        var changing_end = false

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

            if (changing_end) {
                end = currentDateTime
                endView.text = formatter.format(currentDateTime)
            } else {
                start = currentDateTime
                startView.text = formatter.format(currentDateTime)
            }
            changing_end = false
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

        endView.setOnClickListener {
            changing_end = true
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        startView.setOnClickListener {
            changing_end = false
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        updateDialog.findViewById<Button>(R.id.bt_event_save).setOnClickListener(View.OnClickListener {
            val name = nameView.text.toString()
            var date = LocalDate.from(start)

            val databaseHandler: EventDatabaseHandler = EventDatabaseHandler(this)

            if (name.isNotEmpty()) {

                val event = Event(event.id, name, start, end, event.study)

                val status = databaseHandler.updateEvent(event)

                if (status > -1) {
                    Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_LONG).show()
                    nameView.text.clear()

                    setEvents()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name, Start time and End time cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        updateDialog.findViewById<Button>(R.id.bt_event_cancel).setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })
        updateDialog.show()
    }

    fun deleteEventAlertDialog(event: Event) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you wants to delete ${event.name}.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes") { dialogInterface, which ->

            val databaseHandler: EventDatabaseHandler = EventDatabaseHandler(this)
            val status = databaseHandler.deleteEvent(event)
            if (status > -1) {
                Toast.makeText(
                    applicationContext,
                    "Record deleted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                setEvents()
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

    fun addEventDialog(view: View) {
        val updateDialog = Dialog(this)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.event_edit)

        val nameView = updateDialog.findViewById<EditText>(R.id.event_name_edit)
        val startView = updateDialog.findViewById<TextView>(R.id.event_startTV)
        val endView = updateDialog.findViewById<TextView>(R.id.event_endTV)

        val calendar = Calendar.getInstance()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        var currentDateTime = LocalDateTime.now()


        var start = currentDateTime
        var end = currentDateTime.plusHours(1)

        var changing_end = false

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

            if (changing_end) {
                end = currentDateTime
                endView.text = formatter.format(currentDateTime)
            } else {
                start = currentDateTime
                startView.text = formatter.format(currentDateTime)
            }
            changing_end = false
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

        endView.setOnClickListener {
            changing_end = true
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        startView.setOnClickListener {
            changing_end = false
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        updateDialog.findViewById<Button>(R.id.bt_event_save).setOnClickListener(View.OnClickListener {
            val name = nameView.text.toString()

            val databaseHandler: EventDatabaseHandler = EventDatabaseHandler(this)

            if (name.isNotEmpty()) {
                val event = Event(0, name, start, end)

                val status = databaseHandler.addEvent(event)

                if (status > -1) {
                    Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_LONG).show()
                    nameView.text.clear()

                    setEvents()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name, Start time and End time cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        updateDialog.findViewById<Button>(R.id.bt_event_cancel).setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })

        updateDialog.show()
    }

    fun todoAction(view: View) {
        startActivity(Intent(this, TodoActivity::class.java))
    }

    fun selectStudyDialog(view: android.view.View) {

        val updateDialog = Dialog(this)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.select_study)

        val startView = updateDialog.findViewById<TextView>(R.id.study_startTV)
        val endView = updateDialog.findViewById<TextView>(R.id.study_endTV)

        val calendar = Calendar.getInstance()
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        var currentTime = LocalTime.now()


        var start = CalendarUtils.studyStart
        var end = CalendarUtils.studyEnd

        startView.text = formatter.format(start)
        endView.text = formatter.format(end)

        var changing_end = false

        fun calendar_date(): LocalTime {
            val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
            val minute: Int = calendar.get(Calendar.MINUTE)
            return LocalTime.of(hour, minute)
        }

        val timePicker = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            currentTime = calendar_date()

            if (changing_end) {
                end = currentTime
                endView.text = formatter.format(currentTime)
                CalendarUtils.studyEnd = currentTime
            } else {
                start = currentTime
                startView.text = formatter.format(currentTime)
                CalendarUtils.studyStart = currentTime
            }
            changing_end = false
        }

        endView.setOnClickListener {
            changing_end = true
            TimePickerDialog(
                this,
                timePicker,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        startView.setOnClickListener {
            changing_end = false
            TimePickerDialog(
                this,
                timePicker,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        updateDialog.findViewById<Button>(R.id.bt_study_save).setOnClickListener(View.OnClickListener {
            CalendarUtils.studyStart = start
            CalendarUtils.studyEnd = end
            updateDialog.dismiss()
        })
        updateDialog.findViewById<Button>(R.id.bt_study_cancel).setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })

        updateDialog.show()
    }
}

