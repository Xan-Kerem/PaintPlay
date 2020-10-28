package com.example.paintplay

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.paintplay.databinding.ActivityMainBinding
import com.example.paintplay.databinding.DialogBrushSizeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var currentPaintImageButton: ImageButton

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.drawingView.setSizeForBrush(5f)

        currentPaintImageButton = binding.paintColorsLl[1] as ImageButton

        currentPaintImageButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_selected
            )
        )

        binding.brushIb.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.galleryIb.setOnClickListener {
            if (isReadStorageAllowed()) {
                // run gallery code
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent, GALLERY)

            } else {
                requestStoragePermission()
            }
        }

        binding.undoIb.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.saveIb.setOnClickListener {

            if (isReadStorageAllowed()) {

                showProgressDialog()

                coroutineScope.launch {

                    val result = saveImageAsFile(getBitmapFromView(binding.flDrawingViewContainer))

                    if (result.isNotEmpty()) {

                        withContext(Dispatchers.Main) {

                            cancelProgressDialog()

                            Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()

                            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                                path, uri ->

                                val shareIntent = Intent()
                                shareIntent.action = Intent.ACTION_SEND
                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                                shareIntent.type = "image/png"

                                startActivity(Intent.createChooser(shareIntent, "Share"))
                            }

                        }
                    } else {
                        withContext(Dispatchers.Main) {

                            cancelProgressDialog()

                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong on saving image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            } else {
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data?.data != null) {
                        binding.backIv.setImageURI(data.data)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Something went wrong with loading image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)

        val dBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(dBinding.root)

        brushDialog.setTitle("Brush size")

        dBinding.smallBrushIb.setOnClickListener {
            binding.drawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        dBinding.mediumBrushIb.setOnClickListener {
            binding.drawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }
        dBinding.largeBrushIb.setOnClickListener {
            binding.drawingView.setSizeForBrush(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {

        if (view != currentPaintImageButton) {

            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()

            binding.drawingView.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_selected
                )
            )

            currentPaintImageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            currentPaintImageButton = view
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "need permission to add back image", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "oops u just denied the permission!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)

        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap
    }

    private fun saveImageAsFile(bitmap: Bitmap): String {

        try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

            val file =
                File(
                    externalCacheDir?.absoluteFile.toString() +
                            File.separator + "PlayPaint_" + System.currentTimeMillis() / 1000 + ".png"
                )

            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bytes.toByteArray())
            fileOutputStream.close()

            return file.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

    }

    private fun showProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog.setContentView(R.layout.dialog_custom_progress)
        progressDialog.show()
    }

    private fun cancelProgressDialog() {
        progressDialog.dismiss()
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}
