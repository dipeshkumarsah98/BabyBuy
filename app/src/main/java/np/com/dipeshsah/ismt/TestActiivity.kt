package np.com.dipeshsah.ismt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.math.log

class TestActiivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            2000
        )
    }
}