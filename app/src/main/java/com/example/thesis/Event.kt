package com.example.thesis

import java.time.LocalDateTime


class Event (var id: Int,
             var name: String,
             var start: LocalDateTime,
             var end: LocalDateTime,
             var study: Boolean = false,
             var parent_id: Int = -1
)