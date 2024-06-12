package np.com.dipeshsah.ismt.activity

import AppConstants
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.ActivityProductListBinding
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData

class ProductListActivity : AppCompatActivity() {
    private var TAG = "ProductList"
    private lateinit var binding: ActivityProductListBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var  firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        // setting up database
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("categories")

        var categoryDetail: ProductListType? = null

        // getting the props from home fragment
        if (intent.hasExtra(AppConstants.CATEGORY)){
            categoryDetail = intent.getParcelableExtra(AppConstants.CATEGORY)
        }

        if(categoryDetail != null){
            binding.tvTitle.text = categoryDetail.pageName
        }else{
            Log.w(TAG, "No product data found")
            handleEmptyProducts(false);
        }

        // fetching products with particular category
       categoryDetail?.let {
         fetchProducts(categoryDetail.category)
       }

        // TODO: Fix refresh button not working bug
        binding.bRefreshButton.setOnClickListener {
            // refreshing page when refresh button is pressed
            Log.i(TAG, "Refreshing page...")
            showToast("Refreshing page...")
            fetchProducts(categoryDetail!!.category)
        }

        binding.ibBack.setOnClickListener {
            finish()
        }


    }

    private fun fetchProducts(category: String) {
        getProducts(category ) { products ->
            if (products.isNotEmpty()) {
                binding.rvProductList.layoutManager = LinearLayoutManager(this)
                productAdapter = ProductAdapter(products)
                binding.rvProductList.adapter = productAdapter
                handleEmptyProducts(true)
            } else {
                handleEmptyProducts(false)
                Log.d(TAG, "No products found or an error occurred.")
            }
        }
    }
    private fun getProducts(category: String, callback: (List<ProductData>) -> Unit) {
        binding.clNoData.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        val productsReference = databaseReference.child(category)
        Log.i(TAG, "Fetching products with category $category")
        productsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<ProductData>()

                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key
                    val productData = productSnapshot.getValue(ProductData::class.java)

                    productData?.let {
                        val completeProductData = it.copy(productId = productId, category = category)
                        productList.add(completeProductData)
                    }
                }

                binding.progressBar.visibility = View.GONE
                callback(productList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
                Log.e("FirebaseError", "Error fetching data", error.toException())
                callback(emptyList()) // Returning an empty list in case of error
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun handleEmptyProducts (isVisible: Boolean) {
        if(isVisible){
            binding.rvProductList.visibility = View.VISIBLE
            binding.clNoData.visibility = View.GONE
        }else{
            binding.rvProductList.visibility = View.GONE
            binding.clNoData.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@ProductListActivity, message, Toast.LENGTH_SHORT).show()
    }
}