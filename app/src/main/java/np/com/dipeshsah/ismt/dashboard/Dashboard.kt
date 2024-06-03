package np.com.dipeshsah.ismt.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.dashboard.fragment.HomeFragment
import np.com.dipeshsah.ismt.dashboard.fragment.MyItemsFragment
import np.com.dipeshsah.ismt.dashboard.fragment.ProfileFragment
import np.com.dipeshsah.ismt.databinding.ActivityDashboardBinding

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private var fragmentManager = supportFragmentManager
    private var homeFragment= HomeFragment()
    private var myItemsFragment= MyItemsFragment()
    private var profileFragment= ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        loadRespectiveFragment(homeFragment);

        binding.bottomNav.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.home -> {
                    loadRespectiveFragment(homeFragment);
                    true}
                R.id.myItems -> {
                    loadRespectiveFragment(myItemsFragment);
                    true
                }
                R.id.profile -> {
                    loadRespectiveFragment(profileFragment);
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun loadRespectiveFragment(fragment: Fragment){
        fragmentManager.beginTransaction()
            .replace(
            binding.fcvWrapper.id,
            fragment,
            null
                    )
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
    }

}