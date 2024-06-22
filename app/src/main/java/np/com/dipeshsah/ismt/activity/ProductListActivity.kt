package np.com.dipeshsah.ismt.activity

import AppConstants
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.adapters.MyProductAdapter
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.ActivityProductListBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.SwipeGesture

class ProductListActivity : AppCompatActivity(), ProductAdapter.OnItemClickListener, MyProductAdapter.OnItemClickListener {
    private var TAG = "ProductList"
    private lateinit var binding: ActivityProductListBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var myProductAdapter: MyProductAdapter
    private lateinit var  firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String

    private val updateItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val addSuccess = result.data?.getBooleanExtra("addSuccess", false) ?: false
                val updateSuccess = result.data?.getBooleanExtra("updateSuccess", false) ?: false
                if (updateSuccess) {
                    showToast("Product updated successfully!")
                }else if(addSuccess){
                    showToast("Product added successfully!")
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        // setting up database
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("categories")

        var categoryDetail: ProductListType? = null

        val sharedPreferences = this@ProductListActivity.getSharedPreferences("app", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", null).toString()

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

        val swipeGesture = object : SwipeGesture(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when(direction){
                    ItemTouchHelper.LEFT -> {
                        Log.i(TAG, "Left swipe")
                        //val position = viewHolder.adapterPosition
                        //productAdapter.deleteItem(position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        Log.i(TAG, "Right swipe")
                        //val position = viewHolder.adapterPosition
                        //productAdapter.editItem(position)
                    }
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeGesture)
        itemTouchHelper.attachToRecyclerView(binding.rvProductList)

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
        getProducts(category) { products ->
            if (products.isNotEmpty()) {
                binding.rvProductList.layoutManager = LinearLayoutManager(this)

                if(category == "myProducts") {
                    myProductAdapter = MyProductAdapter( products)
                    binding.rvProductList.adapter = myProductAdapter
                    myProductAdapter.setOnItemClickListener(this)
                }else{
                    productAdapter = ProductAdapter(products)
                    binding.rvProductList.adapter = productAdapter
                    productAdapter.setOnItemClickListener(this)
                }
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
        Log.i(TAG, "Fetching products with category $category")
        if(category == "myProducts"){
           FirebaseDatabaseHelper
               .getDatabaseReference("products")
               .orderByChild("userId")
               .equalTo(userId)
               .addListenerForSingleValueEvent(object : ValueEventListener {
               override fun onDataChange(snapshot: DataSnapshot) {
                   val productList = mutableListOf<ProductData>()

                   for (productSnapshot in snapshot.children) {
                       val productId = productSnapshot.key
                       val productData = productSnapshot.getValue(ProductData::class.java)
                       Log.i(TAG, "ProductData: $productData")

                       productData?.let {
                           val completeProductData = it.copy(productId = productId)
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

        }else {
            val productsReference = databaseReference.child(category)
            productsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val productList = mutableListOf<ProductData>()

                    for (productSnapshot in snapshot.children) {
                        val productId = productSnapshot.key
                        val productData = productSnapshot.getValue(ProductData::class.java)

                        productData?.let {
                            val completeProductData =
                                it.copy(productId = productId, category = category)
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
    override fun onProductAddClick(product: ProductData) {
        showToast("Fill this form to add product to your cart")
        val intent = Intent(this@ProductListActivity, AddOrUpdateProductActivity::class.java)
        intent.putExtra("action", ActionType.ADDFROMCATEGORY)
        intent.putExtra("productId", product.productId)
        intent.putExtra("category", product.category)
        startActivity(intent)
    }
    override fun onProductDeleteClick(product: ProductData) {
        showToast("Product deleted from cart")
    }

    override fun onUpdateClick(product: ProductData) {
        val intent = Intent(this@ProductListActivity, AddOrUpdateProductActivity::class.java)
        intent.putExtra("action", ActionType.UPDATE)
        intent.putExtra("productId", product.productId)
        updateItemLauncher.launch(intent)

    }

    override fun onPurchaseClick(product: ProductData) {
        showToast("Product purchased")
    }

    override fun onDeleteClick(product: ProductData) {
        showToast("Product deleted")
    }

    private fun showToast(message: String) {
        Toast.makeText(this@ProductListActivity, message, Toast.LENGTH_SHORT).show()
    }
}