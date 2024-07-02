package np.com.dipeshsah.ismt.dashboard.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.activity.AddOrUpdateProductActivity
import np.com.dipeshsah.ismt.adapters.MyProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentMyItemsBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.SwipeGesture
import np.com.dipeshsah.ismt.utils.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.Manifest
import android.app.Activity.RESULT_OK
import np.com.dipeshsah.ismt.activity.MapActivity


class MyItemsFragment : Fragment(), MyProductAdapter.OnItemClickListener {
    private var TAG = "MyItemsFragment"
    private lateinit var userId: String
    private lateinit var binding: FragmentMyItemsBinding
    private lateinit var productAdapter: MyProductAdapter
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val SMS_PERMISSION_CODE = 1
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val updateItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val addSuccess = result.data?.getBooleanExtra("updateSuccess", false) ?: false
                if (addSuccess) {
                    showSnackbar("Product updated successfully!")
                }
            }
        }

    var mapResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            selectedLatitude = data?.getDoubleExtra("latitude", 0.0)
            selectedLongitude = data?.getDoubleExtra("longitude", 0.0)
            val productId = data?.getStringExtra("productId")

            showSnackbar("Latitude: $selectedLatitude, Longitude: $selectedLongitude , productId: $productId")
            // Save to database
            if (selectedLatitude != null && selectedLongitude != null) {
                //saveSelectedLocation(selectedLatitude!!, selectedLongitude!!)
                // Use the selected location (e.g., update the UI)
                productId?.let {
                    updatedProductLocation(it, selectedLatitude!!, selectedLongitude!!)
                }
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
        // Inflate the layout for this fragment
        binding = FragmentMyItemsBinding.inflate(layoutInflater, container, false);

        val sharedPreferences = context?.getSharedPreferences("app", Context.MODE_PRIVATE)
        userId = sharedPreferences?.getString("userId", null).toString()

        getProductList()

        val swipeGesture = object : SwipeGesture(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        Log.i(TAG, "Left swipe")
                        productAdapter.deleteItem(position)
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

        binding.rvProductList.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
        }

        return binding.root
    }

    private fun fetchMyItems(productList: List<ProductData>) {
        if (productList.isNotEmpty()) {
            Log.i(TAG, "Products found: ${productList.toString()}")
            binding.rvProductList.layoutManager = LinearLayoutManager(context)
            productAdapter = MyProductAdapter(productList)
            binding.rvProductList.adapter = productAdapter
            productAdapter.setOnItemClickListener(this)
            handleEmptyProducts(true)
        } else {

            handleEmptyProducts(false)
            Log.d(TAG, "No products found or an error occurred.")
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
                showSnackbar("Product location updated successfully!")
                getProductList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update product location: ${e.message}")
                showSnackbar("Something went wrong!")
            }
        }
    }
    private fun getProductList() {
        coroutineScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val productList = withContext(Dispatchers.IO) {
                    fetchProductListFromFirebase()
                }
                // reorder the product list based on markAsPurchased and has location data
               val sortedProductList = productList.sortedWith(compareBy<ProductData> { it.markAsPurchased }
                    .thenBy { it.storeLocationLat != 0.0 && it.storeLocationLng != 0.0 }
                    .thenBy { it.name })

                fetchMyItems(sortedProductList)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch product list: ${e.message}")
                fetchMyItems(emptyList())
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }

    }

    private suspend fun fetchProductListFromFirebase(): List<ProductData> {
        return suspendCoroutine { continuation ->
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

                            productData?.let {
                                val completeProductData = it.copy(productId = productId)
                                productList.add(completeProductData)
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

    private fun handleEmptyProducts(isVisible: Boolean) {
        if (isVisible) {
            binding.rvProductList.visibility = View.VISIBLE
            binding.clNoData.visibility = View.GONE
        } else {
            binding.rvProductList.visibility = View.GONE
            binding.clNoData.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(product: ProductData) {
        MaterialAlertDialogBuilder(requireContext())
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
                        showSnackbar("Product deleted successfully!")
                        getProductList()
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to delete product ${it.cause}")
                        showSnackbar("Something went wrong!")
                    })
                dialog.dismiss()
            }
            .show()
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
                showSnackbar("Something went wrong!")
                onFailure(e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showSendSmsDialog(product: ProductData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_sms, null)
        val phoneNumberEditText = dialogView.findViewById<EditText>(R.id.phoneNumberEditText)
        val sendMessageButton = dialogView.findViewById<Button>(R.id.sendMessageButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        sendMessageButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotEmpty()) {
                checkSmsPermissionAndSend(phoneNumber, product)
                dialog.dismiss()
            } else {
                showSnackbar("Please enter a phone number")
            }
        }

        dialog.show()
    }

    private fun checkSmsPermissionAndSend(phoneNumber: String, product: ProductData) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showSnackbar("Permsission not granted. Requesting permission...")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            sendSms(phoneNumber, product)
            showSnackbar("SMS permission granted")
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
            showSnackbar("SMS sent successfully")
        } catch (e: Exception) {
            showSnackbar("Failed to send SMS")
            e.printStackTrace()
        }
    }

    override fun onUpdateClick(product: ProductData) {
        val intent = Intent(context, AddOrUpdateProductActivity::class.java)
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
                getProductList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update product: ${e.message}")
                showSnackbar("Something went wrong!")
            }

        }
    }

    override fun onShareClick(product: ProductData) {
        showSendSmsDialog(product)
    }

    override fun onLocationClick(product: ProductData) {
        val intent = Intent(requireContext(), MapActivity::class.java)
        intent.putExtra("latitude", product.storeLocationLat ?: 0.0)
        intent.putExtra("longitude", product.storeLocationLng ?: 0.0)
        intent.putExtra("productId", product.productId)
        mapResultLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}