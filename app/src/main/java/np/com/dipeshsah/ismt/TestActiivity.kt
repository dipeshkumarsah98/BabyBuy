package np.com.dipeshsah.ismt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import np.com.dipeshsah.ismt.dashboard.Dashboard

class TestActiivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(3000)
        installSplashScreen()
        setContentView(R.layout.activity_test_actiivity)

        // Navigating to another activity
        Handler().postDelayed(
            {
                // Fetching shared preference data
                val sharedPreferences = this@TestActiivity.getSharedPreferences("app", Context.MODE_PRIVATE)
                val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
                if(!isLoggedIn)
                {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    val intent = Intent(this, Dashboard::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            2000
        )

    }
}