package ai.mayra.app.device

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.biometrics.BiometricManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings

interface DeviceStateProvider {
    fun capture(): DeviceSnapshot
}

class AndroidDeviceStateProvider(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) : DeviceStateProvider {
    private val appContext = context.applicationContext

    override fun capture(): DeviceSnapshot = DeviceSnapshot(
        capturedAt = now(),
        battery = readBattery(),
        storage = readStorage(),
        memory = readMemory(),
        thermal = readThermal(),
        network = readNetwork(),
        capabilities = readCapabilities()
    )

    private fun readBattery(): BatterySnapshot {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }?.div(10.0)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthState.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthState.OVERHEATED
            else -> when {
                temperature != null && temperature >= 45 -> BatteryHealthState.HOT
                temperature != null && temperature >= 40 -> BatteryHealthState.WARM
                else -> BatteryHealthState.UNKNOWN
            }
        }
        val manager = appContext.getSystemService(BatteryManager::class.java)
        val remaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.computeChargeTimeRemaining().takeIf { it > 0 }?.div(60_000L)?.toInt()
        } else null
        return BatterySnapshot(
            levelPercent = percent,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS
                else -> if (plugged == 0) ChargingSource.NONE else ChargingSource.UNKNOWN
            },
            temperatureCelsius = temperature,
            voltageMillivolts = voltage,
            health = health,
            powerSaveMode = appContext.getSystemService(PowerManager::class.java).isPowerSaveMode,
            estimatedMinutesToFull = remaining
        )
    }

    private fun readStorage(): StorageSnapshot {
        val root = Environment.getDataDirectory()
        val stats = StatFs(root.absolutePath)
        val total = stats.totalBytes.coerceAtLeast(0)
        return StorageSnapshot(
            totalBytes = total,
            freeBytes = stats.availableBytes.coerceIn(0, total),
            cacheBytes = runCatching {
                appContext.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrNull()
        )
    }

    private fun readMemory(): MemorySnapshot {
        val manager = appContext.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        val total = info.totalMem.coerceAtLeast(0)
        return MemorySnapshot(
            totalBytes = total,
            availableBytes = info.availMem.coerceIn(0, total),
            lowMemory = info.lowMemory
        )
    }

    private fun readThermal(): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.UNKNOWN
        return when (appContext.getSystemService(PowerManager::class.java).currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.SHUTDOWN
            else -> ThermalState.UNKNOWN
        }
    }

    private fun readNetwork(): NetworkSnapshot {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork
            ?: return NetworkSnapshot(false, false, NetworkTransport.NONE, manager.isActiveNetworkMetered)
        val caps = manager.getNetworkCapabilities(network)
            ?: return NetworkSnapshot(true, false, NetworkTransport.OTHER, manager.isActiveNetworkMetered)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransport.BLUETOOTH
            else -> NetworkTransport.OTHER
        }
        return NetworkSnapshot(
            connected = true,
            validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            transport = transport,
            metered = manager.isActiveNetworkMetered,
            downstreamKbps = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 },
            upstreamKbps = caps.linkUpstreamBandwidthKbps.takeIf { it > 0 }
        )
    }

    @Suppress("DEPRECATION")
    private fun readCapabilities(): DeviceCapabilities {
        val pm = appContext.packageManager
        val sensors = appContext.getSystemService(SensorManager::class.java)
        val notificationDeclared = runCatching {
            pm.getServiceInfo(
                android.content.ComponentName(appContext, "ai.mayra.app.background.MayraNotificationListener"),
                0
            )
            true
        }.getOrDefault(false)
        return DeviceCapabilities(
            camera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            cameraFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
            bluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            telephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            microphone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null,
            stepCounter = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null,
            biometric = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appContext.getSystemService(BiometricManager::class.java)?.canAuthenticate() ==
                    BiometricManager.BIOMETRIC_SUCCESS
            } else false,
            notificationListenerDeclared = notificationDeclared,
            exactAlarmDeclared = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else true,
            writeSystemSettingsGranted = Settings.System.canWrite(appContext)
        )
    }
}

class DeviceActionIntentFactory(private val context: Context) {
    fun intentFor(action: DeviceAction): Intent? = when (action) {
        DeviceAction.OPEN_BATTERY_SETTINGS,
        DeviceAction.SUGGEST_CHARGING -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        DeviceAction.OPEN_STORAGE_SETTINGS,
        DeviceAction.SUGGEST_CLEANUP -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        DeviceAction.OPEN_WIFI_SETTINGS,
        DeviceAction.RETRY_WHEN_CONNECTED -> Intent(Settings.ACTION_WIFI_SETTINGS)
        DeviceAction.OPEN_DATA_SETTINGS -> Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
        DeviceAction.OPEN_APP_SETTINGS -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")
        )
        DeviceAction.SUGGEST_COOLDOWN,
        DeviceAction.DEFER_HEAVY_WORK -> null
    }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
