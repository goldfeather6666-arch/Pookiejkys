package com.pixellab.suite.core.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

data class HardwareSensorSpec(
    val type: Int,
    val typeName: String,
    val name: String,
    val vendor: String,
    val version: Int,
    val resolution: Float,
    val maximumRange: Float,
    val powerMa: Float,
    val minDelayMicros: Int,
    val isWakeUpSensor: Boolean,
    val isDynamicSensor: Boolean
)

data class LiveSensorReading(
    val sensorType: Int,
    val sensorName: String,
    val timestampNanos: Long,
    val wallClockMillis: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val accuracy: Int
)

class SensorDiagnosticsProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun discoverAllSensors(): List<HardwareSensorSpec> {
        val list = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        return list.map { s ->
            HardwareSensorSpec(
                type = s.type,
                typeName = humanReadableSensorType(s.type, s.stringType),
                name = s.name,
                vendor = s.vendor.ifBlank { "Unknown Vendor" },
                version = s.version,
                resolution = s.resolution,
                maximumRange = s.maximumRange,
                powerMa = s.power,
                minDelayMicros = s.minDelay,
                isWakeUpSensor = s.isWakeUpSensor,
                isDynamicSensor = s.isDynamicSensor
            )
        }.sortedBy { it.typeName }
    }

    fun observeSensor(sensorType: Int, samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI): Flow<LiveSensorReading> = callbackFlow {
        val mgr = sensorManager
        val targetSensor = mgr?.getDefaultSensor(sensorType)
        if (mgr == null || targetSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val vx = event.values.getOrNull(0) ?: 0f
                val vy = event.values.getOrNull(1) ?: 0f
                val vz = event.values.getOrNull(2) ?: 0f
                val mag = sqrt(vx * vx + vy * vy + vz * vz)
                trySend(
                    LiveSensorReading(
                        sensorType = targetSensor.type,
                        sensorName = targetSensor.name,
                        timestampNanos = event.timestamp,
                        wallClockMillis = System.currentTimeMillis(),
                        x = vx,
                        y = vy,
                        z = vz,
                        magnitude = mag,
                        accuracy = event.accuracy
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        mgr.registerListener(listener, targetSensor, samplingPeriodUs)
        awaitClose {
            mgr.unregisterListener(listener)
        }
    }

    companion object {
        fun humanReadableSensorType(type: Int, fallback: String): String = when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
            Sensor.TYPE_LIGHT -> "Ambient Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_PRESSURE -> "Barometric Pressure"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_HINGE_ANGLE -> "Hinge Angle"
            else -> fallback.substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}
