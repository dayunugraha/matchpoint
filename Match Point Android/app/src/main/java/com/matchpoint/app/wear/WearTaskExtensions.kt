package com.matchpoint.app.wear

import com.huawei.hmf.tasks.Task
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bridges Wear Engine's callback-based [Task] into a suspend call. */
suspend fun <T> Task<T>.await(): T {
    if (isComplete) {
        val error = exception
        return if (error != null) throw error else @Suppress("UNCHECKED_CAST") (result as T)
    }
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val error = exception
            if (error != null) {
                cont.resumeWithException(error)
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(result as T)
            }
        }
    }
}

/** Suspends until the user grants or denies every permission in [permissions]. */
suspend fun AuthClient.requestPermissionSuspend(permissions: Array<Permission>): Unit =
    suspendCancellableCoroutine { cont ->
        requestPermission(object : AuthCallback {
            override fun onOk(granted: Array<out Permission>) {
                if (!cont.isActive) return
                val notGranted = permissions.filterNot { requested -> granted.any { it.name == requested.name } }
                if (notGranted.isEmpty()) {
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(
                        IllegalStateException("Wear Engine permissions denied: ${notGranted.joinToString { it.name }}")
                    )
                }
            }

            override fun onCancel() {
                if (cont.isActive) {
                    cont.resumeWithException(IllegalStateException("Wear Engine permission request was canceled"))
                }
            }
        }, *permissions).addOnCompleteListener { task ->
            if (!task.isSuccessful && cont.isActive) {
                cont.resumeWithException(task.exception ?: IllegalStateException("Wear Engine permission request failed"))
            }
        }
    }
