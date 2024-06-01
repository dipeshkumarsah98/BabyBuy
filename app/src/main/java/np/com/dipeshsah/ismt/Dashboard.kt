package np.com.dipeshsah.ismt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import np.com.dipeshsah.ismt.databinding.ActivityDashboardBinding

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
    }
}