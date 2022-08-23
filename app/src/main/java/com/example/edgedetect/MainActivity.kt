package com.example.edgedetect

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.edgedetect.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        supportActionBar?.hide()

        viewBinding.captureCard.setOnClickListener{
            val intent = Intent(this , CaptureActivity::class.java)
            startActivity(intent)
        }

        viewBinding.cameraIcon.setOnClickListener{
            val intent = Intent(this , CaptureActivity::class.java)
            startActivity(intent)
        }

        viewBinding.uploadText.setOnClickListener {
            viewBinding.edittextImageUrl.visibility = if(viewBinding.edittextImageUrl.visibility==View.GONE) View.VISIBLE else View.GONE
            viewBinding.getImageButton.visibility = if(viewBinding.getImageButton.visibility==View.GONE) View.VISIBLE else View.GONE
        }

        viewBinding.getImageButton.setOnClickListener {
            getBitmap(viewBinding.edittextImageUrl.text.toString())
        }
    }

    private fun getBitmap(url : String){
        try{
            Glide.with(this)
                .asBitmap()
                .load(url)
                .into(object : CustomTarget<Bitmap>(400,400){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val intent = Intent(this@MainActivity , PreviewActivity::class.java)

                        val stream = ByteArrayOutputStream()
                        resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val bytes: ByteArray = stream.toByteArray()

                        intent.putExtra("fromUrl" , true)
                        intent.putExtra("imageByte" , bytes)
                        startActivity(intent)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }catch (e : Exception){
            Log.i("UrlException" , e.message.toString())
            Toast.makeText(this, "Error getting Image! Try again later", Toast.LENGTH_SHORT).show()
        }
    }
}