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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.activity.AddOrUpdateProductActivity
import np.com.dipeshsah.ismt.activity.ProductListActivity
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentHomeBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.dto.Categories
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.CategoryProductSwipeGesture
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class HomeFragment : Fragment(), ProductAdapter.OnItemClickListener {
    private var TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding
    private lateinit var productAdapter: ProductAdapter
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

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

        fetchSuggestedProducts()

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

        val swipeGesture = object : CategoryProductSwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        Log.i(TAG, "Left swipe")
                        productAdapter.addItem(position)
                        binding.rvSuggestion.adapter?.notifyItemChanged(position)
                    }

                    ItemTouchHelper.RIGHT -> {
                        Log.i(TAG, "Right swipe")
                        productAdapter.editItem(position)
                        binding.rvSuggestion.adapter?.notifyItemChanged(position)
                    }
                    else -> {
                        Log.i(TAG, "Invalid swipe")
                    }
                }
            }

        }
        val itemTouchHelper = ItemTouchHelper(swipeGesture)
        itemTouchHelper.attachToRecyclerView(binding.rvSuggestion)
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



    private fun fetchSuggestedProducts(){
        showProgressBar(true)
        coroutineScope.launch{
           try {
              val products = withContext(Dispatchers.IO) {
                  fetchAllProducts()
              }
               val suggestedProducts = products.shuffled().take(5)
               if(suggestedProducts.isNotEmpty()){
                   setUpRecycleView(suggestedProducts)
               }else{
                   Log.d(TAG, "No products found or an error occurred.")
               }
           }catch (e: Exception){
               Log.e(TAG, "Error occurred while fetching products: ${e.message}")
           }finally {
               showProgressBar(false)
           }
        }
    }

    suspend fun fetchAllProducts(): List<ProductData> {
        return suspendCoroutine { continuation ->
            FirebaseDatabaseHelper.getDatabaseReference("categories")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productList = mutableListOf<ProductData>()
                        for (categorySnapshot in snapshot.children) {
                            for (productSnapshot in categorySnapshot.children) {
                                val productId = productSnapshot.key
                                val productData = productSnapshot.getValue(ProductData::class.java)
                                productData?.let {
                                    val completeProductData = it.copy(productId = productId, category = categorySnapshot.key.toString())
                                    productList.add(completeProductData)
                                }
                            }
                        }
                        continuation.resume(productList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showProgressBar(isVisible: Boolean) {
        if (isVisible) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onProductDeleteClick(product: ProductData) {
        showSnackbar("Delete clicked")
    }

    override fun onProductAddClick(product: ProductData) {
        val intent = Intent(context, AddOrUpdateProductActivity::class.java)
        intent.putExtra("action", ActionType.ADDFROMCATEGORY)
        intent.putExtra("productId", product.productId)
        intent.putExtra("category", product.category)
        addNewItemLauncher.launch(intent)
    }
}