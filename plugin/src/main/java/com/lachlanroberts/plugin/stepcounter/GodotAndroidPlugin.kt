package com.lachlanroberts.plugin.stepcounter

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.request.LocalDataReadRequest
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import com.google.android.gms.fitness.data.LocalField

class GodotAndroidPlugin(godot: Godot): GodotPlugin(godot) {
    companion object {
        private const val RC_CODE = 989
        const val permissionRequired = "android.permission.ACTIVITY_RECOGNITION"

        // List of constants for results of permission request
        const val PERMISSION_RESULT_GRANTED = 0
        const val PERMISSION_RESULT_DENIED = 1
        const val PERMISSION_RESULT_DENIED_SHOW_RATIONALE = 2

        // List of return values
        const val PERMISSION_CODE_UNAVAILABLE = -1
        const val PERMISSION_CODE_OK = 0

        const val SIGNAL_PERMISSION_REQUEST_COMPLETED = "permission_request_completed"
        const val SIGNAL_TOTAL_STEPS_RETRIEVED = "total_steps_retrieved"
    }

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    private val currentActivity: Activity = activity ?: throw IllegalStateException()

    val localRecordingClient = FitnessLocal.getLocalRecordingClient(currentActivity)

    /**
     * Registers all the signals which the game may need to listen to
     */
    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo(SIGNAL_PERMISSION_REQUEST_COMPLETED, Any::class.java, String::class.java, Any::class.java),
            SignalInfo(SIGNAL_TOTAL_STEPS_RETRIEVED, Any::class.java)
        )
    }

    /**
     * Checks if the passed permission is granted. Returns 0 for true and 1 or 2 for false
     */
    @UsedByGodot
    fun checkRequiredPermissions() : Int {
        return when (currentActivity.checkSelfPermission(permissionRequired)) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.v(pluginName, "Already Granted")
                PERMISSION_RESULT_GRANTED
            }
            else -> {
                Log.v(pluginName, "Already Denied")
                val showRationale = currentActivity.shouldShowRequestPermissionRationale(permissionRequired)
                if (showRationale)
                    PERMISSION_RESULT_DENIED_SHOW_RATIONALE
                else
                    PERMISSION_RESULT_DENIED
            }
        }
    }

    /**
     * Launches the permission request launcher for android.permission.ACTIVITY_RECOGNITION
     */
    @UsedByGodot
    fun requestRequiredPermissions() {
        currentActivity.requestPermissions(arrayOf(permissionRequired), RC_CODE)
    }

    /**
     * Callback for the Permission request launcher
     */
    override fun onMainRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ) {
        super.onMainRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_CODE && permissions != null && permissions.isNotEmpty()){

            val requestedPermission = permissions.first()
            val permissionCode = 0

            if (grantResults?.first() == PackageManager.PERMISSION_GRANTED) {
                Log.v(pluginName, "Granted")
                emitSignal(
                    SIGNAL_PERMISSION_REQUEST_COMPLETED,
                    permissionCode, requestedPermission, PERMISSION_RESULT_GRANTED
                )
            } else {
                Log.v(pluginName, "Denied")
                emitSignal(
                    SIGNAL_PERMISSION_REQUEST_COMPLETED,
                    permissionCode, requestedPermission, PERMISSION_RESULT_DENIED
                )
            }
        }
    }

    /**
     * Registers the app to start tracking the steps.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @UsedByGodot
    fun subscibeToFitnessData() {
        // Subscribe to steps data
        localRecordingClient.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i(pluginName, "Successfully subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(pluginName, "There was a problem subscribing.", e)
            }
    }

    /**
     * Gets the amount of steps taken in the last numSeconds and emits a signal with the result
     * subcribeToFitnessData MUST be called first
     * @param numSeconds the number of seconds to fetch the steps data from
     */
    @UsedByGodot
    fun getStepsInLastSeconds(numSeconds: Int) {
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusSeconds(numSeconds.toLong())

        var totalSteps = 0
        try {
            val readRequest = LocalDataReadRequest.Builder()
                .read(LocalDataType.TYPE_STEP_COUNT_DELTA) // Directly read the step count data
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()

            localRecordingClient.readData(readRequest).addOnSuccessListener { response ->

                for (dataSet in response.dataSets) {
                    totalSteps += dataSet.dataPoints.sumOf { it.getValue(LocalField.FIELD_STEPS).asInt() }
                }
                Log.i(pluginName, "Total steps: $totalSteps")
                emitSignal(
                    SIGNAL_TOTAL_STEPS_RETRIEVED,
                    totalSteps
                )
            }.addOnFailureListener { e ->
                Log.w(pluginName, "There was an error reading data", e)
            }
        } catch (e: Exception) {
            Log.e(pluginName, "Exception occurred while reading data: ", e)
        }
    }

}
