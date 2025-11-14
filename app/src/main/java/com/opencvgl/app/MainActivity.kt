package com.opencvgl.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opencvgl.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
    
    // UI components
    private lateinit var textureView: TextureView
    private lateinit var glSurfaceView: android.opengl.GLSurfaceView
    private lateinit var fpsTextView: TextView
    private lateinit var resolutionTextView: TextView
    private lateinit var toggleButton: Button
    
    // Camera-related variables
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var cameraId: String? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    
    // Renderer
    private lateinit var renderer: GLRenderer

    // Processing-related variables
    private var isProcessingEnabled = false
    private var processMode = 0 // 0=raw, 1=grayscale, 2=canny
    private var frameCount = 0
    private var lastFpsTime = 0L
    
    // Native library methods
    private external fun nativeInit(width: Int, height: Int): Boolean
    private external fun nativeProcessFrame(data: ByteArray, width: Int, height: Int): ByteArray?
    private external fun nativeSetProcessMode(mode: Int)
    private external fun nativeRelease()
    
    // Load native library
    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("opencvgl")
    }
    
    // TextureView listener for camera preview
    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }
        
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Not used
        }
        
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Frame is available - capture for processing
            captureFrame()
        }
    }
    
    // Camera state callback
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
            Toast.makeText(this@MainActivity, "Camera error: $error", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Capture callback for repeating requests
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            // Frame captured - update FPS
            updateFPS()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize UI components
        textureView = binding.textureView
        glSurfaceView = binding.glSurfaceView
        fpsTextView = binding.fpsTextView
        resolutionTextView = binding.resolutionTextView
        toggleButton = binding.toggleButton
        
        // Setup OpenGL SurfaceView
        glSurfaceView.setEGLContextClientVersion(2)
        renderer = GLRenderer(this)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        
        // Setup toggle button
        toggleButton.setOnClickListener {
            isProcessingEnabled = !isProcessingEnabled
            processMode = if (isProcessingEnabled) {
                (processMode + 1) % 3 // Cycle: 0=raw, 1=grayscale, 2=canny
            } else {
                0
            }
            nativeSetProcessMode(processMode)
            toggleButton.text = when (processMode) {
                0 -> getString(R.string.raw_mode)
                1 -> getString(R.string.grayscale_mode)
                2 -> getString(R.string.canny_mode)
                else -> getString(R.string.toggle_processed)
            }
        }
        
        // Initialize camera manager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        // Setup texture view listener
        textureView.surfaceTextureListener = textureListener
        
        // Check and request permissions
        if (checkCameraPermission()) {
            // Permission already granted
        } else {
            requestCameraPermission()
        }
    }
    
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = textureListener
        }
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
    
    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request camera permission
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (textureView.isAvailable) {
                    openCamera()
                } else {
                    textureView.surfaceTextureListener = textureListener
                }
            } else {
                Toast.makeText(
                    this,
                    R.string.camera_permission_required,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Start background thread for camera operations
     */
    private fun startBackgroundThread() {
        Log.i(TAG, "Starting background thread")
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stop background thread
     */
    private fun stopBackgroundThread() {
        Log.i(TAG, "Stopping background thread")
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500) // Wait for half a second
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    /**
     * Open camera device
     */
    private fun openCamera() {
        if (!checkCameraPermission()) {
            requestCameraPermission()
            return
        }
        
        try {
            // Get first available camera
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e(TAG, "No cameras available")
                return
            }
            
            cameraId = cameraIds[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            
            // Get preview size
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(SurfaceTexture::class.java)
            val previewSize = sizes?.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
            
            imageWidth = previewSize.width
            imageHeight = previewSize.height
            
            // Initialize native code
            if (!nativeInit(imageWidth, imageHeight)) {
                Log.e(TAG, "Failed to initialize native code")
                return
            }
            
            // Update resolution display
            runOnUiThread {
                resolutionTextView.text = getString(R.string.resolution_text, imageWidth, imageHeight)
            }
            
            // Configure texture view
            textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            
            // Open camera
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager.openCamera(cameraId!!, cameraStateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error opening camera", e)
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error opening camera", e)
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Create camera capture session
     */
    private fun createCaptureSession() {
        try {
            val surfaceTexture = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(imageWidth, imageHeight)
            val surface = Surface(surfaceTexture)
            
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            captureRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            captureRequestBuilder?.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )
                            
                            val captureRequest = captureRequestBuilder?.build()
                            session.setRepeatingRequest(
                                captureRequest!!,
                                captureCallback,
                                backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Error starting capture session", e)
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to configure capture session",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }
    
    /**
     * Capture frame from TextureView for processing
     */
    private fun captureFrame() {
        if (!isProcessingEnabled || imageWidth == 0 || imageHeight == 0) {
            return
        }
        
        // Get bitmap from TextureView
        val bitmap = textureView.bitmap
        if (bitmap != null) {
            // Convert to YUV format (simplified - in real implementation, get YUV directly from ImageReader)
            // For now, we'll process the bitmap
            backgroundHandler?.post {
                processFrame(bitmap)
            }
        }
    }
    
    /**
     * Process frame through JNI to OpenCV
     */
    private fun processFrame(bitmap: android.graphics.Bitmap) {
        try {
            // Convert bitmap to byte array (RGBA format)
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to byte array (RGBA)
            val rgbaData = ByteArray(width * height * 4)
            var idx = 0
            for (pixel in pixels) {
                rgbaData[idx++] = ((pixel shr 16) and 0xFF).toByte() // R
                rgbaData[idx++] = ((pixel shr 8) and 0xFF).toByte()  // G
                rgbaData[idx++] = (pixel and 0xFF).toByte()           // B
                rgbaData[idx++] = ((pixel shr 24) and 0xFF).toByte() // A
            }
            
            // Process through JNI
            val processedData = nativeProcessFrame(rgbaData, width, height)
            
            // Update OpenGL renderer with processed data
            if (processedData != null) {
                renderer.updateTexture(processedData, width, height)
                glSurfaceView.requestRender()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        }
    }
    
    /**
     * Update FPS display
     */
    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            val fps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
            
            runOnUiThread {
                fpsTextView.text = getString(R.string.fps_text, fps)
            }
        }
    }
    
    /**
     * Close camera device
     */
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }
}
