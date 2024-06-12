package np.com.dipeshsah.ismt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.databinding.ActivityRegisterBinding
import np.com.dipeshsah.ismt.dto.FormKey
import np.com.dipeshsah.ismt.models.UserData


class RegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    private lateinit var  firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var TAG = "Register"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("users")

        // Click handler For Sign up button
        binding.bSubmit.setOnClickListener{
            val email = binding.tielEmail.text.toString()
            val password = binding.tietPassword.text.toString()
            val fullName = binding.tielFullName.text.toString()

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
                signupUser(fullName, email, password)
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

    private fun signupUser(name: String, email: String, password: String){
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(!dataSnapshot.exists()){
                    val id = databaseReference.push().key
                    val userData = UserData(id, name, email, password)
                    databaseReference.child(id!!).setValue(userData)
                    showToast("Signup Successful")
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()

                }
                else{
                    showToast("Email should be unique")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database Error: ${error.message}")
            }

        })

        /*
        val id = databaseReference.push().key
        val userData = UserData(id, name, email, password)

        if (id != null) {
            databaseReference.child(id).setValue(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Data write was successful
                    Log.i(TAG, "signupUser: user created")
                    showToast("Signup Successful")
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    // Data write failed
                    Log.w(TAG, "signupUser: Failed to create user", )
                }
            }
        }
*/
        /*
        databaseReference.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(UserData::class.java)
                    // Use the user object as needed
                    Log.i(TAG, "onDataChange: $user")
                }

            }

            override fun onCancelled(error: DatabaseError) {
                showToast("FirebaseError: ${error.message}")
            }

        })*/
    }

    private fun showToast(message: String?) {
        Toast.makeText(this@RegisterActivity, message.orEmpty(), Toast.LENGTH_SHORT).show()
    }
}