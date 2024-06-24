package np.com.dipeshsah.ismt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.databinding.ActivityRegisterBinding
import np.com.dipeshsah.ismt.dto.FormKey
import np.com.dipeshsah.ismt.models.UserData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class RegisterActivity : AppCompatActivity() {
    private var TAG = "Register"
    private lateinit var binding:ActivityRegisterBinding
    private lateinit var  firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("users")

        // This is dropdown Adapter
        val items = listOf("Male", "Female", "Others")

        val arrayAdapter = ArrayAdapter(this, R.layout.list_item,items)
        binding.actvGender.setAdapter(arrayAdapter)

        binding.bSubmit.setOnClickListener{
            val email = binding.tielEmail.text.toString()
            val password = binding.tietPassword.text.toString()
            val fullName = binding.tielFullName.text.toString()
            val gender = binding.actvGender.text.toString()

            if(email.isEmpty()){
                validForm(FormKey.EMAIL, "Email is required")
            }

            if(fullName.isEmpty()){
                validForm(FormKey.NAME, "Full name is required")
            }

            if(password.isEmpty()){
                validForm(FormKey.PASSWORD, "Password is required")
            }

            if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty())
            {
                signupUser(fullName, email, password, gender)
            }

        }
        // Click handler for login page
        binding.tvLoginAccount.setOnClickListener {
            val loginIntent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(loginIntent);
        }

    }

    private fun validForm(key: FormKey, value: String){
       when (key){
           FormKey.EMAIL -> binding.tilEmail.error = value
           FormKey.NAME -> binding.tilFullName.error = value
           FormKey.PASSWORD -> binding.tilPassword.error = value
           else -> {}
       }
    }

    private fun signupUser(name: String, email: String, password: String, gender: String){
        coroutineScope.launch {
            showProgressBar(true)
            try {
                val userExists = withContext(Dispatchers.IO) {
                    checkIfUserExists(email)
                }
                if(!userExists){
                    val id = withContext(Dispatchers.IO) {
                        registerNewUser(name, email, password, gender)
                    }
                    showToast("Signup Successful")
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }else{
                    showToast("Email should be unique")
                    validForm(FormKey.EMAIL, "Email should be unique")
                    binding.tielEmail.requestFocus()
                }
            }catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
            finally {
                showProgressBar(false)
            }
        }
    }

    private suspend fun checkIfUserExists(email: String): Boolean {
        return suspendCoroutine { continuation ->
            databaseReference.orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    continuation.resume(dataSnapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    private suspend fun registerNewUser(name: String, email: String, password: String, gender: String): String {
        return suspendCoroutine { continuation ->
            val id = databaseReference.push().key!!
            val userData = UserData(id = id, name = name, email = email, password = password, gender = gender)
            databaseReference.child(id)
                .setValue(userData).addOnSuccessListener {
                continuation.resume(id)
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(this@RegisterActivity, message.orEmpty(), Toast.LENGTH_SHORT).show()
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