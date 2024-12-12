package id.my.bdakel4

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ResnetDetector(
    private val context: Context,
    private val modelPath: String,
    private val classNames: List<String>
) {

    private var module: Module? = null

    fun setup(){
        module = Module.load(assetFilePath(context, modelPath));
    }

    fun classify(image: Bitmap): String {
        // Preprocess image
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            image,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        // Perform inference
        val outputTensor = module!!.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        // Get the index of the max score
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

        // Return the class name
        return if (maxIndex in classNames.indices) {
            classNames[maxIndex]
        } else {
            "Tidak Terdeteksi"
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String?): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while ((`is`.read(buffer).also { read = it }) != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

}