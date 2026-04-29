package com.hanwentian.courseschedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Query("SELECT * FROM courses ORDER BY id ASC")
    suspend fun getAllCourses(): List<CourseEntity>

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
