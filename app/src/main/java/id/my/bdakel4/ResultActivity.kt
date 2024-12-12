package id.my.bdakel4

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var listView: ListView
    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bitmap = ResultStorage.bitmap!!
        detector = ResultStorage.detector!!

        imageView = findViewById(R.id.imgView)
        listView = findViewById(R.id.listView)
        predict(bitmap)

    }

    fun predict(bitmap: Bitmap){
        Toast.makeText(this, "Predict", Toast.LENGTH_SHORT).show()
        val (result, classifyResult) = detector.predictAndClassify(bitmap)
        imageView.setImageBitmap(result)
        val customAdapter = CustomListAdapter(this, classifyResult)
        listView.adapter = customAdapter
    }

}