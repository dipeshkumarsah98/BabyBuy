package np.com.dipeshsah.ismt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.connection.ConnectionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.dashboard.Dashboard
import np.com.dipeshsah.ismt.databinding.ActivityTestActiivityBinding
import np.com.dipeshsah.ismt.utils.ConnectivityUtil

class TestActiivity : AppCompatActivity() {
    private var TAG = "TestActivity"
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var binding: ActivityTestActiivityBinding
    private lateinit var databaseReference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityTestActiivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().reference
        checkConnection()

        binding.bRefresh.setOnClickListener {
            checkConnection()
        }
    }

   private fun checkConnection(){
       binding.progressBar.visibility = View.VISIBLE
       binding.statusTextView.visibility = View.VISIBLE
       binding.statusTextView.text = "Checking internet connection..."

       coroutineScope.launch {
           val isConnectedToInterne = withContext(Dispatchers.IO){
               ConnectivityUtil.isConnectedToInternet(this@TestActiivity)
           }

           if(isConnectedToInterne){
               binding.statusTextView.text = "Internet connected. Checking firebase connectivity..."
               binding.progressBar.visibility = View.GONE
               checkFirebaseConnectivity()
           }else{
               binding.statusTextView.text = "No internet connection. Please check your internet connection and try again."
               binding.progressBar.visibility = View.GONE
               binding.bRefresh.visibility = View.VISIBLE
           }
       }
   }

    private fun checkFirebaseConnectivity(){
       coroutineScope.launch {
          withContext(Dispatchers.IO){
                databaseReference.child("test").setValue("test")
          }
           databaseReference.child("test").addListenerForSingleValueEvent(object :
               ValueEventListener {
               override fun onDataChange(snapshot: DataSnapshot) {
                   binding.statusTextView.text = "Firebase connected successfully!"
                   binding.progressBar.visibility = View.GONE
                   binding.bRefresh.visibility = View.GONE

                   val sharedPreferences = this@TestActiivity.getSharedPreferences("app", Context.MODE_PRIVATE)
                   val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

                   if(!isLoggedIn)
                   {
                       val intent = Intent(this@TestActiivity, LoginActivity::class.java)
                       startActivity(intent)
                       finish()
                   }else{
                       val intent = Intent(this@TestActiivity, Dashboard::class.java)
                       startActivity(intent)
                       finish()
                   }
               }

               override fun onCancelled(error: DatabaseError) {
                   binding.statusTextView.text = "Firebase connection failed: ${error.message}"
                   binding.progressBar.visibility = View.GONE
                   binding.bRefresh.visibility = View.VISIBLE
               }
           })
       }
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}