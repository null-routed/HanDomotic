package com.masss.smartwatchapp.presentation.classifier

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.utilities.NotificationHelper


class SVMClassifierService: Service() {

    private val TAG = "SVM_CLASSIFIER_SERVICE"

    private var notificationHelper = NotificationHelper()
    companion object {
        private const val NOTIFICATION_ID = 2
        private const val NOTIFICATION_CHANNEL_ID = "SVM_CLASSIFIER_SERVICE_CHANNEL"
        private const val NOTIFICATION_CHANNEL_NAME = "SVM Classifier Service Channel"
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "SVMClassifierService has stopped.")
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val notificationChannel = notificationHelper.getNotificationChannelObject(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        return notificationHelper.createNotification(
            NOTIFICATION_CHANNEL_ID,
            "SVM Classifier Service",
            "The SVM Classifier is classifying...",
            R.drawable.handomotic_notification,
            this
        )
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
        Log.i(TAG, "SVMClassifierService has started.")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}