package com.example.thesis

import java.time.LocalDateTime

class Task(
    var id: Int,
    var name: String,
    var deadline: LocalDateTime,
    var duration: Double = 0.25,
    var description: String = "",
    var nonDiv: Boolean = false,
    var nonEarly: Boolean = false,
    var start: LocalDateTime = LocalDateTime.now(),
    var parentStart: LocalDateTime = start,
    var done: Boolean = false,
    var scheduled: Boolean = false
)