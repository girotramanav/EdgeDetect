package com.example.edgedetect

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import com.example.edgedetect.databinding.ActivityPreviewBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.FileDescriptor
import java.io.IOException
import java.io.OutputStream

class PreviewActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPreviewBinding

    private lateinit var image : Bitmap
    private var rImage : Bitmap? = null
    private var uri : Uri? = null
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        supportActionBar?.hide()

        val fromUrl = intent.getBooleanExtra("fromUrl" , false)
        OpenCVLoader.initDebug()
        if(fromUrl){
            val imageByte = intent.getByteArrayExtra("imageByte")
            if (imageByte != null) {
                image = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.size)
                viewBinding.capturedImagePreview.setImageBitmap(image)
            }
        }
        else{
            uri = intent.extras?.get("imageUri") as Uri
            if(uri!=null)
            try {
                val parcelFileDescriptor = uri.let { contentResolver.openFileDescriptor(it!!, "r") }
                val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
                image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image = rotateImage(image)
                viewBinding.capturedImagePreview.setImageBitmap(image)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        viewBinding.detectEdge.setOnClickListener {
            detectEdges(image)
            viewBinding.buttonSaveImage.visibility = View.VISIBLE
        }

        viewBinding.buttonSaveImage.setOnClickListener {
            if(rImage!=null)
            if(!saved){
                saveImage(image , false)
                saveImage(rImage!! , true)
            }
            else Toast.makeText(this, "Image already Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectEdges(bitmap : Bitmap){
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)

        val edges = Mat(rgba.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4)
        Imgproc.Canny(edges, edges, 80.0, 100.0)

        val resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edges, resultBitmap)
        viewBinding.capturedImagePreview.setImageBitmap(resultBitmap)
        rImage = resultBitmap
        viewBinding.detectEdge.visibility = View.GONE
    }

    private fun rotateImage(bitmap : Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    override fun onDestroy() {
       if(uri!=null)
        try{
            contentResolver.delete(uri!! , null , null)
        }catch (e : SecurityException){
            var pendingIntent : PendingIntent? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val collection = arrayListOf<Uri>()
                collection.add(uri!!)
                pendingIntent = MediaStore.createDeleteRequest(contentResolver , collection)
            }
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                if (e is RecoverableSecurityException) {
                    pendingIntent = e.userAction.actionIntent
                }
            }

            if (pendingIntent != null) {
                val sender = pendingIntent.intentSender
                val request = IntentSenderRequest.Builder(sender).build()
                val launcher = registerForActivityResult(
                    StartIntentSenderForResult()
                ) { result: ActivityResult ->
                    if (result.resultCode == RESULT_OK) {
                        Log.i("ActivityResult" , "ImageDeleted")
                    }
                }
                launcher.launch(request)
            }
        }
        catch (e : Exception){
            Log.i("DeleteException" , "Uri not found")
        }
        super.onDestroy()
    }

    private fun saveImage(bitmap : Bitmap , isResult : Boolean){
        var filename = "ed-org-${System.currentTimeMillis()}.jpg"
        if(isResult)
            filename = "ed-result-${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/EdgeDetect")
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
               // Log.i("MyINFO-im" , imageUri?.path.toString())
            }
        } else {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                   //put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/EdgeDetect")
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
               // Log.i("MyINFO-im" , imageUri.toString())
            }
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this, "Saved Photo", Toast.LENGTH_SHORT).show()
            //Log.i("MyINFO" , uri?.path.toString())
            saved = true
        }
    }
}