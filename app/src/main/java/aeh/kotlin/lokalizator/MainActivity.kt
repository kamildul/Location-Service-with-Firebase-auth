package aeh.kotlin.lokalizator

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // Inicjalizacja FirebaseAuth
    private lateinit var firebaseAuth: FirebaseAuth

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CAMERA_PERMISSION = 2
    private val PROFILE_IMAGE_FILENAME = "profile_image.jpg"

    private lateinit var profileImageFile: File
    private lateinit var profileImage: ImageView

    companion object {
        // Stałe dla identyfikacji żądań uprawnień
        const val PERMISSION_REQUEST_FINE_LOCATION = 100
        const val PERMISSION_REQUEST_COARSE_LOCATION = 101
        const val PERMISSION_REQUEST_BACKGROUND_LOCATION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Ustawienie paska narzędzi
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Ustawienie przycisku śledzenia
        val startTrackingButton: Button = findViewById(R.id.startTrackingButton)
        updateButtonState(startTrackingButton)
        startTrackingButton.setOnClickListener {
            if (isServiceRunning(TrackingService::class.java)) {
                stopTracking()
                updateButtonState(startTrackingButton)
            } else {
                checkFineLocationPermission()
            }
        }

        // Inicjalizacja ImageView dla zdjęcia profilowego
        profileImage = findViewById(R.id.profileImage)
        profileImageFile = File(getExternalFilesDir(null), PROFILE_IMAGE_FILENAME)
        loadProfileImage()

        // Obsługa robienia zdjęcia profilowego
        val changeImageButton: ImageButton = findViewById(R.id.changeImageButton)
        changeImageButton.setOnClickListener {
            checkCameraPermission()
        }
        
    }

    // Aktualizacja zdjęcia profilowego
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            saveImageToExternalStorage(imageBitmap)
            profileImage.setImageBitmap(imageBitmap)
        }
    }

    // Zapisywanie zdjęcia do pamięci zewnętrznej
    private fun saveImageToExternalStorage(bitmap: Bitmap) {
        try {
            FileOutputStream(profileImageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Log.d("MainActivity", "Zapisano zdjęcie profilowe: ${profileImageFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("MainActivity", "Błąd podczas zapisywania zdjęcia: ${e.message}")
        }
    }

    // Ładowanie zdjęcia profilowego z pamięci zewnętrznej
    private fun loadProfileImage() {
        if (profileImageFile.exists()) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, profileImageFile.toUri())
            profileImage.setImageBitmap(bitmap)
            Log.d("MainActivity", "Załadowano zdjęcie profilowe z pamięci: ${profileImageFile.absolutePath}")
        } else {
            Log.d("MainActivity", "Nie znaleziono zdjęcia profilowego w pamięci: ${profileImageFile.absolutePath}")
        }
    }

    // Funkcja otwierająca aparat
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Brak aplikacji aparatu", Toast.LENGTH_SHORT).show()
        }
    }

    // Sprawdzenie uprawnień do obsługi kamery oraz zapisu
    private fun checkCameraPermission() {
        Log.d("MainActivity", "Sprawdzanie uprawnień do aparatu")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            Log.d("MainActivity", "Uprawnienia do aparatu lub zapisu nie zostały przyznane, żądanie uprawnień")

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            Log.d("MainActivity", "Uprawnienia do aparatu i zapisu już przyznane")
            openCamera()
        }
    }

    // Sprawdzanie uprawnień do dokładnej lokalizacji
    private fun checkFineLocationPermission() {
        Log.d("MainActivity", "Sprawdzanie uprawnień do ACCESS_FINE_LOCATION")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Uprawnienia ACCESS_FINE_LOCATION nie zostały przyznane, żądanie uprawnień")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_FINE_LOCATION
            )
        } else {
            Log.d("MainActivity", "Uprawnienia ACCESS_FINE_LOCATION już przyznane")
            checkCoarseLocationPermission()
        }
    }

    // Sprawdzanie uprawnień do przybliżonej lokalizacji
    private fun checkCoarseLocationPermission() {
        Log.d("MainActivity", "Sprawdzanie uprawnień do ACCESS_COARSE_LOCATION")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Uprawnienia ACCESS_COARSE_LOCATION nie zostały przyznane, żądanie uprawnień")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_COARSE_LOCATION
            )
        } else {
            Log.d("MainActivity", "Uprawnienia ACCESS_COARSE_LOCATION już przyznane")
            checkBackgroundLocationPermission()
        }
    }

    // Sprawdzanie uprawnień do lokalizacji w tle
    private fun checkBackgroundLocationPermission() {
        Log.d("MainActivity", "Sprawdzanie uprawnień do ACCESS_BACKGROUND_LOCATION")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Uprawnienia ACCESS_BACKGROUND_LOCATION nie zostały przyznane, żądanie uprawnień")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                PERMISSION_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            Log.d("MainActivity", "Uprawnienia ACCESS_BACKGROUND_LOCATION już przyznane")
            startTracking()
        }
    }

    // Obsługa wyników żądań uprawnień
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Potrzebujemy uprawnień do aparatu i zapisu, aby zrobić zdjęcie", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkCoarseLocationPermission()
                } else {
                    Toast.makeText(this, "Uprawnienia do lokalizacji są wymagane do rozpoczęcia śledzenia", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkBackgroundLocationPermission()
                } else {
                    Toast.makeText(this, "Uprawnienia do lokalizacji są wymagane do rozpoczęcia śledzenia", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_BACKGROUND_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startTracking()
                } else {
                    Toast.makeText(this, "Uprawnienia do lokalizacji są wymagane do rozpoczęcia śledzenia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Rozpoczynanie usługi śledzenia
    private fun startTracking() {
        val intent = Intent(this, TrackingService::class.java)
        startService(intent)
        val startTrackingButton: Button = findViewById(R.id.startTrackingButton)
        updateButtonState(startTrackingButton)
    }

    // Zatrzymywanie usługi śledzenia
    private fun stopTracking() {
        val intent = Intent(this, TrackingService::class.java)
        stopService(intent)
        val startTrackingButton: Button = findViewById(R.id.startTrackingButton)
        updateButtonState(startTrackingButton)
    }

    // Aktualizacja stanu przycisku śledzenia
    private fun updateButtonState(button: Button) {
        if (isServiceRunning(TrackingService::class.java)) {
            button.text = "Zatrzymaj śledzenie"
        } else {
            button.text = "Rozpocznij śledzenie"
        }
    }

    // Sprawdzanie, czy usługa śledzenia jest uruchomiona
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // Inicjalizacja menu opcji
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Obsługa wyboru elementów menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                firebaseAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
