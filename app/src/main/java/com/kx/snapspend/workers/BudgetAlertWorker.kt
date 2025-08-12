package com.kx.snapspend.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.R
import java.util.Calendar
import android.app.PendingIntent // Add this import
import android.content.Intent // Add this import
import com.kx.snapspend.MainActivity // Add this import

class BudgetAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "BudgetAlertChannel"
        const val ALERT_THRESHOLD = 0.9 // 90%
    }

    override suspend fun doWork(): Result {
        val repository = (context.applicationContext as BudgetTrackerApplication).repository

        // Get all collections that have a budget set
        val collectionsWithBudgets = repository.getAllCollectionsSync().filter { it.budget > 0 }

        if (collectionsWithBudgets.isEmpty()) {
            return Result.success() // Nothing to check
        }

        // Get date range for the current month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        val monthEnd = System.currentTimeMillis()

        collectionsWithBudgets.forEachIndexed { index, collection ->
            val totalSpent = repository.getTotalForDateRange(monthStart, monthEnd, collection.name)
            val progress = totalSpent / collection.budget

            if (progress >= ALERT_THRESHOLD) {
                sendNotification(
                    collectionName = collection.name,
                    progressPercent = (progress * 100).toInt(),
                    notificationId = index // Use index as a unique ID for each notification
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(collectionName: String, progressPercent: Int, notificationId: Int) {
        createNotificationChannel()
        // ** THIS IS THE NEW PART **
        // 1. Create an intent that points to your app's main screen.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 2. Wrap it in a PendingIntent.
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val message = "You've spent $progressPercent% of your budget for $collectionName."

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure you have this drawable
            .setContentTitle("SnapSpend Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // 3. Attach the PendingIntent here

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications for when you approach a budget limit."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}