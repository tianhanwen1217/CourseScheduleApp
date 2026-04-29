package com.hanwentian.courseschedule.data.local

class CourseLocalRepository(
    private val courseDao: CourseDao
) {
    suspend fun getAllCourses(): List<CourseEntity> {
        return courseDao.getAllCourses()
    }

    suspend fun replaceAllCourses(courses: List<CourseEntity>) {
        courseDao.deleteAllCourses()
        if (courses.isNotEmpty()) {
            courseDao.insertCourses(courses)
        }
    }
}
