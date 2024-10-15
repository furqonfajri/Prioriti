package com.fajri.prioriti

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.fajri.prioriti.data.local.AppDatabase
import com.fajri.prioriti.data.model.Task
import com.fajri.prioriti.data.repository.TaskRepository
import com.fajri.prioriti.databinding.ActivityMainBinding
import com.fajri.prioriti.databinding.BottomSheetAddTaskBinding
import com.fajri.prioriti.viewmodel.TaskViewModel
import com.fajri.prioriti.viewmodel.TaskViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), CalendarAdapter.CalendarInterface {

    companion object {
        private val TAG = "MainActivity"
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomBinding: BottomSheetAddTaskBinding

    private lateinit var taskViewModel: TaskViewModel

    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val sdfb = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
    private val cal = Calendar.getInstance(Locale.ENGLISH)
    private var mStartD: Date? = null

    private val calendarAdapter = CalendarAdapter(this, arrayListOf())
    private val calendarList = ArrayList<CalendarData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
        initCalendar()

        val taskDao = AppDatabase.getInstance(this).taskDao()
        val repository = TaskRepository(taskDao)

        val factory = TaskViewModelFactory(repository)
        taskViewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

    }

    private fun init() {
        binding.apply {
            monthYearPicker.text = sdf.format(cal.time)
            calendarView.setHasFixedSize(true)
            calendarView.adapter = calendarAdapter
            monthYearPicker.setOnClickListener {
                displayDatePicker()
            }
            fabAdd.setOnClickListener {
                showBottoSheetDialog()
            }
        }
    }

    private fun initCalendar() {
        mStartD = Date()
        cal.time = Date()
        getDates()
    }

    private fun displayDatePicker(isBottomSheets: Boolean = false) {
        val materialDateBuilder: MaterialDatePicker.Builder<Long> =
            MaterialDatePicker.Builder.datePicker()
        materialDateBuilder.setTitleText("Select Date")
        val materialDatePicker = materialDateBuilder.build()
        materialDatePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
        materialDatePicker.addOnPositiveButtonClickListener {
            if (isBottomSheets) {
                try {
                    bottomBinding.datePicker.text = sdfb.format(it)


                } catch (e: ParseException) {
                    Log.e(TAG, "displayDatePicker: ${e.message}")
                }
            } else {
                try {
                    mStartD = Date(it)
                    binding.monthYearPicker.text = sdf.format(it)
                    cal.time = Date(it)

                    getDates()


                } catch (e: ParseException) {
                    Log.e(TAG, "displayDatePicker: ${e.message}")
                }
            }
        }
    }

    private fun displayTimePicker() {
        val materialTimeBuilder: MaterialTimePicker.Builder = MaterialTimePicker.Builder()
        materialTimeBuilder.setTitleText("Select Time")
        val materialTimePicker = materialTimeBuilder.build()
        materialTimePicker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")

        materialTimePicker.addOnPositiveButtonClickListener {
            val selectedHour = materialTimePicker.hour
            val selectedMinute = materialTimePicker.minute

            try {
                // Format waktu yang dipilih dan update ke TextView di Bottom Sheet
                val timeFormatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                bottomBinding.timePicker.text = timeFormatted

            } catch (e: Exception) {
                Log.e(TAG, "displayTimePicker: ${e.message}")
            }
        }

    }

    private fun getDates() {
        val dateList = ArrayList<CalendarData>()
        val dates = ArrayList<Date>()
        val monthCalendar = cal.clone() as Calendar
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        while(dates.size < maxDaysInMonth) {
            dates.add(monthCalendar.time)
            dateList.add(CalendarData(monthCalendar.time))
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarList.clear()
        calendarList.addAll(dateList)
        calendarAdapter.updateList(dateList)

        for (item in dateList.indices) {
            if (dateList[item].data == mStartD) {
                calendarAdapter.setPosition(item)
                binding.calendarView.scrollToPosition(item)
            }
        }
    }

    private fun showBottoSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomBinding.root)

        bottomBinding.datePicker.text = sdfb.format(cal.time)

        bottomBinding.datePicker.setOnClickListener{
            displayDatePicker(isBottomSheets = true)
        }

        bottomBinding.timePicker.setOnClickListener{
            displayTimePicker()
        }

        bottomBinding.btnAddTask.setOnClickListener{
            val dateText = bottomBinding.datePicker.text.toString()
            val timeText = bottomBinding.timePicker.text.toString()
            val task = bottomBinding.edtTask.text.toString()
            val description = bottomBinding.edtDescription.text.toString()
            val priority = when {
                bottomBinding.rbLow.isChecked -> "Low"
                bottomBinding.rbMedium.isChecked -> "Medium"
                bottomBinding.rbHigh.isChecked -> "High"
                else -> ""
            }

            if (dateText.isEmpty() || timeText.isEmpty() || task.isEmpty() || description.isEmpty() || priority == "" ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                saveTask(dateText, timeText, task, description, priority)
                bottomSheetDialog.dismiss()
            }



        }

        bottomSheetDialog.show()

    }

    private fun saveTask(dateText: String, timeText: String, task: String, description: String, priority: String ) {
        val timestamp = convertToTimestamp(dateText, timeText)
        val newTask = Task(
            title = task,
            description = description,
            priority = priority,
            timestamp = timestamp,
            isCompleted = false
        )

        taskViewModel.insertTask(newTask)
    }

    private fun convertToTimestamp(dateText: String, timeText: String): Long {
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ENGLISH)
        val dateTimeString = "$dateText, $timeText"
        return format.parse(dateTimeString)?.time ?: 0L
    }

    override fun onSelect(calendarData: CalendarData, position: Int, day: TextView) {
        calendarList.forEachIndexed { index, calendarData ->
            calendarData.isSelected = index == position
        }

        cal.time = calendarData.data

        calendarAdapter.updateList(calendarList)
    }


}