package com.masss.smartwatchapp.presentation.classifier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masss.smartwatchapp.R


class SVMClassifierService: Service() {

    companion object {
        private const val NOTIFICATION_ID = 2
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("SVMClassifierService", "SVMClassifierService has stopped.")
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "SVM_CLASSIFIER_SERVICE_CHANNEL"
        val channel = NotificationChannel(notificationChannelId, "SVM Classifier Service Channel", NotificationManager.IMPORTANCE_LOW)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("SVM Classifier Service")
            .setContentText("Classifying...")
            .setSmallIcon(R.drawable.handomotic_notification)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val classifierReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val classificationResult = it.getStringExtra("Prediction")

                    val resultIntent = Intent("SVMClassifierService_RecognizedGesture")
                    resultIntent.putExtra("ClassificationResult", classificationResult)
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(resultIntent)
                }
            }
        }

        val filter = IntentFilter("SVMClassifier_RecognizedGesture")
        LocalBroadcastManager.getInstance(this).registerReceiver(classifierReceiver, filter)
        Log.i("SVMClassifierService", "SVMClassifierService has started.")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}