package np.com.dipeshsah.ismt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.dashboard.Dashboard
import np.com.dipeshsah.ismt.databinding.ActivityLoginBinding
import np.com.dipeshsah.ismt.dto.FormKey
import np.com.dipeshsah.ismt.models.UserData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private lateinit var binding:ActivityLoginBinding
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        val sharedPreference = this@LoginActivity.getSharedPreferences("app", Context.MODE_PRIVATE)

       val isLoggedIn = sharedPreference.getBoolean("isLoggedIn", false)
        Log.i(TAG, "IsLoggedIn:: $isLoggedIn")

        if(isLoggedIn) {
            showToast("User already logged in")
            navigateToHomeScreen()
        }

        binding.bSubmit.setOnClickListener{
            val email = binding.tielEmail.text.toString()
            val password = binding.tietPassword.text.toString()
            if (isValidData(email, password)) {
                loginUser(email,password)
            }
        }

        binding.loadingOverlay.setOnTouchListener { v, event ->
            true
        }

        binding.tvCreateAccount.setOnClickListener{
            createAccountClicked()
        }

    }
    private fun isValidData(email: String, password: String): Boolean {
        if(email.isEmpty()){
            validForm(FormKey.EMAIL, "Email is required")
        }
        // check if email is valid format or not
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            validForm(FormKey.EMAIL, "Invalid email format")
            // move cursor to email field
            binding.tielEmail.requestFocus()
        }

        if(password.isEmpty()){
            validForm(FormKey.PASSWORD, "Password is required")
        }
        return email.isNotEmpty() && password.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private  fun loginUser(email: String, password: String){
        coroutineScope.launch {
           try {
               showProgressBar(true)
              val userData = withContext(Dispatchers.IO) {
                  fetchUserByEmail(email)
              }
               if(userData != null && userData.password == password) {
                   val sharedPreference = getSharedPreferences("app", Context.MODE_PRIVATE)
                   val editor = sharedPreference.edit()
                   editor.putBoolean("isLoggedIn", true)
                   editor.putString("userId", userData.id)
                   editor.putString("userEmail", userData.email)
                   editor.apply()
                   navigateToHomeScreen()
               }else{
                   showToast("Invalid email or password")
                   // clear password field
                     binding.tietPassword.text?.clear()
               }
           }catch (e: Exception) {
               Log.e(TAG, "Error: ${e.message}")
               showToast("Database Error: ${e.message}")
           } finally {
               showProgressBar(false)
           }
        }
    }

    private suspend fun fetchUserByEmail(email: String): UserData? {
        return suspendCoroutine { continuation ->
            FirebaseDatabaseHelper.getDatabaseReference("users")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (userSnapshot in dataSnapshot.children) {
                        val userData = userSnapshot.getValue(UserData::class.java)
                        if (userData != null) {
                            continuation.resume(userData)
                            return
                        }
                    }
                    continuation.resume(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    private fun navigateToHomeScreen() {
        // Here you would navigate to the home screen of your app
        // moving to dashboard
        startActivity(Intent(this@LoginActivity, Dashboard::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun createAccountClicked() {
        val registerIntent = Intent(this, RegisterActivity::class.java)
        startActivity(registerIntent);
    }

    private fun forgotPasswordClicked(view: android.view.View) {
        showToast("Forgot Password clicked")
    }

    private fun validForm(key: FormKey, value: String){
        when (key){
            FormKey.EMAIL -> binding.tilEmail.error = value
            FormKey.PASSWORD -> binding.tilPassword.error = value
            else -> {}
        }
    }

    private fun showProgressBar(isLoading: Boolean) {
        if(isLoading) {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.bSubmit.isEnabled = false
            binding.loadingOverlay.bringToFront()
            binding.loadingOverlay.invalidate()
        } else {
            binding.loadingOverlay.visibility = View.GONE
            binding.bSubmit.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}