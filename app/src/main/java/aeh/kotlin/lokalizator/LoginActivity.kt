package aeh.kotlin.lokalizator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import aeh.kotlin.lokalizator.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    // Inicjalizacja zmiennych dla FirebaseAuth i ActivityLoginBinding
    lateinit var binding: ActivityLoginBinding
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Sprawdzenie, czy użytkownik jest już zalogowany
        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Ustawienie słuchacza zdarzeń dla przycisku logowania
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            // Sprawdzenie, czy pola email i hasło nie są puste
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Próba zalogowania użytkownika za pomocą FirebaseAuth
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Przekierowanie do MainActivity po udanym logowaniu
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Wyświetlenie komunikatu o błędzie logowania
                        Toast.makeText(this, "Błąd logowania: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Wyświetlenie komunikatu, gdy pola email lub hasło są puste
                Toast.makeText(this, "Wszystkie dane są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }

        // Ustawienie słuchacza zdarzeń dla tekstu przekierowującego do strony rejestracji
        binding.signupRedirectText.setOnClickListener {
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
        }
    }

}
