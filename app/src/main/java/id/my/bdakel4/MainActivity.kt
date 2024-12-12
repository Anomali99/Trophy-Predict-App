package id.my.bdakel4

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.IOException
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageOverlay: ImageView
    private lateinit var uploadBtn: Button
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var detector: Detector
    private var imageCapture: ImageCapture? = null
    private var bitmap: Bitmap? = null
    private var camera: Camera? = null
    private val REQUEST_CAMERA_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        imageOverlay = findViewById(R.id.imageOverlay)
        uploadBtn = findViewById(R.id.btnUpload)

        detector = Detector(baseContext)
        detector.setup()

        uploadBtn.setOnClickListener {
            selectImage(it)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        } else {
            startCamera()  // Mulai kamera jika izin sudah diberikan
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Configure Preview
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Configure ImageCapture
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        displayImage(bitmap!!)
                    } catch (e: IOException) {
                        Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    fun selectImage(view: View) {
        if (bitmap == null) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            selectImageLauncher.launch(intent)
            uploadBtn.text = "Delete"
            stopCamera()
        } else {
            bitmap = null
            uploadBtn.text = "Upload"
            imageOverlay.setImageDrawable(null)
            imageOverlay.visibility = View.GONE
            startCamera()
        }
    }

    fun btnPredict(view: View) {
        if (bitmap == null) {
            captureFrame()
        } else {
            moveToResultActivity()
        }
    }

    private fun captureFrame() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            File(cacheDir, "temp_image.jpg")
        ).build()

        imageCapture?.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val filePath = outputFileResults.savedUri?.path
                        ?: File(cacheDir, "temp_image.jpg").absolutePath
                    bitmap = BitmapFactory.decodeFile(filePath)
                    displayImage(bitmap!!)
                    moveToResultActivity()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to capture image: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun displayImage(bitmap: Bitmap) {
        imageOverlay.setImageBitmap(bitmap)
        imageOverlay.visibility = View.VISIBLE
        uploadBtn.text = "Delete"
        stopCamera()
    }

    private fun stopCamera() {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }

    fun moveToResultActivity() {
        ResultStorage.detector = detector
        ResultStorage.bitmap = bitmap
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()  // Mulai kamera setelah izin diberikan
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

object ResultStorage {
    var detector: Detector? = null
    var bitmap: Bitmap? = null
}

