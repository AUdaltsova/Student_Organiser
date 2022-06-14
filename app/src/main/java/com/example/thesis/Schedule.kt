package com.example.thesis

import android.content.Context
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


fun prepareTasks(task: Task): ArrayList<Task> {

    var tasksUpdated = ArrayList<Task>()

    if (task.duration > 0.25 && !task.nonDiv) {
        var dummyNum: Int =
            (task.duration / 0.25).toBigDecimal().setScale(0, RoundingMode.UP).toInt()
        var dummy = Task(
            task.id, task.name, task.deadline, 0.25, task.description, task.nonDiv,
            task.nonEarly, task.start, task.parentStart, task.done, task.scheduled
        )
        for (i in 0 until dummyNum) {
            tasksUpdated.add(dummy)
        }
    } else tasksUpdated.add(task)

    tasksUpdated.sortBy { it.deadline }
    return tasksUpdated
}



fun makeTimetable(context: Context) {

    val taskDatabaseHandler: TaskDatabaseHandler = TaskDatabaseHandler(context)
    val eventDatabaseHandler: EventDatabaseHandler = EventDatabaseHandler(context)

    var tasks = taskDatabaseHandler.getUnscheduled()

    tasks.sortBy { it.deadline }

    var tasksUpdated = ArrayList<Task>()

    for (task in tasks) {

        if (task.start < LocalDateTime.now()) {
            val now = LocalDateTime.now()
            task.start = LocalDateTime.of(now.year, now.month, now.dayOfMonth, now.hour+1, 0)
        }

        if (task.duration > 2 && !task.nonDiv) {
            var dummy = Task(
                task.id, task.name, task.deadline, 2.0, task.description, true,
                task.nonEarly, task.start, task.start, task.done, task.scheduled
            )
            var dummyNum: Int = (task.duration / 2).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()

            val daysForProject =  ChronoUnit.DAYS.between(task.start, task.deadline).toInt() + 1

            if (dummyNum > daysForProject) {
                dummy.duration = (task.duration.toInt() / daysForProject).toDouble()
                dummyNum = (task.duration / dummy.duration.toInt()).toBigDecimal().setScale(0, RoundingMode.DOWN).toInt()
            }

            var startOffset: Long = (
                    daysForProject / dummyNum
                    ).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toLong()
            for (i in 0 until dummyNum) {
                tasksUpdated.add(Task(dummy.id,
                    dummy.name, dummy.deadline, dummy.duration, dummy.description,
                    dummy.nonDiv, dummy.nonEarly, dummy.start.plusDays(i * startOffset), dummy.parentStart,
                    dummy.done, dummy.scheduled))
            }
        } else if (ChronoUnit.DAYS.between(task.start, task.deadline).toInt() > 80) {
            task.start = task.deadline.minusDays((80))
            tasksUpdated.add(task)
        }
        else {
            tasksUpdated.add(task)
        }
    }

    for (task in tasksUpdated) {

        var events_to_add = ArrayList<Event>()
        var taskList = prepareTasks(task)

        var events = eventDatabaseHandler.getBetween(task.start, task.deadline)

        var sessions: Int =
            (ChronoUnit.MINUTES.between(task.start, task.deadline).toDouble() / 15).toBigDecimal()
                .setScale(0, RoundingMode.DOWN).toInt()

        var timetable = IntArray(sessions) { -1 } // -1 is the tag for empty sessions
        var nonEarlyCheck = IntArray(sessions) { 0 }
        var deadlineCheck = LongArray(sessions) {
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(task.start).toLong()
        }

        var studyOffset =
            (ChronoUnit.MINUTES.between(LocalTime.from(task.start), CalendarUtils.studyEnd)
                .toDouble() / 15).toBigDecimal().setScale(0, RoundingMode.DOWN).toInt()

        if (LocalTime.from(task.start) < CalendarUtils.studyStart || LocalTime.from(task.start) >= CalendarUtils.studyEnd) {

            studyOffset =
                ChronoUnit.MINUTES.between(LocalTime.from(task.start), CalendarUtils.studyStart)
                    .toInt()

            if (LocalTime.from(task.start) > CalendarUtils.studyStart) {
                studyOffset += 24 * 60
            }

            studyOffset =
                (studyOffset.toDouble() / 15).toBigDecimal().setScale(0, RoundingMode.UP).toInt()

            for (i in 0 until studyOffset) {
                timetable[i] = -2 // -2 is the tag for time outside of [studyStart, studyEnd]
            }
            var newStart = LocalTime.from(task.start).plusMinutes(15 * studyOffset.toLong())
            studyOffset = (ChronoUnit.MINUTES.between(newStart, CalendarUtils.studyEnd)
                .toDouble() / 15).toBigDecimal().setScale(0, RoundingMode.DOWN).toInt()
        }

        var dayLength =
            (ChronoUnit.MINUTES.between(CalendarUtils.studyStart, CalendarUtils.studyEnd)
                .toDouble() / 15).toBigDecimal().setScale(0, RoundingMode.DOWN).toInt()
        var nightLength = 24 * 4 - dayLength

        var j: Int

        while (studyOffset < timetable.size) {
            j = 0
            while (j < nightLength && studyOffset + j < timetable.size) {
                timetable[studyOffset + j] = -2
                j++
            }
            studyOffset += j + dayLength
        }


        for (event in events) {

            var eventOffset: Int
            var event_length: Int

            if (event.start >= task.start) {

                eventOffset =
                    (ChronoUnit.MINUTES.between(task.start, event.start)
                        .toDouble() / 15).toBigDecimal()
                        .setScale(0, RoundingMode.DOWN).toInt()

                event_length = if (task.deadline < event.end) timetable.size - eventOffset else (ChronoUnit.MINUTES.between(task.start, event.end)
                        .toDouble() / 15).toBigDecimal()
                        .setScale(0, RoundingMode.UP).toInt() - eventOffset
            } else {
                eventOffset = 0

                event_length =
                    (ChronoUnit.MINUTES.between(task.start, event.end)
                        .toDouble() / 15).toBigDecimal()
                        .setScale(0, RoundingMode.UP).toInt()
            }

            var id_inseart = IntArray(event_length) { event.id }

            id_inseart.copyInto(timetable, eventOffset)

            if (event.study) {
                var parentTask = taskDatabaseHandler.lookupTask(event.parent_id)
                var study_inseart = LongArray(event_length) {
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(parentTask.deadline).toLong()
                }
                study_inseart.copyInto(deadlineCheck, eventOffset)
                if (parentTask.nonEarly) {
                    var nonEarly_inseart = IntArray(event_length) { 1 }
                    nonEarly_inseart.copyInto(nonEarlyCheck, eventOffset)
                }
            }
        }

        var taskScheduled = true

        for (subtask in taskList) {

            val (success, reschedule, studyEvent) = scheduleTask(
                context,
                subtask,
                timetable,
                nonEarlyCheck,
                deadlineCheck
            )

            if (success == -1) {
                task.done = true
                taskDatabaseHandler.updateTask(task)
                taskScheduled = false
                break
            } else {
                events_to_add.add(studyEvent)

                var eventOffset: Int = success

                var event_length = (ChronoUnit.MINUTES.between(studyEvent.start, studyEvent.end)
                    .toDouble() / 15).toBigDecimal().setScale(0, RoundingMode.UP).toInt()

                var id_inseart = IntArray(event_length) { studyEvent.id }

                id_inseart.copyInto(timetable, eventOffset)

                var study_inseart = LongArray(event_length) {
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(subtask.deadline).toLong()
                }
                study_inseart.copyInto(deadlineCheck, eventOffset)
                if (subtask.nonEarly) {
                    var nonEarly_inseart = IntArray(event_length) { 1 }
                    nonEarly_inseart.copyInto(nonEarlyCheck, eventOffset)
                }
            }

            if (reschedule.isNotEmpty()) {
                taskDatabaseHandler.unscheduleTasks(reschedule, studyEvent.start, studyEvent.end)
            }

        }

        if (taskScheduled) {
            task.scheduled = true
            task.duration = 0.0
            taskDatabaseHandler.updateTask(task)

            events_to_add.sortBy { it.start }

            var i = 0
            while (i < events_to_add.size) {

                var j = i + 1
                while (j < events_to_add.size && events_to_add[j - 1].end == events_to_add[j].start) {
                    j++
                }
                var event = events_to_add[i]
                event.end = events_to_add[j - 1].end

                eventDatabaseHandler.addEvent(event)
                i = j
            }
        }
    }
}

fun scheduleTask(context: Context,
                 task: Task,
                 timetable: IntArray,
                 nonEarlyCheck: IntArray,
                 deadlineCheck: LongArray): Triple<Int, List<String>, Event> {


    var taskDurationSs = (task.duration / 0.25).toBigDecimal().setScale(0, RoundingMode.UP).toInt()

    fun findSn0(sn0_init: Int): Pair<Int, Set<String>> {
        var sn0_i = sn0_init + 1
        var i = sn0_init + 1
        var s = 0

        while (s < taskDurationSs && sn0_i < timetable.size && i < timetable.size) {
            var ss = timetable[i]
            if (ss == -1) {
                if (s == 0) {
                    sn0_i = i
                }
                s += 1
            } else {
                s = 0
            }
            i++
        }

        if (s >= taskDurationSs) {
            return Pair(sn0_i, mutableSetOf<String>())
        }

        var rescheduleID = mutableSetOf<String>()

        while (s < taskDurationSs && sn0_i < timetable.size && i < timetable.size) {
            var ss = timetable[i]
            if (ss == -1 || (ss > 0 && nonEarlyCheck[i] == 0 &&
                        deadlineCheck[i] > DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                    .format(task.deadline).toLong())
            ) {
                if (s == 0) {
                    sn0_i = i
                }
                s += 1
                rescheduleID.add(timetable[i].toString())
            } else {
                s = 0
                rescheduleID = mutableSetOf<String>()
            }
            i++
        }

        if (s >= taskDurationSs) {
            return Pair(sn0_i, rescheduleID)
        }

        return Pair(-1, rescheduleID)
    }

    fun findSn0_nonEarly(sn0_init: Int): Pair<Int, Set<String>> {
        var sn0_i = sn0_init - 1
        var i = sn0_init - 1
        var s = 0

        while (s < taskDurationSs && sn0_i > -1 && i > -1) {
            var ss = timetable[i]
            if (ss == -1) {
                if (s == 0) {
                    sn0_i = i
                }
                s += 1
            } else {
                s = 0
            }
            i--
        }

        if (s >= taskDurationSs) {
            return Pair(sn0_i - taskDurationSs + 1, mutableSetOf<String>())
        }

        var rescheduleID = mutableSetOf<String>()

        while (s < taskDurationSs && sn0_i > -1 && i > -1) {
            var ss = timetable[i]
            if (ss == -1 || (ss > 0 && nonEarlyCheck[i] == 0 &&
                        deadlineCheck[i] > DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                    .format(task.deadline).toLong())
            ) {
                if (s == 0) {
                    sn0_i = i
                }
                s += 1
                rescheduleID.add(timetable[i].toString())
            } else {
                s = 0
                rescheduleID = mutableSetOf<String>()
            }
            i--
        }

        if (s >= taskDurationSs) {
            return Pair(sn0_i - taskDurationSs + 1, rescheduleID)
        }

        return Pair(-1, rescheduleID)
    }

    var (sn0_i, reschedule) = if (task.nonEarly) findSn0_nonEarly(timetable.size) else findSn0(-1)
    var rescheduleID = reschedule.toList()

    var event = Event(-1, "", LocalDateTime.now(), LocalDateTime.now().plusHours(1))
    if (sn0_i > -1) {
        event = Event(
            0,
            task.name,
            task.start.plusMinutes((sn0_i).toLong() * 15),
            task.start.plusMinutes((sn0_i + taskDurationSs).toLong() * 15),
            study = true,
            parent_id = task.id
        )
    }

    return Triple(sn0_i, rescheduleID, event)
}
