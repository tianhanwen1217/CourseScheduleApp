package com.hanwentian.courseschedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekText: String,
    val periodText: String,
    val dayOfWeek: String,
    val credit: String = ""
)
