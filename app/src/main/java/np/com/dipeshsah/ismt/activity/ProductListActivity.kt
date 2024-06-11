package np.com.dipeshsah.ismt.activity

import AppConstants
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.ActivityProductListBinding
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData

class ProductListActivity : AppCompatActivity() {
    private var TAG = "ProductList"
    private lateinit var binding: ActivityProductListBinding
    private lateinit var productAdapter: ProductAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        var productDetails: ProductListType? = null

        val images = arrayOf("https://www.homechoice.co.za/file/v1370537154084896159/general/Baby+%26+Kids+-+Tippy+Toes+walker.jpg")
        val products = listOf(
            ProductData("1", "This is a demo product", "Baby", 100, images),
            ProductData("2", "This is another demo product", "Baby", 100, images),
            ProductData("3", "Same goes for this one as well", "Baby", 100, images)
        )

        binding.rvProductList.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(products)
        binding.rvProductList.adapter = productAdapter

        if (intent.hasExtra(AppConstants.CATEGORY)){
            productDetails = intent.getParcelableExtra(AppConstants.CATEGORY)
            Log.i(TAG, "Data from intent is : $productDetails")
        }

        if(productDetails != null){
            binding.tvTitle.text = productDetails.pageName
        }

        binding.ibBack.setOnClickListener {
            finish()
        }
    }
}