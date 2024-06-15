package np.com.dipeshsah.ismt

import android.app.Application
import np.com.dipeshsah.ismt.utils.FirebaseHelper

// This class will be used to initialize Firebase
class MainActivity : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseHelper.initialize()
    }
}