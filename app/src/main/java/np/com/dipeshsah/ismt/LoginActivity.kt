package np.com.dipeshsah.ismt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import np.com.dipeshsah.ismt.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private val TAG = "Login Activity"
    private lateinit var binding:ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        binding.bSubmit.setOnClickListener{
            val email = binding.tielEmail.text.toString()
            val password = binding.tietPassword.text.toString()

            if (isValidCredentials(email, password)) {
                // Here, you would typically call your authentication logic
                // If successful, navigate to the next screen
                navigateToHomeScreen()
            } else {
                showToast("Invalid email or password")
            }
        }

        /*
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (isValidCredentials(email, password)) {
                // Here, you would typically call your authentication logic
                // If successful, navigate to the next screen
                navigateToHomeScreen()
            } else {
                showToast("Invalid email or password")
            }
        }*/
    }
    private fun isValidCredentials(email: String, password: String): Boolean {
        // You can implement your validation logic here
        // For example, you might check if the email and password meet certain criteria
        // This is just a placeholder method, you should replace it with your actual logic
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private fun navigateToHomeScreen() {
        // Here you would navigate to the home screen of your app
        // For demonstration, let's just show a toast message
        showToast("Login successful!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }

    // Handle click on "Create Account" text
    fun createAccountClicked(view: android.view.View) {
        // You can start the registration activity here
        // For now, let's just show a toast message
        showToast("Create Account clicked")
    }

    // Handle click on "Forgot Password" text
    fun forgotPasswordClicked(view: android.view.View) {
        // You can implement forgot password functionality here
        // For now, let's just show a toast message
        showToast("Forgot Password clicked")
    }

}