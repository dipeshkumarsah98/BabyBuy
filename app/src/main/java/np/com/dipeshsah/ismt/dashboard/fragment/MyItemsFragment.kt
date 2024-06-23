package np.com.dipeshsah.ismt.dashboard.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.activity.AddOrUpdateProductActivity
import np.com.dipeshsah.ismt.adapters.MyProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentMyItemsBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.SwipeGesture


class MyItemsFragment : Fragment(), MyProductAdapter.OnItemClickListener {
    private var TAG = "MyItemsFragment"
    private lateinit var userId: String
    private lateinit var binding: FragmentMyItemsBinding
    private lateinit var productAdapter: MyProductAdapter

    private val updateItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val addSuccess = result.data?.getBooleanExtra("updateSuccess", false) ?: false
                if (addSuccess) {
                    showSnackbar("Product updated successfully!")
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

        userId.let {
            fetchMyItems()
        }

        val swipeGesture = object : SwipeGesture(requireContext()){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                when(direction){
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

    private fun fetchMyItems(){
        binding.progressBar.visibility = View.VISIBLE
        getProductList { products ->
            if(products.isNotEmpty()){
                binding.rvProductList.layoutManager = LinearLayoutManager(context)
                productAdapter = MyProductAdapter(products)
                binding.rvProductList.adapter = productAdapter
                productAdapter.setOnItemClickListener(this)
                handleEmptyProducts(true)
            }else{
                handleEmptyProducts(false)
                Log.d(TAG, "No products found or an error occurred.")
            }
        }
     }

    private fun getProductList(callback: (List<ProductData>) -> Unit){
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
                    fetchMyItems()
                },
                onFailure = {
                    Log.e(TAG, "Failed to delete product ${it.cause}")
                    showSnackbar("Something went wrong!")
                })
                dialog.dismiss()
            }
            .show()
    }
    private fun deleteProduct(productId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit){
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabaseHelper.getDatabaseReference("products")
            .child(productId)
            .removeValue()
            .addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener {
                onFailure(it)
            }
    }
    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
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
        FirebaseDatabaseHelper.getDatabaseReference("products/${product.productId}")
            .setValue(product)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    showSnackbar("Product updated successfully!")
                    fetchMyItems()
                } else {
                    showSnackbar("Something went wrong!")
                }
            }

    }
    override fun onShareClick(product: ProductData) {
        showSnackbar("Share button clicked for product: ${product.name}")
    }
    override fun onLocationClick(product: ProductData) {
        showSnackbar("Location button clicked for product: ${product.name}")
    }
}