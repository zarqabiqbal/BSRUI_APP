package com.bsrui.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bsrui.app.Classifier.ImageClassifier
import kotlinx.android.synthetic.main.activity_cameralive.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraLiveActivity:AppCompatActivity(),LifecycleOwner {

    private val TAG="CameraLiveActivity"
    private val REQUEST_CODE_PERMISSIONS = 10
    // This is an array of all the permission specified in the manifest.
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var camera_view: TextureView

    private lateinit var imgClassifier: ImageClassifier

    private val cvHandler = Handler()
    private var textToShow = " "

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cameralive)
        imgClassifier = ImageClassifier(this)

        // Add this at the end of onCreate function

        camera_view = findViewById(R.id.camera_view)

        // Request camera permissions
        if (allPermissionsGranted()) {
            camera_view.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        camera_view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private fun startCamera() {
        // Create configuration object for the camera_view use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the camera_view use case
        val preview = Preview(previewConfig)

        // Every time the camera_view is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = camera_view.parent as ViewGroup
            parent.removeView(camera_view)
            parent.addView(camera_view, 0)

            camera_view.surfaceTexture = it.surfaceTexture
            updateTransform()
        }


        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)


        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = camera_view.parent as ViewGroup
            parent.removeView(camera_view)
            parent.addView(camera_view, 0)

            camera_view.surfaceTexture = it.surfaceTexture
            updateTransform()
            cvHandler.post(sendCV)
            cvHandler.post(sendTxt)
        }


        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, LuminosityAnalyzerLive())
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.


        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = camera_view.width / 2f
        val centerY = camera_view.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (camera_view.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        camera_view.setTransform(matrix)
    }

    //   Process result from permission request dialog box, has the request been granted? If yes, start Camera. Otherwise display a toast
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                camera_view.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    //   Check if all permission specified in the manifest have been granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        imgClassifier.close()
        cvHandler.removeCallbacks(sendCV)
        cvHandler.removeCallbacks(sendTxt)
    }

    override fun onStop() {
        super.onStop()
        cvHandler.removeCallbacks(sendCV)
        cvHandler.removeCallbacks(sendTxt)
    }

    override fun onResume() {
        super.onResume()
        if (camera_view.isAvailable) {
            cvHandler.post(sendCV)
            cvHandler.post(sendTxt)
        }
    }

    private val sendCV = object : Runnable {
        override fun run() {
            val bitmap: Bitmap = camera_view.getBitmap(
                imgClassifier.DIM_IMG_SIZE_X,
                imgClassifier.DIM_IMG_SIZE_Y
            )
            val (speed, Data) = imgClassifier.classifyFrame(bitmap)
            textToShow=""
            for (i in Data.keys) {
                textToShow = String.format("\n%s : %.2f%s", i, Data[i]!! * 100, "%") + textToShow
            }
            textToShow = speed + textToShow
//            result_viewlive.setText(textToShow)
            cvHandler.post(this)
        }

    }
    private val sendTxt=object:Runnable{
        override fun run() {
            result_viewlive.setText(textToShow)
            cvHandler.postDelayed(this,0)
        }

    }
}


    private class LuminosityAnalyzerLive : ImageAnalysis.Analyzer {
        private val TAG="LuminosityAnalyzerLive_CLA"
        private var lastAnalyzedTimestamp = 0L

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        @SuppressLint("LongLogTag")
        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)
            ) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                // Compute average luminance for the image
                val luma = pixels.average()
                // Log the new luma value
                Log.d(TAG, "Average luminosity: $luma")
                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
            }
        }

    }
