package np.com.dipeshsah.ismt.dashboard.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.activity.UpdateProfileActivity
import np.com.dipeshsah.ismt.databinding.FragmentProfileBinding
import np.com.dipeshsah.ismt.models.UserData
import np.com.dipeshsah.ismt.utils.FirebaseHelper


class ProfileFragment : Fragment() {
    private var TAG = "ProfileFragment"
    private  lateinit var binding: FragmentProfileBinding

    private val updateProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updateSuccess = result.data?.getBooleanExtra("updateSuccess", false) ?: false
            if (updateSuccess) {
                showSnackbar("Profile updated successfully!")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false);
        // get the userId from the storage
        val sharedPreference = activity?.getSharedPreferences("app", 0)
        val userId = sharedPreference?.getString("userId", null)

        userId?.let {
            getUserData(userId)
        }

        binding.bEdit.setOnClickListener {
            // navigate to Update profile Activity
            val profileUpdateIntent = Intent(activity, UpdateProfileActivity::class.java)
            profileUpdateIntent.putExtra("userId", userId)
           updateProfileLauncher.launch(profileUpdateIntent)

        }

        binding.bLogout.setOnClickListener {
            // remove the userId from the storage
            sharedPreference?.edit()?.remove("userId")?.apply()
            sharedPreference?.edit()?.remove("isLoggedIn")?.apply()
            sharedPreference?.edit()?.remove("userEmail")?.apply()

            // navigate to login activity
            activity?.finish()
        }


        return binding.root
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun getUserData(userId: String) {
        showProgressBar(true)
        val databaseReference = FirebaseHelper.getDatabaseReference("users/$userId")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserData::class.java)
                Log.e(TAG, "UserResponse: $user")
                binding.tvNameFieldValue.text = user?.name
                binding.tvProfileName.text = user?.name

                binding.tvEmailFieldValue.text = user?.email
                binding.tvProfileEmail.text = user?.email

                binding.tvGenderFieldValue.text = user?.gender

                // show no of start as same of password length
                binding.tvPasswordFieldValue.text = "*".repeat(user?.password?.length ?: 0)

                showProgressBar(false)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: ${error.message}")
                showProgressBar(false)
            }
        })
    }
   // logic to show progress bar
    private fun showProgressBar(isLoading: Boolean) {
        if(!isLoading) {
            binding.progressBar.visibility = View.GONE
            binding.clHeading.visibility = View.VISIBLE
            binding.llBody.visibility = View.VISIBLE
            binding.llButtonGroup.visibility = View.VISIBLE
        }
        else {
            binding.progressBar.visibility = View.VISIBLE
            binding.clHeading.visibility = View.GONE
            binding.llBody.visibility = View.GONE
            binding.llButtonGroup.visibility = View.GONE
        }
    }

}