package np.com.dipeshsah.ismt.activity

import AppConstants
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.adapters.MyProductAdapter
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.ActivityProductListBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.dto.ProductListType
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.CategoryProductSwipeGesture
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.SwipeGesture
import np.com.dipeshsah.ismt.utils.await

class ProductListActivity : AppCompatActivity(), ProductAdapter.OnItemClickListener,
    MyProductAdapter.OnItemClickListener {
    private var TAG = "ProductList"
    private lateinit var binding: ActivityProductListBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var myProductAdapter: MyProductAdapter
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String
    private  var categoryDetail: ProductListType? = null
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val mapPermissionCode = 1

    private val updateItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val addSuccess = result.data?.getBooleanExtra("addSuccess", false) ?: false
                val updateSuccess = result.data?.getBooleanExtra("updateSuccess", false) ?: false
                if (updateSuccess) {
                    showToast("Product updated successfully!")
                    fetchProducts(categoryDetail!!.category)
                } else if (addSuccess) {
                    showToast("Product added successfully!")
                }
            }
        }
    var mapResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
           val selectedLatitude = data?.getDoubleExtra("latitude", 0.0)
            val selectedLongitude = data?.getDoubleExtra("longitude", 0.0)
            val productId = data?.getStringExtra("productId")

            // Save to database
            if (selectedLatitude != null && selectedLongitude != null) {
                //saveSelectedLocation(selectedLatitude!!, selectedLongitude!!)
                // Use the selected location (e.g., update the UI)
                productId?.let {
                    updatedProductLocation(it, selectedLatitude, selectedLongitude)
                }
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


        val sharedPreferences =
            this@ProductListActivity.getSharedPreferences("app", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", null).toString()

        // getting the props from home fragment
        if (intent.hasExtra(AppConstants.CATEGORY)) {
            categoryDetail = intent.getParcelableExtra(AppConstants.CATEGORY)
        }

        if (categoryDetail != null) {
            binding.tvTitle.text = categoryDetail!!.pageName
        } else {
            Log.w(TAG, "No product data found")
            handleEmptyProducts(false);
        }

        // fetching products with particular category
        categoryDetail?.let {
            fetchProducts(categoryDetail!!.category)
        }

        if(categoryDetail?.category == "myProducts"){
            val swipeGesture = object : SwipeGesture(this) {
                @SuppressLint("SuspiciousIndentation")
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                        when (direction) {
                            ItemTouchHelper.LEFT -> {
                                Log.i(TAG, "Left swipe")
                                myProductAdapter.deleteItem(position)
                                binding.rvProductList.adapter?.notifyItemChanged(position)
                            }

                            ItemTouchHelper.RIGHT -> {
                                Log.i(TAG, "Right swipe")
                                myProductAdapter.editItem(position)
                                binding.rvProductList.adapter?.notifyItemChanged(position)
                            }
                            else -> {
                                Log.i(TAG, "Invalid swipe")
                            }
                        }
                }
            }
            val itemTouchHelper = ItemTouchHelper(swipeGesture)
            itemTouchHelper.attachToRecyclerView(binding.rvProductList)
        }else{
           val swipeGesture = object : CategoryProductSwipeGesture(this) {
               override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                   val position = viewHolder.bindingAdapterPosition
                   when (direction) {
                       ItemTouchHelper.LEFT -> {
                           Log.i(TAG, "Left swipe")
                           productAdapter.addItem(position)
                           binding.rvProductList.adapter?.notifyItemChanged(position)
                       }

                       ItemTouchHelper.RIGHT -> {
                           Log.i(TAG, "Right swipe")
                           productAdapter.editItem(position)
                           binding.rvProductList.adapter?.notifyItemChanged(position)
                       }
                       else -> {
                           Log.i(TAG, "Invalid swipe")
                       }
                   }
               }

           }
            val itemTouchHelper = ItemTouchHelper(swipeGesture)
            itemTouchHelper.attachToRecyclerView(binding.rvProductList)
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
        getProducts(category) { products ->
            if (products.isNotEmpty()) {
                binding.rvProductList.layoutManager = LinearLayoutManager(this)

                if (category == "myProducts") {
                    myProductAdapter = MyProductAdapter(products)
                    binding.rvProductList.adapter = myProductAdapter
                    myProductAdapter.setOnItemClickListener(this)
                } else {
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
        if (category == "myProducts") {
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

        } else {
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
    private fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        binding.progressBar.visibility = View.VISIBLE
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("products")
                        .child(productId)
                        .removeValue()
                        .await()
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete product: ${e.message}")
                showToast("Something went wrong!")
                onFailure(e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    private fun updatedProductLocation(productId: String, latitude: Double, longitude: Double) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("products/$productId")
                        .child("storeLocationLat")
                        .setValue(latitude)
                        .await()

                    FirebaseDatabaseHelper.getDatabaseReference("products/$productId")
                        .child("storeLocationLng")
                        .setValue(longitude)
                        .await()
                }
                showToast("Product location updated successfully!")
                fetchProducts(categoryDetail!!.category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update product location: ${e.message}")
                showToast("Something went wrong!")
            }
        }
    }
    private fun handleEmptyProducts(isVisible: Boolean) {
        if (isVisible) {
            binding.rvProductList.visibility = View.VISIBLE
            binding.clNoData.visibility = View.GONE
        } else {
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
        product.markAsPurchased = !product.markAsPurchased
        Log.i(TAG, "Purchase button clicked for product: ${product.name}")
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("products/${product.productId}")
                        .setValue(product)
                        .await()
                }
                fetchProducts(categoryDetail!!.category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update product: ${e.message}")
                showToast("Something went wrong!")
            }

        }
    }
    private fun showSendSmsDialog(product: ProductData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_sms, null)
        val phoneNumberEditText = dialogView.findViewById<EditText>(R.id.phoneNumberEditText)
        val sendMessageButton = dialogView.findViewById<Button>(R.id.sendMessageButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        sendMessageButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotEmpty()) {
                checkSmsPermissionAndSend(phoneNumber, product)
                dialog.dismiss()
            } else {
                showToast("Please enter a phone number")
            }
        }

        dialog.show()
    }

    private fun checkSmsPermissionAndSend(phoneNumber: String, product: ProductData) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Permsission not granted. Requesting permission...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                mapPermissionCode
            )
        } else {
            sendSms(phoneNumber, product)
            showToast("SMS permission granted")
        }
    }

    private fun sendSms(phoneNumber: String, product: ProductData) {
        val productDetails =
            "Product: ${product.name}\n" +
                    "Price: ${product.price}" +
                    "\nQuantity: ${product.quantity}" +
                    "\nlongitude: ${product.storeLocationLng}" +
                    "\nlatitude: ${product.storeLocationLat} "

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, productDetails, null, null)
            showToast("SMS sent successfully")
        } catch (e: Exception) {
            showToast("Failed to send SMS")
            e.printStackTrace()
        }
    }

    override fun onShareClick(product: ProductData) {
        showSendSmsDialog(product)
    }

    override fun onLocationClick(product: ProductData) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("latitude", product.storeLocationLat)
        intent.putExtra("longitude", product.storeLocationLng)
        intent.putExtra("productId", product.productId)
        mapResultLauncher.launch(intent)
    }

    override fun onDeleteClick(product: ProductData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.decline)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                deleteProduct(productId = product.productId!!,
                    onSuccess = {
                        showToast("Product deleted successfully!")
                        fetchProducts(categoryDetail!!.category)
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to delete product ${it.cause}")
                        showToast("Something went wrong!")
                    })
                dialog.dismiss()
            }
            .show()
    }

    private fun showToast(message: String) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}