package com.example.thesis

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EventDatabaseHandler(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 4
        private val DATABASE_NAME = "EventDatabase"

        private val TABLE_EVENTS = "EventTable"

        private val KEY_ID = "id"
        private val KEY_NAME = "name"
        private val KEY_START = "start"
        private val KEY_END = "end"
        private val KEY_STUDY = "study"
        private val KEY_PARENTTASK = "parent_task"

        private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }

    override fun onCreate(db: SQLiteDatabase?) {

        val CREATE_EVENTS_TABLE = ("CREATE TABLE " + TABLE_EVENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_START + " INTEGER," + KEY_END + " INTEGER,"
                + KEY_STUDY + " INTEGER," + KEY_PARENTTASK + " INTEGER" + ")")
        db?.execSQL(CREATE_EVENTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    fun addEvent(event: Event): Long {
        val db = this.writableDatabase

        val contentValues = ContentValues()

        contentValues.put(KEY_NAME, event.name)

        contentValues.put(KEY_START, formatter.format(event.start).toLong())
        contentValues.put(KEY_END, formatter.format(event.end).toLong())
        contentValues.put(KEY_STUDY, if(event.study) 1 else 0)
        contentValues.put(KEY_PARENTTASK, event.parent_id)

        val success = db.insert(TABLE_EVENTS, null, contentValues)

        db.close()
        return success
    }

    fun getBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<Event> {

        val eventList: ArrayList<Event> = ArrayList<Event>()

        var startTime: Long = (formatter.format(start)).toLong()
        val endTime: Long = (formatter.format(end)).toLong()

        val selectQuery = "SELECT * FROM $TABLE_EVENTS WHERE ($KEY_START >= $startTime AND $KEY_START < $endTime) OR ($KEY_END > $startTime AND $KEY_END <= $endTime)"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)

        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }

        var id: Int
        var name: String
        var start: LocalDateTime
        var end: LocalDateTime
        var study: Boolean
        var parent_id: Int

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                name = cursor.getString(cursor.getColumnIndex(KEY_NAME))

                start = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_START))))
                end = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_END))))

                study = (cursor.getInt(cursor.getColumnIndex(KEY_STUDY)) == 1)
                parent_id = (cursor.getInt(cursor.getColumnIndex(KEY_PARENTTASK)))

                val event = Event(id, name, start, end, study, parent_id)
                eventList.add(event)

            } while (cursor.moveToNext())
        }
        return eventList
    }

    fun deleteStudy(id: Int, start: LocalDateTime, end: LocalDateTime): Double {

        val startTime = (formatter.format(start)).toLong()
        val endTime = (formatter.format(end)).toLong()

        val selectQuery = "SELECT * FROM $TABLE_EVENTS WHERE $KEY_PARENTTASK=$id AND " +
                "(($KEY_START >= $startTime AND $KEY_START < $endTime) " +
                "OR ($KEY_END > $startTime AND $KEY_END <= $endTime))"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)

        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return 0.0
        }

        var totalDuration = 0.0

        if (cursor.moveToFirst()) {
            do {

                var eventStart = LocalDateTime.from(
                    formatter
                        .parse(cursor.getString(cursor.getColumnIndex(KEY_START)))
                )
                var eventEnd = LocalDateTime.from(
                    formatter
                        .parse(cursor.getString(cursor.getColumnIndex(KEY_END)))
                )
                totalDuration += (ChronoUnit.MINUTES.between(eventStart, eventEnd) / 60).toDouble()
            } while (cursor.moveToNext())
        }

        db.close()

        val dbW = this.writableDatabase
        dbW.delete(TABLE_EVENTS, "$KEY_PARENTTASK=$id AND + (($KEY_START >= $startTime AND $KEY_START < $endTime) OR ($KEY_END > $startTime AND $KEY_END <= $endTime))", null)
        dbW.close()

        return totalDuration
    }

    fun viewEvents(selected_date: LocalDateTime): ArrayList<Event> {

        val eventList: ArrayList<Event> = ArrayList<Event>()

        var now: Long = (formatter.format(selected_date).slice(0..7)+"0000").toLong()
        val tmr: Long = (formatter.format(selected_date.plusDays(1)).slice(0..7)+"0000").toLong()

        val selectQuery = "SELECT * FROM $TABLE_EVENTS WHERE $KEY_START >= $now AND $KEY_START < $tmr"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)

        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }

        var id: Int
        var name: String
        var start: LocalDateTime
        var end: LocalDateTime
        var study: Boolean
        var parent_id: Int

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                name = cursor.getString(cursor.getColumnIndex(KEY_NAME))

                start = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_START))))
                end = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_END))))

                study = (cursor.getInt(cursor.getColumnIndex(KEY_STUDY)) == 1)
                parent_id = (cursor.getInt(cursor.getColumnIndex(KEY_PARENTTASK)))

                val event = Event(id, name, start, end, study, parent_id)
                eventList.add(event)

            } while (cursor.moveToNext())
        }
        return eventList
    }


    fun updateEvent(event: Event): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        contentValues.put(KEY_ID, event.id)
        contentValues.put(KEY_NAME, event.name)

        contentValues.put(KEY_START, formatter.format(event.start).toLong())
        contentValues.put(KEY_END, formatter.format(event.end).toLong())

        val success = db.update(TABLE_EVENTS, contentValues, KEY_ID + "=" +event.id, null)

        db.close()
        return success
    }

    fun deleteEvent(event: Event): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, event.id)

        val success = db.delete(TABLE_EVENTS, KEY_ID + "=" + event.id, null)

        db.close()
        return success
    }
}