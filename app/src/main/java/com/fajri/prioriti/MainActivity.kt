package com.fajri.prioriti

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CalendarAdapter.CalendarInterface {

    companion object {
        private val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
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

    private val taskAdapter = TaskAdapter(emptyList())

    private var selectedDate: Date = Date()

    private lateinit var alarmHandler: AlarmHandler


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

        requestNotificationPermission()

        init()
        initCalendar()
//        createNotificationChannel()

        alarmHandler = AlarmHandler(this)

        val taskDao = AppDatabase.getInstance(this).taskDao()
        val repository = TaskRepository(taskDao)

        val factory = TaskViewModelFactory(repository)
        taskViewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)


        binding.taskView.adapter = taskAdapter

        updateTaskListForSelectedDate()

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

            if (dateText.isEmpty() || timeText.isEmpty() || task.isEmpty() || description.isEmpty() || priority == "" || timeText == "Set Time" ) {
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

        taskViewModel.insertTask(newTask).observeOnce(this) { id ->
            val savedTask = newTask.copy(id = id.toInt())
            updateTaskListForSelectedDate()
            setAlarm(savedTask)
        }

    }

    private fun convertToTimestamp(dateText: String, timeText: String): Long {
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ENGLISH)
        val dateTimeString = "$dateText, $timeText"
        return format.parse(dateTimeString)?.time ?: 0L
    }

    private fun updateTaskListForSelectedDate() {

        val calStart = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        Log.d("SELECTED DATE", "${sdfb.format(calStart.timeInMillis)} - ${sdfb.format(calEnd.timeInMillis)}")


        taskViewModel.getTaskByDate(calStart.timeInMillis, calEnd.timeInMillis).observeOnce(this) { tasks ->
            Log.d("TASK LIST", tasks.toString())
            taskAdapter.updateTasks(tasks)
        }

    }

//    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(task: Task) {
//        val workManager = WorkManager.getInstance(this)
//
//        val data = workDataOf(
//            "title" to task.title,
//            "description" to task.description
//        )
//
//        val notificationWorkRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
//            .setInputData(data)
//            .setInitialDelay(getDelay(task.timestamp), TimeUnit.MILLISECONDS)
//            .build()
//
//        workManager.enqueue(notificationWorkRequest)

//        val context = this
//
//        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        val intent = Intent(context, NotificationReceiver::class.java).apply {
//            putExtra("title", task.title)
//            putExtra("description", task.description)
//        }
//        val pendingIntent1 = PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//        val pendingIntent2 = PendingIntent.getBroadcast(context, (System.currentTimeMillis() + 1).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//
//        val alarmTime = task.timestamp

        when(task.priority) {
            "Low" -> {
                alarmHandler.scheduleAlarm(task, isJustNotification = true)
            }
            "Medium" -> {
                alarmHandler.scheduleAlarm(task)
            }
            "High" -> {
                alarmHandler.scheduleAlarm(task)
                alarmHandler.scheduleAlarm(task, offsetMillis = 60_000)
            }
        }
    }

//    private fun getDelay(timestamp: Long): Long {
//        val currentTime = System.currentTimeMillis()
//        return if (timestamp > currentTime) {
//            timestamp - currentTime
//        } else {
//            0
//        }
//    }

//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "TASK_NOTIFICATION_CHANNEL",
//                "Task Notifications",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//        }
//    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

//    private fun sendNotification(notificationManager: NotificationManager, task: Task) {
//        val channelId = "TASK_NOTIFICATION_CHANNEL"
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_check_circle)
//            .setContentTitle(task.title)
//            .setContentText(task.description)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(channelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH)
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        notificationManager.notify(task.id, notificationBuilder.build())
//    }

    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
        observe(lifecycleOwner, object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                removeObserver(this)
            }
        })
    }

    override fun onSelect(calendarData: CalendarData, position: Int, day: TextView) {
        calendarList.forEachIndexed { index, calendarDatas ->
            calendarDatas.isSelected = index == position
        }

        selectedDate = calendarData.data
        cal.time = selectedDate


        updateTaskListForSelectedDate()


        calendarAdapter.updateList(calendarList)
    }


}