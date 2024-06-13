package aeh.kotlin.lokalizator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import aeh.kotlin.lokalizator.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    // Inicjalizacja zmiennych dla FirebaseAuth i ActivitySignupBinding
    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Sprawdzenie, czy użytkownik jest już zalogowany
        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Ustawienie słuchacza zdarzeń dla przycisku rejestracji
        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString()
            val password = binding.signupPassword.text.toString()
            val confirmPassword = binding.signupConfirm.text.toString()

            // Sprawdzenie, czy pola email, hasło i potwierdzenie hasła nie są puste
            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                // Sprawdzenie, czy hasła są zgodne
                if (password == confirmPassword) {
                    // Próba utworzenia nowego użytkownika za pomocą FirebaseAuth
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Przekierowanie do LoginActivity po udanej rejestracji
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Wyświetlenie komunikatu o błędzie rejestracji
                            Toast.makeText(this, "Rejestracja nie powiodła się: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Wyświetlenie komunikatu, gdy hasła nie są zgodne
                    Toast.makeText(this, "Hasła nie zgadzają się", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Wyświetlenie komunikatu, gdy pola email, hasło lub potwierdzenie hasła są puste
                Toast.makeText(this, "Wszystkie dane są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }

        // Ustawienie słuchacza zdarzeń dla tekstu przekierowującego do strony logowania
        binding.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}
