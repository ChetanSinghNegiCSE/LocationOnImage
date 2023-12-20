package com.example.drawonimage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.drawonimage.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val takePicture: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val imageBitmap: Bitmap = data?.extras?.get("data") as Bitmap
                getCurrentLocation(imageBitmap)

            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, dispatchTakePictureIntent
                //dispatchTakePictureIntent()
            } else {
                // Permission denied
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, launch the camera
                //dispatchTakePictureIntent()
            } else {
                // Permission denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


       /* // Update time and date every second
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                //displayDateTime()
                handler.postDelayed(this, 1000)
            }
        })
*/
        requestCameraPermission()
        checkAndEnableGPS()


        binding.button.setOnClickListener {
            requestLocationPermission()
            dispatchTakePictureIntent()
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch the camera
                //dispatchTakePictureIntent()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            ) -> {
                requestCameraPermission()
            }

            else -> {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePicture.launch(takePictureIntent)
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
               // dispatchTakePictureIntent()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                requestLocationPermission()
            }
            else -> {
                // No explanation needed; request the permission
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun displayLocationOnCanvas(location: android.location.Location, bitmap: Bitmap) {
        // Create a mutable copy of the bitmap
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        // Create a Canvas object to draw on the bitmap
        val canvas = Canvas(mutableBitmap)
        // Create a Paint object for drawing text
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isDither = true
            textAlign = Paint.Align.LEFT
        }
        // Get current date and time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateAndTime: String = dateFormat.format(Date())
        // Get address from coordinates
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address> =
            geocoder.getFromLocation(location.latitude, location.longitude, 1)!!
        // Draw current location on the bitmap
        val locationText = "Lat Lag: ${location.latitude}, ${location.longitude}"
        val x = 5f
        var y = 200f
        canvas.drawText(locationText, x, y, paint)
        // Draw address on the bitmap
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            val maxLineLength = 40 // Set your desired maximum line length
            val addressLines = splitAddressLines(address.getAddressLine(0), maxLineLength)

            for (line in addressLines) {
                y += 10f // Move down for each line of the address
                canvas.drawText(line, x, y, paint)
            }
        }
        val dateTime = currentDateAndTime
        val x3 = 10f
        val y3 = 250f
        canvas.drawText(dateTime, x3, y3, paint)
        // Set the modified bitmap to the ImageView
        binding.imageView.setImageBitmap(mutableBitmap)
        binding.imageView.visibility = ImageView.VISIBLE
    }
    private fun getCurrentLocation(imageBitmap: Bitmap) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: android.location.Location? ->
                    location?.let {
                        // Location retrieved successfully, display on Canvas
                        displayLocationOnCanvas(it, imageBitmap)
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to retrieve location
                    e.printStackTrace()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun checkAndEnableGPS() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is not enabled, show a dialog to enable it
            showPermissionSettingsDialog()
        } else {
            // GPS is already enabled
            // Proceed with location-related tasks or any other logic
        }
    }

    private fun showPermissionSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
            .setMessage("Location permission is required for the app to function. Please enable it in the app settings.")
            .setPositiveButton("Settings") { dialog, which ->
                showEnableGPSDialog()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle the case where the user cancels the dialog
            }
            .show()
    }

    private fun showEnableGPSDialog() {
        val enableGpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(enableGpsIntent)
    }

    private fun splitAddressLines(address: String, maxLineLength: Int): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in address.split(" ")) {
            if (currentLine.length + word.length + 1 <= maxLineLength) {
                // Add the word to the current line
                if (currentLine.isNotEmpty()) {
                    currentLine += " "
                }
                currentLine += word
            } else {
                // Start a new line
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}





