package com.example.ryukisakuma.camerafilterview

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.telecom.VideoProfile
import android.util.Size
import android.view.Surface
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private var cameraId = ""
    private var manager: CameraManager? = null
    private var previewSize = Size(500, 500)
    private var cameraDevice: CameraDevice? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)

        manager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = manager!!.cameraIdList[0]
            val cameraCharacteristics = manager!!.getCameraCharacteristics(cameraId)
            val streamConfigurationMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if(permission != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }
            mSurfaceView.holder.setFixedSize(previewSize.width, previewSize.height)
            manager!!.openCamera(cameraId, stateCallback, null)
        }
        catch (e: CameraAccessException) {

        }
    }

    fun requestCameraPermission () {
        if(this.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this.baseContext)
                .setMessage("Permissoin Here")
                .setPositiveButton(android.R.string.ok) {_, _ ->
                    this.requestPermissions(arrayOf(Manifest.permission.CAMERA),200)
                }
                .setNegativeButton(android.R.string.cancel) {_, _ ->
                    this.finish()
                }
                .create()
        }
        else {
            this.requestPermissions(arrayOf(Manifest.permission.CAMERA), 200)
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@MainActivity.cameraDevice = camera
            val surfaceList = ArrayList<Surface>()
            surfaceList.add(mSurfaceView.holder.surface)

            try{
                previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder!!.addTarget(mSurfaceView.holder.surface)

                cameraDevice!!.createCaptureSession(surfaceList, cameraCaptureSessionCallback, null)
            }
            catch (e: CameraAccessException) {
                e.printStackTrace()
            }

        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            this@MainActivity.cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    val cameraCaptureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            if(cameraDevice == null) return
            this@MainActivity.captureSession = session
            try {
                previewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                previewRequest = previewRequestBuilder!!.build()
                val handler = Handler(backgroundThread!!.looper)
                captureSession!!.setRepeatingRequest(previewRequest!!, null, handler)
            }
            catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {

        }
    }
}
