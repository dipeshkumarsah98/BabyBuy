package np.com.dipeshsah.ismt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.dashboard.Dashboard
import np.com.dipeshsah.ismt.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private val TAG = "Login"
    private lateinit var binding:ActivityLoginBinding
    private lateinit var  firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

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
            } else {
                Log.w(TAG, "Bad email or password provided")
                showToast("Invalid email or password")
            }
        }

        binding.tvCreateAccount.setOnClickListener{
            createAccountClicked()
        }

    }
    private fun isValidData(email: String, password: String): Boolean {
        if(email.isEmpty()){
            validForm(FormKey.EMAIL, "Email is required")
        }

        if(password.isEmpty()){
            validForm(FormKey.PASSWORD, "Password is required")
        }
        // You can implement your validation logic here
        // For example, you might check if the email and password meet certain criteria
        // This is just a placeholder method, you should replace it with your actual logic
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private  fun loginUser(email: String, password: String){
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()){
                    for(userSnapshot in dataSnapshot.children){
                        val userData = userSnapshot.getValue(UserData::class.java)

                        Log.i(TAG, "User data is $userData")

                        if(userData != null && userData.password == password){
                            // For demonstration, let's just show a toast message
                            showToast("Login successful!")
                            val sharedPreference = this@LoginActivity.getSharedPreferences("app", Context.MODE_PRIVATE)
                            val editior = sharedPreference.edit()
                            editior.putBoolean("isLoggedIn", true)
                            editior.apply()
                            navigateToHomeScreen()
                            return
                        }
                    }
                }
                showToast("Login Failed")
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database Error: ${error.message}")
            }
        })
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

    // Handle click on "Create Account" text
    private fun createAccountClicked() {
        // You can start the registration activity here
        // For now, let's just show a toast message
        showToast("Create Account clicked")
        val registerIntent = Intent(this, RegisterActivity::class.java)
        startActivity(registerIntent);
    }

    // Handle click on "Forgot Password" text
    private fun forgotPasswordClicked(view: android.view.View) {
        // You can implement forgot password functionality here
        // For now, let's just show a toast message
        showToast("Forgot Password clicked")
    }

    private fun validForm(key: FormKey, value: String){
        when (key){
            FormKey.EMAIL -> binding.tilEmail.error = value
            FormKey.PASSWORD -> binding.tilPassword.error = value
            else -> {}
        }
    }
}