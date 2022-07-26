package com.fitnesstracker

import android.app.Activity
import com.facebook.react.bridge.*
import com.fitnesstracker.googlefit.GoogleFitManager
import com.fitnesstracker.permission.Permission
import com.fitnesstracker.permission.PermissionKind
import com.google.android.gms.fitness.FitnessOptions
import java.util.Date


class RNFitnessTrackerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val googleFitManager: GoogleFitManager = GoogleFitManager(reactContext)

    override fun getName(): String {
        return "RNFitnessTracker"
    }

    private fun getActivity(promise: Promise): Activity? {
        val activity: Activity? = currentActivity

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist")
            return null
        }

        return activity
    }

    @ReactMethod
    fun authorize(readPermissions: ReadableArray, writePermission: ReadableArray, promise: Promise) {
        if (googleFitManager.isAuthorized()) {
            return promise.resolve(true)
        }

        val permissions: ArrayList<Permission> =
            createPermissionsFromReactArray(readPermissions, FitnessOptions.ACCESS_READ, promise)
        permissions.addAll(createPermissionsFromReactArray(writePermission, FitnessOptions.ACCESS_WRITE, promise))

        val activity: Activity = getActivity(promise) ?: return
        googleFitManager.authorize(promise, activity, permissions)
    }


    @ReactMethod
    fun isTrackingAvailable(readPermissions: ReadableArray, writePermission: ReadableArray, promise: Promise) {
        val permissions: ArrayList<Permission> =
            createPermissionsFromReactArray(readPermissions, FitnessOptions.ACCESS_READ, promise)
        permissions.addAll(createPermissionsFromReactArray(writePermission, FitnessOptions.ACCESS_WRITE, promise))

        googleFitManager.isTrackingAvailable(promise, permissions)
    }

    @ReactMethod
    fun queryTotal(dataType: String, startDate: Double, endDate: Double, promise: Promise) {
        val endTime: Long = endDate.toLong()
        val startTime: Long = startDate.toLong()

        googleFitManager.queryTotal(promise, dataType, startTime, endTime)
    }

    @ReactMethod
    fun queryDailyTotals(dataType: String, startDate: Double, endDate: Double, promise: Promise) {
        val endTime: Long = endDate.toLong()
        val startTime: Long = startDate.toLong()

        googleFitManager.queryDailyTotals(
            promise,
            dataType,
            Date(startTime),
            Date(endTime)
        )
    }

    @ReactMethod
    fun getStatisticWeekDaily(dataType: String, promise: Promise) {
        googleFitManager.getStatisticWeekDaily(promise, dataType)
    }

    @ReactMethod
    fun getStatisticWeekTotal(dataType: String, promise: Promise) {
        googleFitManager.getStatisticWeekTotal(promise, dataType)
    }

    @ReactMethod
    fun getStatisticTodayTotal(dataType: String, promise: Promise) {
        googleFitManager.getStatisticTodayTotal(promise, dataType)
    }

    @ReactMethod
    fun getLatestDataRecord(dataType: String, promise: Promise) {
        googleFitManager.getLatestDataRecord(promise, dataType)
    }

    @ReactMethod
    fun writeWorkout(startTime: Double, endTime: Double, options: ReadableMap, promise: Promise) {
        googleFitManager.writeWorkout(
            promise,
            startTime.toLong(),
            endTime.toLong(),
            options
        )
    }

    @ReactMethod
    fun deleteWorkouts(startTime: Double, endTime: Double, promise: Promise) {
        googleFitManager.deleteWorkouts(
            promise,
            startTime.toLong(),
            endTime.toLong(),
        )
    }

    private fun createPermissionsFromReactArray(
        permissions: ReadableArray,
        access: Int,
        promise: Promise
    ): ArrayList<Permission> {
        val result: ArrayList<Permission> = ArrayList()
        val size = permissions.size()
        for (i in 0 until size) {
            try {
                val permissionKind = permissions.getString(i)

                result.add(Permission(PermissionKind.getByValue(permissionKind), access))
            } catch (e: NullPointerException) {
                promise.reject(e)
                e.printStackTrace()
            }
        }

        return result
    }

    companion object {
        const val E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST"
    }
}
