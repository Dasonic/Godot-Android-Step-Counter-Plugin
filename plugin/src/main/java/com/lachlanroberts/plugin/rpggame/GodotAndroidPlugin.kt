package com.lachlanroberts.plugin.rpggame

import android.util.Log
import android.widget.Toast
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import org.godotengine.godot.plugin.SignalInfo

class GodotAndroidPlugin(godot: Godot): GodotPlugin(godot) {
    companion object {
        private const val RC_CODE = 989

        // List of constants for results of permission request
        const val PERMISSION_RESULT_GRANTED = 0
        const val PERMISSION_RESULT_DENIED = 1
        const val PERMISSION_RESULT_DENIED_SHOW_RATIONALE = 2

        // List of return values
        const val PERMISSION_CODE_UNAVAILABLE = -1
        const val PERMISSION_CODE_OK = 0

        const val SIGNAL_PERMISSION_REQUEST_COMPLETED = "permission_request_completed"
    }

    private val REQUEST_CODE_ACTIVITY_RECOGNITION = 100

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    private val currentActivity: Activity = activity ?: throw IllegalStateException()

    /**
     * Registers all the signals which the game may need to listen to
     */
    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo(SIGNAL_PERMISSION_REQUEST_COMPLETED, Any::class.java, String::class.java, Any::class.java)
        )
    }

    @UsedByGodot
    fun checkPermissionString(permission: String) : Int {
        return when (currentActivity.checkSelfPermission(permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.v(pluginName, "Already Granted")
                PERMISSION_RESULT_GRANTED
            }
            else -> {
                Log.v(pluginName, "Already Denied")
                val showRationale = currentActivity.shouldShowRequestPermissionRationale(permission)
                if (showRationale)
                    PERMISSION_RESULT_DENIED_SHOW_RATIONALE
                else
                    PERMISSION_RESULT_DENIED
            }
        }
    }

    /**
     * Launches the permission request launcher for the given permission string
     * @param permission one of string value as specified in [Manifest.permission]
     */
    @UsedByGodot
    fun requestPermissionString(permission: String) {
        currentActivity.requestPermissions(arrayOf(permission), RC_CODE)
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

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.v(pluginName, "User granted permissions")
            } else {
                // Permission denied
                // Handle the case where the user denied the permission
                // You might want to show a message to the user explaining why the permission is needed
                Log.v(pluginName, "User denied permissions")
            }
        }
    }

//        @UsedByGodot
//    fun subscibeToFitnessData() {
//        val localRecordingClient = FitnessLocal.getLocalRecordingClient(this)
//        // Subscribe to steps data
//        localRecordingClient.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA)
//            .addOnSuccessListener {
//                Log.i(TAG, "Successfully subscribed!")
//            }
//            .addOnFailureListener { e ->
//                Log.w(TAG, "There was a problem subscribing.", e)
//            }
//    }
}
