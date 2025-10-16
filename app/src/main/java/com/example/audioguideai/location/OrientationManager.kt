package com.example.audioguideai.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class OrientationManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _azimuthFlow = MutableStateFlow(0f)
    val azimuthFlow: StateFlow<Float> = _azimuthFlow.asStateFlow()

    private var lastAzimuth = 0f
    private val threshold = 1f // Уменьшили порог для более плавного обновления

    fun start() {
        Log.d("OrientationManager", "Starting orientation tracking")
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("OrientationManager", "Accelerometer registered")
        } ?: Log.e("OrientationManager", "Accelerometer not available")

        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("OrientationManager", "Magnetometer registered")
        } ?: Log.e("OrientationManager", "Magnetometer not available")
    }

    fun stop() {
        Log.d("OrientationManager", "Stopping orientation tracking")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        updateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d("OrientationManager", "Sensor accuracy changed: ${sensor.name}, accuracy: $accuracy")
    }

    private fun updateOrientation() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Азимут в радианах, конвертируем в градусы
            var azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Нормализуем значение от 0 до 360
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360f
            }

            // Обновляем только если изменение достаточно большое
            if (abs(azimuthInDegrees - lastAzimuth) > threshold) {
                lastAzimuth = azimuthInDegrees
                _azimuthFlow.value = azimuthInDegrees
                Log.d("OrientationManager", "Azimuth updated: $azimuthInDegrees°")
            }
        }
    }
}