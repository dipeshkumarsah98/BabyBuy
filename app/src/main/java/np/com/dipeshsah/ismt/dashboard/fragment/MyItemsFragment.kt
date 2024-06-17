package np.com.dipeshsah.ismt.dashboard.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentMyItemsBinding
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper


class MyItemsFragment : Fragment() {
    private var TAG = "MyItemsFragment"
    private lateinit var userId: String
    private lateinit var binding: FragmentMyItemsBinding
    private lateinit var productAdapter: ProductAdapter

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

        return binding.root
    }

    private fun fetchMyItems(){
        binding.progressBar.visibility = View.VISIBLE
        getProductList { products ->
            if(products.isNotEmpty()){
                binding.rvProductList.layoutManager = LinearLayoutManager(context)
                productAdapter = ProductAdapter(products)
                binding.rvProductList.adapter = productAdapter
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
}