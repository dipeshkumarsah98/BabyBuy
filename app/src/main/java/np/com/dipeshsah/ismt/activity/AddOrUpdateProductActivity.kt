package np.com.dipeshsah.ismt.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.databinding.ActivityAddOrUpdateProductBinding

class AddOrUpdateProductActivity : AppCompatActivity() {
    private var TAG = "AddOrUpdateProduct"
    private lateinit var binding: ActivityAddOrUpdateProductBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOrUpdateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This is dropdown Adapter
        val items = listOf("Material", "Design", "Components", "Android")

        val arrayAdapter = ArrayAdapter(this, R.layout.list_item,items)
        binding.actvProductCategory.setAdapter(arrayAdapter)
    }
}