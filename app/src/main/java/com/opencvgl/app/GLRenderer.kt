package com.opencvgl.app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 Renderer for displaying processed camera frames
 */
class GLRenderer(private val context: android.content.Context) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "GLRenderer"
        
        // Vertex shader code
        private const val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                texCoord = vTexCoord;
            }
        """
        
        // Fragment shader code
        private const val fragmentShaderCode = """
            precision mediump float;
            varying vec2 texCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, texCoord);
            }
        """
        
        // Full screen quad vertices (x, y, s, t)
        private val quadVertices = floatArrayOf(
            // Position        // Texture coords
            -1.0f, -1.0f,      0.0f, 1.0f,  // Bottom left
             1.0f, -1.0f,      1.0f, 1.0f,  // Bottom right
            -1.0f,  1.0f,      0.0f, 0.0f,  // Top left
             1.0f,  1.0f,      1.0f, 0.0f   // Top right
        )
    }
    
    private var program: Int = 0
    private var textureHandle: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var textureHandleUniform: Int = 0
    
    private val vertexBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    
    private var textureWidth: Int = 0
    private var textureHeight: Int = 0
    private val textureDataLock = Any()
    private var pendingTextureData: ByteArray? = null
    private var pendingWidth: Int = 0
    private var pendingHeight: Int = 0
    
    init {
        // Initialize vertex buffer
        val bb = ByteBuffer.allocateDirect(quadVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(quadVertices)
        vertexBuffer.position(0)
        
        // Initialize MVP matrix to identity
        Matrix.setIdentityM(mvpMatrix, 0)
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // Load and compile shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        // Create shader program
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // Check linking status
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val infoLog = GLES20.glGetProgramInfoLog(program)
            android.util.Log.e(TAG, "Error linking program: $infoLog")
            GLES20.glDeleteProgram(program)
            return
        }
        
        // Get attribute/uniform locations
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandleUniform = GLES20.glGetUniformLocation(program, "uTexture")
        
        // Generate texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureHandle = textures[0]
        
        // Bind and configure texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        // Update MVP matrix for orthographic projection
        val ratio = width.toFloat() / height.toFloat()
        Matrix.orthoM(mvpMatrix, 0, -1.0f, 1.0f, -ratio, ratio, -1.0f, 1.0f)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // Update texture if new data is available
        synchronized(textureDataLock) {
            if (pendingTextureData != null) {
                updateTextureInternal(pendingTextureData!!, pendingWidth, pendingHeight)
                pendingTextureData = null
            }
        }
        
        // Use shader program
        GLES20.glUseProgram(program)
        
        // Set MVP matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
        GLES20.glUniform1i(textureHandleUniform, 0)
        
        // Enable vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        
        // Set vertex data
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4, // stride: 4 floats * 4 bytes
            vertexBuffer
        )
        
        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4, // stride: 4 floats * 4 bytes
            vertexBuffer
        )
        
        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        // Disable vertex attributes
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }
    
    /**
     * Update texture with new frame data (called from main thread)
     */
    fun updateTexture(data: ByteArray, width: Int, height: Int) {
        synchronized(textureDataLock) {
            pendingTextureData = data
            pendingWidth = width
            pendingHeight = height
        }
    }
    
    /**
     * Actually update the OpenGL texture (called from render thread)
     */
    private fun updateTextureInternal(data: ByteArray, width: Int, height: Int) {
        if (textureHandle == 0) return
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
        
        // Update texture if size changed
        if (textureWidth != width || textureHeight != height) {
            textureWidth = width
            textureHeight = height
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(data)
            )
        } else {
            // Update existing texture
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(data)
            )
        }
    }
    
    /**
     * Load and compile shader
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        // Check compilation status
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shader)
            android.util.Log.e(TAG, "Error compiling shader: $infoLog")
            GLES20.glDeleteShader(shader)
            return 0
        }
        
        return shader
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        if (textureHandle != 0) {
            val textures = intArrayOf(textureHandle)
            GLES20.glDeleteTextures(1, textures, 0)
            textureHandle = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }
}

