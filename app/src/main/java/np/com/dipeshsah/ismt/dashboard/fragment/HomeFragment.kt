package np.com.dipeshsah.ismt.dashboard.fragment

import AppConstants
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import np.com.dipeshsah.ismt.activity.AddOrUpdateProductActivity
import np.com.dipeshsah.ismt.activity.ProductListActivity
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentHomeBinding
import np.com.dipeshsah.ismt.dto.Categories
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData

class HomeFragment : Fragment() {
    private var TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding
    private lateinit var productAdapter: ProductAdapter

    private val addNewItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val addSuccess = result.data?.getBooleanExtra("addSuccess", false) ?: false
                if (addSuccess) {
                    showSnackbar("New item created!")
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
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false);
        // Inflate the layout for this fragment

        val images =
            "https://www.homechoice.co.za/file/v1370537154084896159/general/Baby+%26+Kids+-+Tippy+Toes+walker.jpg"
        val products = listOf(
            ProductData("1", "ABcd", "Baby", "toys", 100, image = images),
            ProductData("2", "Xyz", "Baby", "toys", 100, image = images),
            ProductData("3", "fgh", "Baby", "toys", 100, image =  images)
        )

        setUpRecycleView(products)

        binding.fabAdd.setOnClickListener {
            val createIntent = Intent(context, AddOrUpdateProductActivity::class.java)
            addNewItemLauncher.launch(createIntent)
        }

        binding.myproducts.setOnClickListener {
            redirectToProductList(null)
        }

        binding.toys.setOnClickListener {
            redirectToProductList(Categories.PLAYING)
        }
        binding.clothes.setOnClickListener {
            redirectToProductList(Categories.CLOTHES)
        }
        binding.milkItems.setOnClickListener {
            redirectToProductList(Categories.MILK)
        }
        binding.diaperr.setOnClickListener {
            redirectToProductList(Categories.DIAPER)
        }
        binding.travelling.setOnClickListener {
            redirectToProductList(Categories.TRAVELLING)
        }
        return binding.root
    }

    private fun redirectToProductList(category: Categories?) {
        var productListType: ProductListType? = null;

        when (category) {
            Categories.MILK -> productListType = ProductListType("Milk items", "milk items")
            Categories.DIAPER -> productListType = ProductListType("Diaper items", "diapers")
            Categories.CLOTHES -> productListType = ProductListType("Clothes items", "clothes")
            Categories.PLAYING -> productListType = ProductListType("Playing Items", "toys")
            Categories.TRAVELLING -> productListType =
                ProductListType("Travelling Items", "travelling")

            else -> {
                productListType = ProductListType("My Products", "myProducts")
            }
        }

        val productIntent = Intent(context, ProductListActivity::class.java)
        productIntent.putExtra(AppConstants.CATEGORY, productListType)
        startActivity(productIntent)
    }

    private fun setUpRecycleView(products: List<ProductData>) {
        binding.rvSuggestion.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(products)
        binding.rvSuggestion.adapter = productAdapter
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}