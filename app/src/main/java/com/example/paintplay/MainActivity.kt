package com.example.paintplay

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var currentPaintImageButton: ImageButton

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

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}
