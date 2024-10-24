package com.fajri.prioriti.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fajri.prioriti.data.model.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE timestamp BETWEEN :startOfday AND :endOfDay")
    fun getTaskByDate(startOfday: Long, endOfDay: Long): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE timestamp > :currentTime")
    fun getTaskAfter(currentTime: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)


}