package com.masss.smartwatchapp.presentation.utilities

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.masss.handomotic.BTBeaconManager
import com.masss.handomotic.models.Beacon
import com.masss.smartwatchapp.R


class UIManager(
    private val activity: Activity,
    private val startAppServices: () -> Unit,
    private val stopAppServices: () -> Unit
)
{

    private val TAG = "UI_MANAGER"

    fun setupMainButton(
        missingRequiredPermissionsView: Boolean,
        appWasRunningWhenResumed: Boolean
    ) {
        val mainButton: Button = activity.findViewById(R.id.mainButton)

        if (missingRequiredPermissionsView)            // graying out the button to make it look disabled
            mainButton.background = ContextCompat.getDrawable(activity, R.drawable.power_disabled)
        else {                    // restoring the button to its main style
            if (appWasRunningWhenResumed)           // if the app was running in the background, the button should be displayed as power off when resumed
                mainButton.background = ContextCompat.getDrawable(activity, R.drawable.power_off)
            else
                mainButton.background = ContextCompat.getDrawable(activity, R.drawable.power_on)
        }
    }

    fun setupMainButtonOnClickListener(appIsRecording: Boolean, missingRequiredPermissionsView: Boolean) {
        val mainButton: Button = activity.findViewById(R.id.mainButton)
        mainButton.setOnClickListener {
            if (missingRequiredPermissionsView)
                Toast.makeText(activity, "Some needed permissions are still required", Toast.LENGTH_SHORT).show()
            else {
                if (appIsRecording)
                    stopAppServices()
                else
                    startAppServices()
                
                toggleMainButtonBackground(mainButton, appIsRecording)
            }
        }
    }

    fun setupWhereAmIButton(
        missingRequiredPermissionsView: Boolean,
        btBeaconManager: BTBeaconManager,
        knownBeacons: List<Beacon>?)
    {
        val whereAmIButton: Button = activity.findViewById(R.id.whereAmIButton)
        val whereAmITextView: TextView = activity.findViewById(R.id.whereAmIText)

        if (missingRequiredPermissionsView) {
            whereAmITextView.visibility = View.GONE
            whereAmIButton.visibility = View.GONE
        } else {
            whereAmITextView.visibility = View.VISIBLE
            whereAmIButton.visibility = View.VISIBLE
        }

        whereAmIButton.setOnClickListener {
            val closeBTBeacons = btBeaconManager.getBeacons()       // getting close beacons

            Log.i(TAG, "Close detected beacons: ${closeBTBeacons.values.joinToString(separator = ", ")}")

            if (!knownBeacons.isNullOrEmpty()) {                // some known beacons have been registered
                if (closeBTBeacons.isNotEmpty()) {              // some beacons are close by
                    val closestBeaconLocation = getCurrentRoom(closeBTBeacons, knownBeacons)
                    if (closestBeaconLocation != null)
                        Toast.makeText(activity, "You are here: $closestBeaconLocation", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(activity, "No close known beacons found", Toast.LENGTH_SHORT).show()
                } else
                    Toast.makeText(activity, "No close known beacons found", Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(activity, "No beacons have been registered yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentRoom(closeBTBeacons: MutableMap<String, Beacon>, knownBeacons: List<Beacon>?): String? {
        if (knownBeacons.isNullOrEmpty() || closeBTBeacons.isEmpty())
            return null

        val knownBeaconsMap = knownBeacons.associateBy { it.address }

        // Sorting the scan results, ordering for the nearest beacon first
        val sortedEntries = closeBTBeacons.entries
            .sortedByDescending { it.value.rssi }

        closeBTBeacons.clear()
        closeBTBeacons.putAll(sortedEntries.associate { it.toPair() })

        for (beacon in closeBTBeacons.values) {
            knownBeaconsMap[beacon.address]?.let {
                Log.i(TAG, "Current room detected: ${it.name}")
                return it.name
            }
        }

        Log.i(TAG, "No room has been detected")
        return null
    }

    @SuppressLint("InflateParams")
    fun showGestureRecognizedScreen(recognizedGesture: String, btBeaconManager: BTBeaconManager, knownBeacons: List<Beacon>?, durationInMillis: Long) {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_gesture_recognized, null)

        val popupWindow = PopupWindow(
            popupView,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            true
        )

        // Set background to semi-transparent black
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
        popupWindow.isOutsideTouchable = false

        // get close beacons. if no known beacon is close by, the gesture is useless, it doesn't activate anything
        val closeBTBeacons = btBeaconManager.getBeacons()

        val closestKnownBeaconLocation = getCurrentRoom(closeBTBeacons, knownBeacons)

        val popupMainView: LinearLayout = popupView.findViewById(R.id.popup_main_parent)
        val messageTextView: TextView = popupView.findViewById(R.id.gesture_recognized_text)
        if (closestKnownBeaconLocation.isNullOrEmpty()) {
            popupMainView.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.holo_red_light))
            messageTextView.text = activity.getString(R.string.no_known_beacons_are_near_you)
        } else {
            popupMainView.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.holo_green_light))
            messageTextView.text =
                activity.getString(
                    R.string.gesture_in_room,
                    activity.getString(R.string.gesture_recognized, recognizedGesture),
                    closestKnownBeaconLocation
                )
        }

        val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = VibrationEffect.createOneShot(durationInMillis, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val fadeAfterMillis = 3000L
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            popupWindowFadeOut(popupWindow, popupView)
        }, fadeAfterMillis)
    }

    private fun popupWindowFadeOut(popupWindow: PopupWindow, popupView: View) {
        val animationDuration = 1000L // 1 second fadeout duration
        val fadeOut = ObjectAnimator.ofFloat(popupView, "alpha", 1f, 0f)
        fadeOut.duration = animationDuration
        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                popupWindow.dismiss()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        fadeOut.start()
    }

    fun notifyGestureRecognizedOnAppBackground(btBeaconManager: BTBeaconManager, knownBeacons: List<Beacon>?, durationInMillis: Long) {
        val closeBTBeacons = btBeaconManager.getBeacons()

        val closestKnownBeaconLocation = getCurrentRoom(closeBTBeacons, knownBeacons)

        if (!closestKnownBeaconLocation.isNullOrEmpty()) {
            val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val vibrationEffect =
                VibrationEffect.createOneShot(durationInMillis, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }

    private fun toggleMainButtonBackground(button: Button, appIsRecording: Boolean) {
        if (appIsRecording)
            button.background = ContextCompat.getDrawable(activity, R.drawable.power_on)
        else
            button.background = ContextCompat.getDrawable(activity, R.drawable.power_off)

    }

    fun toggleSettingsNavigationUI(visible: Boolean) {
        val deniedPermissionAcceptText = activity.findViewById<TextView>(R.id.permissionsText)
        val permissionsActivityButton = activity.findViewById<Button>(R.id.grantMissingPermissionsButton)

        if (visible) {
            deniedPermissionAcceptText.visibility = View.VISIBLE
            permissionsActivityButton.visibility = View.VISIBLE
        } else {
            deniedPermissionAcceptText.visibility = View.GONE
            permissionsActivityButton.visibility = View.GONE
        }
    }
}