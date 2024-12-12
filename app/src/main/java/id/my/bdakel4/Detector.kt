package id.my.bdakel4

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect


class Detector(
    private val context: Context
) {
    private lateinit var yoloModel: YoloDetector
    private lateinit var resnetModel: ResnetDetector
    var boxPaint: Paint = Paint()
    var textPaint: Paint = Paint()
    var textBg: Paint = Paint()

     fun setup(){
         val classNames = listOf(
             "Piala Akademik",
             "Piala Ekstrakulikuler",
             "Piala Olahraga",
             "Plakat Karya Ilmiah",
             "Plakat STEM"
         )

         resnetModel = ResnetDetector(context,"resnet_model.ptl", classNames)
         resnetModel.setup()
         yoloModel = YoloDetector(context, "yolo_model.tflite", "yolo_labels.txt")
         yoloModel.setup()

         boxPaint.strokeWidth = 5f
         boxPaint.style = Paint.Style.STROKE
         boxPaint.color = Color.RED

         textBg.strokeWidth = 5f
         textBg.style = Paint.Style.FILL
         textBg.color = Color.RED

         textPaint.strokeWidth = 50f
         textPaint.style = Paint.Style.FILL
         textPaint.color = Color.WHITE
         textPaint.textSize = 50f
     }

    fun predict(bitmap: Bitmap): Bitmap {
        var bestBoxes: List<BoundingBox>? = yoloModel.detect(bitmap)

        var multableBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var canvas: Canvas = Canvas(multableBitmap)

        if (bestBoxes != null) {
            for (box in bestBoxes) {
                // Skala koordinat dari normalisasi (0-1) ke ukuran bitmap
                val left = box.x1 * bitmap.width
                val top = box.y1 * bitmap.height
                val right = box.x2 * bitmap.width
                val bottom = box.y2 * bitmap.height

                // Gambar bounding box
                canvas.drawRect(left, top, right, bottom, boxPaint)

                // Tambahkan label (jika ada) di dekat bounding box
                box.clsName?.let { label ->
                    canvas.drawText(label, left, top - 10, textPaint)
                }
            }
            return multableBitmap
        }

        return bitmap
    }

    fun predictAndCrop(bitmap: Bitmap): Pair<Bitmap, List<Bitmap>> {
        var bestBoxes: List<BoundingBox>? = yoloModel.detect(bitmap)
        val croppedBitmaps = mutableListOf<Bitmap>()

        var mutableBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var canvas: Canvas = Canvas(mutableBitmap)

        if (bestBoxes != null) {
            bestBoxes.forEachIndexed { index, box ->
                val left = (box.x1 * bitmap.width).toInt()
                val top = (box.y1 * bitmap.height).toInt()
                val right = (box.x2 * bitmap.width).toInt()
                val bottom = (box.y2 * bitmap.height).toInt()

                val text = (index + 1).toString()
                val bounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)

                // Padding untuk latar belakang teks
                val padding = 10f

                // Hitung posisi teks dan latar belakang
                val textBgRight = left + bounds.width() + (padding * 2)
                val textBgBottom = top + bounds.height() + (padding * 2)

                val textX = left + padding
                val textY = textBgBottom - padding

                // Gambar bounding box
                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), boxPaint)

                // Gambar latar belakang teks
                canvas.drawRect(left.toFloat(), top.toFloat(), textBgRight, textBgBottom, textBg)

                // Gambar teks di atas latar belakang
                canvas.drawText(text, textX, textY, textPaint)

                // Crop bagian gambar berdasarkan bounding box
                val croppedBitmap =
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                croppedBitmaps.add(croppedBitmap)
            }
        }
        // Kembalikan gambar dengan bounding box yang digambar, serta array hasil crop
        return Pair(mutableBitmap, croppedBitmaps)
    }

    fun predictAndClassify(bitmap: Bitmap): Pair<Bitmap, List<Pair<Bitmap, String>>> {
        val (mutableBitmap, croppedImages) = predictAndCrop(bitmap)
        val classifyResult = mutableListOf<Pair<Bitmap, String>>()

        for (img in croppedImages){
            val result = resnetModel.classify(img)
            classifyResult.add(Pair(img, result))
        }

        return Pair(mutableBitmap, classifyResult)
    }

}