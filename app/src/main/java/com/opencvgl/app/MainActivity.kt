package com.opencvgl.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opencvgl.app.databinding.ActivityMainBinding
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private lateinit var renderer: GLRenderer

    private var isProcessingEnabled = false
    private var processMode = 0
    private var frameCount = 0
    private var lastFpsTime = 0L

    private external fun nativeInit(width: Int, height: Int): Boolean
    private external fun nativeProcessFrame(yBuffer: ByteBuffer, uBuffer: ByteBuffer, vBuffer: ByteBuffer, yStride: Int, uStride: Int, vStride: Int, uvPixelStride: Int, width: Int, height: Int): ByteArray?
    private external fun nativeSetProcessMode(mode: Int)
    private external fun nativeRelease()

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireLatestImage() ?: return@OnImageAvailableListener
        backgroundHandler?.post {
            processImage(image)
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera error: $error")
            camera.close()
            cameraDevice = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("opencvgl")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native libraries", e)
            Toast.makeText(this, "Failed to load native libraries, the app will close.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.glSurfaceView.setEGLContextClientVersion(2)
        renderer = GLRenderer(this)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY

        binding.toggleButton.setOnClickListener {
            isProcessingEnabled = !isProcessingEnabled
            processMode = if (isProcessingEnabled) (processMode + 1) % 3 else 0
            nativeSetProcessMode(processMode)
            binding.toggleButton.text = when (processMode) {
                0 -> getString(R.string.raw_mode)
                1 -> getString(R.string.grayscale_mode)
                2 -> getString(R.string.canny_mode)
                else -> getString(R.string.toggle_processed)
            }
        }

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (checkCameraPermission()) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        openCamera()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        nativeRelease()
    }

    private fun checkCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
        backgroundThread = null
        backgroundHandler = null
    }

    private fun openCamera() {
        if (!checkCameraPermission() || isFinishing) return

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: run {
                Log.e(TAG, "No cameras available")
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = map?.getOutputSizes(ImageFormat.YUV_420_888)?.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

            imageWidth = previewSize.width
            imageHeight = previewSize.height

            imageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            if (!nativeInit(imageWidth, imageHeight)) {
                Log.e(TAG, "Failed to initialize native code")
                return
            }

            runOnUiThread { binding.resolutionTextView.text = getString(R.string.resolution_text, imageWidth, imageHeight) }
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
        }
    }

    private fun createCaptureSession() {
        try {
            val surface = imageReader?.surface ?: run {
                Log.e(TAG, "ImageReader surface is null")
                return
            }

            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply { addTarget(surface) }

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting capture session", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture session configuration failed")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }

    private fun processImage(image: android.media.Image) {
        if (!isProcessingEnabled) {
            image.close()
            return
        }

        val planes = image.planes
        val processedData = nativeProcessFrame(planes[0].buffer, planes[1].buffer, planes[2].buffer, planes[0].rowStride, planes[1].rowStride, planes[2].rowStride, planes[1].pixelStride, image.width, image.height)

        if (processedData != null) {
            renderer.updateTexture(processedData, image.width, image.height)
            binding.glSurfaceView.requestRender()
        }

        image.close()
        updateFPS()
    }

    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            val fps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
            runOnUiThread { binding.fpsTextView.text = getString(R.string.fps_text, fps) }
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera resources", e)
        }
    }
}
