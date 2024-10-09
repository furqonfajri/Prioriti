package com.fajri.prioriti.data.model

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val priority: Byte,
    val timestamp: Long,
    val isCompleted: Boolean,
)
