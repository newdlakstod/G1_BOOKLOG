package com.g1.booklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.g1.booklog.data.database.BookDatabase
import com.g1.booklog.data.repository.BookRepository
import com.g1.booklog.data.repository.FirebaseRepository
import com.g1.booklog.navigation.BookLogNavGraph
import com.g1.booklog.ui.theme.BookLogTheme
import com.g1.booklog.ui.viewmodel.AuthViewModel
import com.g1.booklog.ui.viewmodel.BookViewModel
import com.g1.booklog.ui.viewmodel.FriendViewModel
import com.g1.booklog.worker.UploadWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private fun scheduleAutoUpload() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val scheduledHours = listOf(6, 12, 18, 24)
        val nextHour = scheduledHours.firstOrNull { it > currentHour } ?: 6

        val nextTime = Calendar.getInstance().apply {
            if (nextHour == 24) { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }
            else set(Calendar.HOUR_OF_DAY, nextHour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val initialDelay = (nextTime.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0)

        val request = PeriodicWorkRequestBuilder<UploadWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_upload",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleAutoUpload()

        val database = BookDatabase.getDatabase(applicationContext)
        val repository = BookRepository(database.bookDao(), database.readingRecordDao())
        val firebaseRepo = FirebaseRepository()
        val prefs = getSharedPreferences("booklog_prefs", MODE_PRIVATE)
        val webClientId = getString(R.string.default_web_client_id)

        setContent {
            var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            val navController = rememberNavController()

            val viewModel: BookViewModel = viewModel(
                factory = BookViewModel.Factory(repository, firebaseRepo)
            )
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(firebaseRepo, webClientId)
            )
            val friendViewModel: FriendViewModel = viewModel(
                factory = FriendViewModel.Factory(firebaseRepo)
            )

            BookLogTheme(darkTheme = darkTheme) {
                BookLogNavGraph(
                    navController   = navController,
                    viewModel       = viewModel,
                    authViewModel   = authViewModel,
                    friendViewModel = friendViewModel,
                    darkTheme       = darkTheme,
                    onToggleTheme   = {
                        darkTheme = !darkTheme
                        prefs.edit().putBoolean("dark_theme", darkTheme).apply()
                    }
                )
            }
        }
    }
}
