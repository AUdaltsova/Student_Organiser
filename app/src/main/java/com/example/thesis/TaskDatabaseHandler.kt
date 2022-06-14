package com.example.thesis

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskDatabaseHandler(private val context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 4
        private val DATABASE_NAME = "TaskDatabase"

        private val TABLE_TASKS = "TaskTable"

        private val KEY_ID = "id"
        private val KEY_NAME = "name"
        private val KEY_DEADLINE = "deadline"
        private val KEY_DURATION = "duration"
        private val KEY_DESCRIPTION = "description"
        private val KEY_NONDIVISIBLE = "divisibility"
        private val KEY_NONEARLY = "early"
        private val KEY_START = "start"
        private val KEY_PARENTSTART = "parent_start"
        private val KEY_DONE = "done"
        private val KEY_SCHEDULED = "scheduled"

        private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TASKS_TABLE = ("CREATE TABLE " + TABLE_TASKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," +  KEY_NAME + " TEXT,"
                + KEY_DEADLINE + " INTEGER,"
                + KEY_DURATION + " REAL,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_NONDIVISIBLE + " INTEGER,"
                + KEY_NONEARLY + " INTEGER,"
                + KEY_START + " INTEGER,"
                + KEY_PARENTSTART + " INTEGER,"
                + KEY_DONE + " INTEGER,"
                + KEY_SCHEDULED + " INTEGER" + ")")
        db?.execSQL(CREATE_TASKS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    fun addTask(task: Task): Long {
        val db = this.writableDatabase

        val contentValues = ContentValues()

        contentValues.put(KEY_NAME, task.name)
        contentValues.put(KEY_DEADLINE, formatter.format(task.deadline).toLong())
        contentValues.put(KEY_DURATION, task.duration.toString())
        contentValues.put(KEY_DESCRIPTION, task.description)
        contentValues.put(KEY_NONDIVISIBLE, if(task.nonDiv) 1 else 0)
        contentValues.put(KEY_NONEARLY, if(task.nonEarly) 1 else 0)
        contentValues.put(KEY_START, formatter.format(task.start).toLong())
        contentValues.put(KEY_PARENTSTART, formatter.format(task.parentStart).toLong())
        contentValues.put(KEY_DONE, 0)
        contentValues.put(KEY_SCHEDULED, 0)

        val success = db.insert(TABLE_TASKS, null, contentValues)

        db.close()
        return success
    }

    fun getUnscheduled(): ArrayList<Task> {
        val taskList: ArrayList<Task> = ArrayList<Task>()

        val selectQuery = "SELECT  * FROM $TABLE_TASKS WHERE done=0 AND scheduled=0"

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
        var deadline: LocalDateTime
        var duration: Double
        var description: String
        var nonDiv: Boolean
        var nonEarly: Boolean
        var start: LocalDateTime
        var parentStart: LocalDateTime
        var done: Boolean
        var scheduled: Boolean

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                deadline = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_DEADLINE))))
                duration = cursor.getDouble(cursor.getColumnIndex(KEY_DURATION))
                description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                nonDiv = (cursor.getInt(cursor.getColumnIndex(KEY_NONDIVISIBLE)) == 1)
                nonEarly = (cursor.getInt(cursor.getColumnIndex(KEY_NONEARLY)) == 1)
                start = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_START))))
                parentStart = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_PARENTSTART))))
                done = (cursor.getInt(cursor.getColumnIndex(KEY_DONE)) == 1)
                scheduled = (cursor.getInt(cursor.getColumnIndex(KEY_SCHEDULED)) == 1)

                val task = Task(id, name, deadline, duration, description, nonDiv, nonEarly, start, parentStart, done, scheduled)
                taskList.add(task)

            } while (cursor.moveToNext())
        }
        return taskList
    }

    fun unscheduleTasks(ids: List<String>, startTime: LocalDateTime, endTime: LocalDateTime) {
        val db = this.writableDatabase

        var eventHandler: EventDatabaseHandler = EventDatabaseHandler(context)

        val contentValues = ContentValues()
        contentValues.put(KEY_SCHEDULED, 0)

        for (id in ids) {
            var id: Int = id.toInt()

            var duration = eventHandler.deleteStudy(id, startTime, endTime)
            contentValues.put(KEY_DURATION, duration)
            db.update(TABLE_TASKS, contentValues,"$KEY_ID = $id", null)
        }

        db.close()

    }


    fun lookupTask(req_id: Int): Task {

        val selectQuery = "SELECT  * FROM $TABLE_TASKS WHERE $KEY_ID = $req_id"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)

        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return Task(0, "", CalendarUtils.selectedDate)
        }

        var id: Int = 0
        var name: String = ""
        var deadline: LocalDateTime = CalendarUtils.selectedDate
        var duration: Double = 0.0
        var description: String = ""
        var nonDiv: Boolean = false
        var nonEarly: Boolean = false
        var start: LocalDateTime = CalendarUtils.selectedDate
        var parentStart: LocalDateTime = CalendarUtils.selectedDate
        var done: Boolean = false
        var scheduled: Boolean = true

        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
            name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
            deadline = LocalDateTime.from(
                formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_DEADLINE)))
            )
            duration = cursor.getDouble(cursor.getColumnIndex(KEY_DURATION))
            description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
            nonDiv = (cursor.getInt(cursor.getColumnIndex(KEY_NONDIVISIBLE)) == 1)
            nonEarly = (cursor.getInt(cursor.getColumnIndex(KEY_NONEARLY)) == 1)
            start = LocalDateTime.from(
                formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_START)))
            )
            parentStart = LocalDateTime.from(
                formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_PARENTSTART)))
            )
            done = (cursor.getInt(cursor.getColumnIndex(KEY_DONE)) == 1)
            scheduled = (cursor.getInt(cursor.getColumnIndex(KEY_DONE)) == 1)
        }

            return Task(
                id,
                name,
                deadline,
                duration,
                description,
                nonDiv,
                nonEarly,
                start,
                parentStart,
                done,
                scheduled
            )
    }

    fun viewTasks(): ArrayList<Task> {

        val taskList: ArrayList<Task> = ArrayList<Task>()

        val selectQuery = "SELECT  * FROM $TABLE_TASKS"

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
        var deadline: LocalDateTime
        var duration: Double
        var description: String
        var nonDiv: Boolean
        var nonEarly: Boolean
        var start: LocalDateTime
        var parentStart: LocalDateTime
        var done: Boolean
        var scheduled: Boolean

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                deadline = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_DEADLINE))))
                duration = cursor.getDouble(cursor.getColumnIndex(KEY_DURATION))
                description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                nonDiv = (cursor.getInt(cursor.getColumnIndex(KEY_NONDIVISIBLE)) == 1)
                nonEarly = (cursor.getInt(cursor.getColumnIndex(KEY_NONEARLY)) == 1)
                parentStart = LocalDateTime.from(formatter
                    .parse(cursor.getString(cursor.getColumnIndex(KEY_PARENTSTART))))
                done = (cursor.getInt(cursor.getColumnIndex(KEY_DONE)) == 1)
                scheduled = (cursor.getInt(cursor.getColumnIndex(KEY_DONE)) == 1)

                if (!scheduled && !done) {
                    var old_start = LocalDateTime.from(formatter.parse(cursor.getString(
                                cursor.getColumnIndex(KEY_START))))
                    start = if (old_start < LocalDateTime.now()) LocalDateTime.now() else old_start
                } else {
                    start = LocalDateTime.from(formatter.parse(cursor.getString(
                        cursor.getColumnIndex(KEY_START))))
                }

                val task = Task(id, name, deadline, duration, description, nonDiv, nonEarly, start, parentStart, done, scheduled)
                taskList.add(task)

            } while (cursor.moveToNext())
        }
        return taskList
    }


    fun updateTask(task: Task): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        contentValues.put(KEY_NAME, task.name)
        contentValues.put(KEY_DEADLINE, formatter.format(task.deadline).toLong())
        contentValues.put(KEY_DURATION, task.duration.toString())
        contentValues.put(KEY_DESCRIPTION, task.description)
        contentValues.put(KEY_NONDIVISIBLE, if(task.nonDiv) 1 else 0)
        contentValues.put(KEY_NONEARLY, if(task.nonEarly) 1 else 0)
        contentValues.put(KEY_START, formatter.format(task.start).toLong())
        contentValues.put(KEY_PARENTSTART, formatter.format(task.parentStart).toLong())
        contentValues.put(KEY_DONE, if(task.done) 1 else 0)
        contentValues.put(KEY_SCHEDULED, if(task.scheduled) 1 else 0)


        val success = db.update(TABLE_TASKS, contentValues, KEY_ID + "=" + task.id, null)

        db.close()
        return success
    }

    fun deleteTask(task: Task): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, task.id)

        val success = db.delete(TABLE_TASKS, KEY_ID + "=" + task.id, null)

        var eventDatabaseHandler: EventDatabaseHandler = EventDatabaseHandler(context)

        eventDatabaseHandler.deleteStudy(task.id, task.start, task.deadline)

        db.close()
        return success
    }
}