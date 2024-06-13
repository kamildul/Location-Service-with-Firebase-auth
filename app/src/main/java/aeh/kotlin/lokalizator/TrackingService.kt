package aeh.kotlin.lokalizator

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class TrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "TrackingServiceChannel"
    }

    private var startTime: Long = 0
    private lateinit var notificationManager: NotificationManager
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()

        // Inicjalizacja Firebase Authentication i FusedLocationProviderClient
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Tworzenie kanału powiadomień dla usługi
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Tworzenie i uruchamianie powiadomienia w trybie foreground
        val notification = createNotification()
        startForeground(1, notification)

        // Rozpoczęcie śledzenia czasu i lokalizacji
        startTrackingTime()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Powiadomienie o zatrzymaniu usługi śledzenia
        Toast.makeText(this, "Serwis śledzenia zatrzymany", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Tworzenie kanału powiadomień dla usługi
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // Tworzenie powiadomienia dla usługi śledzenia lokalizacji
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Śledzenie lokalizacji")
            .setContentText("Trwa zapis Twojej lokalizacji")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .build()
    }

    // Rozpoczęcie śledzenia czasu i wysyłanie danych lokalizacji
    private fun startTrackingTime() {
        startTime = System.currentTimeMillis()
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val user = auth.currentUser
                if (user != null) {
                    val email = user.email
                    if (email != null) {
                        try {
                            // Pobieranie ostatniej znanej lokalizacji użytkownika
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        val lat = location.latitude
                                        val lon = location.longitude
                                        sendLocationData(email, lat, lon) // Wysyłanie danych lokalizacji
                                    } else {
                                        Log.e("LOCATION_ERROR", "Location is null")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LOCATION_ERROR", "Failed to get location", e)
                                }
                        } catch (e: SecurityException) {
                            Log.e("LOCATION_ERROR", "Permission not granted", e)
                        }
                    }
                }
            }
        }, 0, 60000) // Wykonywanie co 60 sekund
    }

    // Wysyłanie danych lokalizacji do serwera
    private fun sendLocationData(email: String, lat: Double, lon: Double) {
        val url = "http://aeh-lokalizator.you2.pl/api.php"
        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                Log.d("API_RESPONSE", response)
            },
            Response.ErrorListener { error ->
                Log.e("API_ERROR", error.toString())
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                params["lat"] = lat.toString()
                params["lon"] = lon.toString()
                return params
            }
        }
        requestQueue.add(stringRequest)
    }

}
