package com.fitnesstracker.googlefit

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.*
import com.fitnesstracker.permission.Permission
import com.fitnesstracker.permission.PermissionKind
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType


class GoogleFitManager(private val reactContext: ReactApplicationContext) : ActivityEventListener {
    private val recordingApi: RecordingApi = RecordingApi(reactContext)
    private var authorized = false
    private var historyClient: HistoryClient? = null
    private var authorisationPromise: Promise? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun onNewIntent(intent: Intent?) {}
    override fun onActivityResult(
        activity: Activity?,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                accessGoogleFit()
            } else {
                authorisationPromise?.resolve(false)
            }
        }
    }

    fun authorize(promise: Promise, activity: Activity, permissions: ArrayList<Permission>) {
        try {
            authorisationPromise = promise

            if (authorized) {
                accessGoogleFit()
            } else {
                if (permissions.find { it.permissionKind == PermissionKind.STEPS } !== null) {
                    /** Subscribes to tracking steps even if google fit is not installed */
                    // Todo: test if this works
                    recordingApi.subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                }

                val fitnessOptionsBuilder: FitnessOptions.Builder = FitnessOptions.builder()

                for (permission in permissions) {
                    for (dataType in permission.dataTypes) {
                        fitnessOptionsBuilder.addDataType(
                            dataType,
                            permission.permissionAccess
                        )
                    }
                }

                val fitnessOptions = fitnessOptionsBuilder.build()
                val googleAccount =
                    GoogleSignIn.getAccountForExtension(reactContext, fitnessOptions)

                GoogleSignIn.requestPermissions(
                    activity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    googleAccount,
                    fitnessOptions
                )
            }
        } catch (e: Exception) {
            promiseException(authorisationPromise, e)
        }
    }

    fun isAuthorized(): Boolean {
        return authorized
    }

    fun isTrackingAvailable(
        promise: Promise,
        permissions: ArrayList<Permission>
    ) {
        try {
            val fitnessOptionsBuilder: FitnessOptions.Builder = FitnessOptions.builder()
            for (permission in permissions) {
                for (dataType in permission.dataTypes) {
                    fitnessOptionsBuilder.addDataType(
                        dataType,
                        permission.permissionAccess
                    )
                }
            }
            val fitnessOptions = fitnessOptionsBuilder.build()

            val googleAccount =
                GoogleSignIn.getAccountForExtension(reactContext, fitnessOptions)

            val hasPermissions = GoogleSignIn.hasPermissions(
                googleAccount,
                fitnessOptions
            )

            if (hasPermissions && !authorized) {
                accessGoogleFit(false)
            }

            promise.resolve(hasPermissions)
        } catch (e: Exception) {
            promiseException(promise, e)
        }
    }

    private fun accessGoogleFit(resolvePromise: Boolean = true) {
        try {
            authorized = true
            historyClient = HistoryClient(reactContext)

            if (resolvePromise) authorisationPromise!!.resolve(true)
        } catch (e: Exception) {
            promiseException(authorisationPromise, e)
        }
    }

    fun getHistoryClient(): HistoryClient? {
        return historyClient
    }

    private fun promiseException(promise: Promise?, e: Exception) {
        promise!!.reject(e)
        e.printStackTrace()
    }

    companion object {
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 111
    }
}
