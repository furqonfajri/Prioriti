package com.fajri.prioriti

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fajri.prioriti.data.model.Task
import com.fajri.prioriti.databinding.DateItemBinding
import com.fajri.prioriti.databinding.TaskItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(private var taskList: List<Task>): RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    inner class TaskViewHolder(private val binding: TaskItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(taskData: Task, position: Int) {
            val sdfb = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
            val task = taskData
            binding.titleTask.text = task.title
            binding.descriptionTask.text = task.description
            binding.tvTaskTime.text  = sdfb.format(task.timestamp)

            binding.line.visibility = if (position == taskList.size - 1) View.INVISIBLE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskAdapter.TaskViewHolder {
        val binding = TaskItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskAdapter.TaskViewHolder, position: Int) {
        holder.bind(taskList[position], position)
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTasks(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()
    }

}