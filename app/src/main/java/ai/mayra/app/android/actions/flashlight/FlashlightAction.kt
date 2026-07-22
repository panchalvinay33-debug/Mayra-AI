package ai.mayra.app.android.actions.flashlight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import ai.mayra.app.android.actions.ActionResult
import ai.mayra.app.android.actions.AndroidAction


data class FlashlightRequest(
    val enabled: Boolean,
)

class FlashlightAction(
    private val context: Context,
    private val cameraManager: CameraManager =
        context.getSystemService(CameraManager::class.java),
) : AndroidAction<FlashlightRequest> {

    override val id: String = "device.flashlight"

    override suspend fun execute(request: FlashlightRequest): ActionResult {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return ActionResult.Unsupported("This device does not have a camera flash")
        }

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.PermissionRequired(
                permissions = setOf(Manifest.permission.CAMERA),
                reason = "Camera permission is required to control the flashlight",
            )
        }

        val cameraId = runCatching { findFlashCameraId() }
            .getOrElse { error ->
                return ActionResult.Failure(
                    message = error.message ?: "Unable to find a usable flashlight camera",
                    cause = error,
                )
            }
            ?: return ActionResult.Unsupported("No torch-capable camera was found")

        return runCatching {
            cameraManager.setTorchMode(cameraId, request.enabled)
            ActionResult.Success(
                message = if (request.enabled) {
                    "Flashlight turned on"
                } else {
                    "Flashlight turned off"
                },
                data = mapOf("enabled" to request.enabled.toString()),
            )
        }.getOrElse { error ->
            ActionResult.Failure(
                message = error.message ?: "Flashlight operation failed",
                recoverable = true,
                cause = error,
            )
        }
    }

    private fun findFlashCameraId(): String? =
        cameraManager.cameraIdList.firstOrNull { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
}
