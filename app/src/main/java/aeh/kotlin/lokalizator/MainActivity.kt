package aeh.kotlin.lokalizator

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    // Inicjalizacja FirebaseAuth
    private lateinit var firebaseAuth: FirebaseAuth

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
