package com.fajri.prioriti.data.repository

import androidx.lifecycle.LiveData
import com.fajri.prioriti.data.dao.TaskDao
import com.fajri.prioriti.data.model.Task

class TaskRepository (private val taskDao: TaskDao){
    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    fun getTasks(): LiveData<List<Task>> {
        return taskDao.getAllTasks()
    }

    fun getTaskByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>> {
        return taskDao.getTaskByDate(startOfDay, endOfDay)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
}