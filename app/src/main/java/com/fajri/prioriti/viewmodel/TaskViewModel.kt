package com.fajri.prioriti.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajri.prioriti.data.model.Task
import com.fajri.prioriti.data.repository.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository): ViewModel() {

    fun insertTask(task: Task): LiveData<Long> {
        val liveData = MutableLiveData<Long>()
        viewModelScope.launch {
            val id = repository.insertTask(task)
            liveData.postValue(id)
        }
        return liveData
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun getTasks(): LiveData<List<Task>> {
            return repository.getTasks()
    }

    fun getTaskByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>> {
        return repository.getTaskByDate(startOfDay, endOfDay)
    }
}