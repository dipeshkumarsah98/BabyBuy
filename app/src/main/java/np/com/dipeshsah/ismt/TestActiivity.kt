package np.com.dipeshsah.ismt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

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
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            },
            2000
        )
    }
}