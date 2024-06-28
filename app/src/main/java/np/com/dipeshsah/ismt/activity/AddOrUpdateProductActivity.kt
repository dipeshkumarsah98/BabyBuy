package np.com.dipeshsah.ismt.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.databinding.ActivityAddOrUpdateProductBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.dto.FormKey
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.FirebaseStorageHelper
import np.com.dipeshsah.ismt.utils.await
import np.com.dipeshsah.ismt.utils.createImageFile
import np.com.dipeshsah.ismt.utils.getUriForFile
import kotlin.coroutines.suspendCoroutine

class AddOrUpdateProductActivity : AppCompatActivity() {
    private var TAG = "AddOrUpdateProduct"
    private lateinit var binding: ActivityAddOrUpdateProductBinding
    private var currentAction = ActionType.ADD
    private lateinit var userId: String
    private lateinit var imageView: ImageView
    private lateinit var captureImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var updateProductId: String
    private var currentImageUri: Uri? = null
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private var selectedLongitute: Double = 0.0
    private var selectedLatitude: Double = 0.0

    var mapResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)
            // Save to database
            if (latitude != null && longitude != null) {
                   selectedLongitute = longitude
                    selectedLatitude = latitude
                    binding.tvLocationAddress.text = "long: $longitude \nlati: $latitude"
                    binding.tbAddLocation.setText("Update Location")
                    binding.tbAddLocation.setBackgroundColor(getColor(R.color.accent2))
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOrUpdateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActivity()
    }

    private fun fetchCategories() {
        coroutineScope.launch {
            showProgressBar(true)
            try {
                val categories = withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("categories")
                        .get()
                        .await()
                        .children
                        .mapNotNull { it.key }
                }
                Log.i(TAG, "Categories: $categories")
                val arrayAdapter = ArrayAdapter(this@AddOrUpdateProductActivity, R.layout.list_item, categories)
                binding.actvProductCategory.setAdapter(arrayAdapter)
                showProgressBar(false)
            }catch (exception: Exception) {
                Log.e(TAG, "Error fetching data", exception)
                showProgressBar(false)
            }
        }
    }

    private fun fetchParticularProduct(productId: String) {
        coroutineScope.launch {
            showProgressBar(true)
            try {
                val productData = withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("products/$productId")
                        .get()
                        .await()
                        .getValue(ProductData::class.java)
                }
                productData?.let {
                    Log.i(TAG, "Update Product data: $it")
                    binding.tietProductName.setText(it.name)
                    binding.tietProductDescription.setText(it.description)
                    binding.tietProductPrice.setText(it.price.toString())
                    binding.actvProductCategory.setText(it.category)
                    binding.tietProductQuantity.setText(it.quantity.toString())
                    binding.cbPhurchased.isChecked = it.markAsPurchased
                    productData.image.let {
                        currentImageUri = Uri.parse(it)
                        Glide.with(this@AddOrUpdateProductActivity)
                            .load(it)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.ic_baseline_24)
                            .into(binding.productImage)
                    }
                    binding.tbAddLocation.text = "Update Location"
                    binding.tvLocationAddress.text = "long: ${it.storeLocationLng} \nlati: ${it.storeLocationLat}"
                    binding.tbAddLocation.setBackgroundColor(getColor(R.color.accent2))
                    selectedLongitute = it.storeLocationLng!!
                    selectedLatitude = it.storeLocationLat!!
                }
                showProgressBar(false)
            } catch (exception: Exception) {
                Log.e(TAG, "Error fetching data", exception)
                showProgressBar(false)
            }
        }
    }

    // This method should be called when user add product from category page
    private fun fetchProductData(productId: String, category: String) {
        coroutineScope.launch {
            showProgressBar(true)
            try {
                val productData = withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.getDatabaseReference("categories/$category/$productId")
                        .get()
                        .await()
                        .getValue(ProductData::class.java)
                }
                productData?.let {
                    binding.tietProductName.setText(it.name)
                    binding.tietProductDescription.setText(it.description)
                    binding.tietProductPrice.setText(it.price.toString())
                    binding.actvProductCategory.setText(it.category)

                    // select particular category from dropdown
                    if (it.quantity == null) {
                        binding.tietProductQuantity.setText("0")
                    }
                    binding.cbPhurchased.isChecked = it.markAsPurchased
                    productData.image.let {
                        currentImageUri = Uri.parse(it)
                        Glide.with(this@AddOrUpdateProductActivity)
                            .load(it)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.ic_baseline_24)
                            .into(binding.productImage)
                    }
                    showProgressBar(false)
                }
            }catch (exception: Exception) {
                Log.e(TAG, "Error fetching data", exception)
                showProgressBar(false)
            }

        }
    }

    private fun handleNewProduct() {
        val name = binding.tietProductName.text.toString()
        val description = binding.tietProductDescription.text.toString()
        val price = binding.tietProductPrice.text.toString()
        val category = binding.actvProductCategory.text.toString()
        val quality = binding.tietProductQuantity.text.toString()

        val isValid = validateForm(
            name,
            description,
            price,
            category,
            quality
        )

        if (!isValid) {
            return
        }

        val productReference = FirebaseDatabaseHelper.getDatabaseReference("products");
        val id = productReference.push().key
        id?.let {
            showProgressBar(true)
            when (currentAction) {
                ActionType.UPDATE -> {
                    updateProduct(updateProductId)
                }

                ActionType.ADDFROMCATEGORY -> {
                    createProductFromCategory(id)
                }

                else -> {
                    createNewProduct(id)
                }
            }

        }
    }

    private fun updateProduct(productId: String) {
        coroutineScope.launch {
            try {
                showProgressBar(true)
                val productData = ProductData(
                    productId = productId,
                    userId = userId,
                    name = binding.tietProductName.text.toString(),
                    category = binding.actvProductCategory.text.toString(),
                    price = binding.tietProductPrice.text.toString().toInt(),
                    image = currentImageUri.toString(),
                    description = binding.tietProductDescription.text.toString(),
                    quantity = binding.tietProductQuantity.text.toString().toInt(),
                    markAsPurchased = binding.cbPhurchased.isActivated,
                    storeLocationLat = selectedLatitude,
                    storeLocationLng = selectedLongitute,
                )
                withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.createProduct(productId, productData)
                }
                Log.i(TAG, "Item updated successfully!")
                val resultIntent = Intent()
                resultIntent.putExtra("updateSuccess", true)
                setResult(Activity.RESULT_OK, resultIntent)
                showProgressBar(false)
                finish()
            } catch (exception: Exception) {
                Log.e(TAG, "Error updating product", exception)
                showProgressBar(false)
            }
        }
    }

    private fun createProductFromCategory(productId: String) {
        // TODO missing logic to update quantity
        // since we are adding product from category page,
        // there might be case when user had already that select
        // product in his own item, so we have to check for and increase quantity
        createProduct(productId, currentImageUri.toString())
    }

    private fun createNewProduct(productId: String) {
        uploadProfileImage(productId)
    }

    private fun createProduct(productId: String, image: String?) {
        coroutineScope.launch {
            showProgressBar(true);
            try {
                val productData = ProductData(
                    productId = productId,
                    userId = userId,
                    name = binding.tietProductName.text.toString(),
                    category = binding.actvProductCategory.text.toString(),
                    price = binding.tietProductPrice.text.toString().toInt(),
                    image = image,
                    description = binding.tietProductDescription.text.toString(),
                    quantity = binding.tietProductQuantity.text.toString().toInt(),
                    markAsPurchased = binding.cbPhurchased.isActivated,
                    storeLocationLat = selectedLatitude,
                    storeLocationLng = selectedLongitute
                )
                withContext(Dispatchers.IO) {
                    FirebaseDatabaseHelper.createProduct(productId, productData)
                }
                Log.i(TAG, "Item added successfully")
                val resultIntent = Intent()
                resultIntent.putExtra("addSuccess", true)
                setResult(Activity.RESULT_OK, resultIntent)
                showProgressBar(false)
                finish()
            } catch (exception: Exception) {
                Log.e(TAG, "Error creating product", exception)
                showProgressBar(false)
            }
        }
    }

    private fun uploadProfileImage(productId: String) {
        currentImageUri?.let {
            coroutineScope.launch {
                val url = withContext(Dispatchers.IO) {
                    FirebaseStorageHelper.uploadImage(it, "product/${userId}-${productId}.jpg")
                }
                Log.i(TAG, "The image is uploaded successfully: $url")

                createProduct(productId, url.toString())
            }
        } ?: run {
            // if no image is selected
            createProduct(productId, null);
        }
    }

    private fun captureImage() {
        val imageFile = createImageFile(this)
        currentImageUri = getUriForFile(this, imageFile)
        captureImageLauncher.launch(currentImageUri)
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun requestCameraPermission() {
        if (hasCameraPermission()) {
            captureImage()
        } else {
            ActivityCompat.requestPermissions(
                this, AddOrUpdateProductActivity.CAMERA_PERMISSION, 0
            )
            requestPermissions(AddOrUpdateProductActivity.CAMERA_PERMISSION, 0)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return AddOrUpdateProductActivity.CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED

        }
    }

    private fun validateForm(
        email: String,
        desc: String,
        price: String,
        category: String,
        quality: String
    ): Boolean {

        if (email.isEmpty()) {
            validForm(FormKey.NAME, "Name is required")
        }
        if (desc.isEmpty()) {
            validForm(FormKey.DESCRIPTION, "Description is required")
        }
        if (price.isEmpty()) {
            validForm(FormKey.PRICE, "Price is required")
        } else if (price.toInt() < 1) {
            validForm(FormKey.PRICE, "Price should be greater than 0")
            Log.i(TAG, "Price is ${price.toInt()}")
        }
        if (category.isEmpty()) {
            validForm(FormKey.CATEGORY, "Category is required")
        }
        if (quality.isEmpty()) {
            validForm(FormKey.QUANTITY, "Quantity is required")
        } else if (quality.toInt() < 1) {
            validForm(FormKey.QUANTITY, "Quantity should be greater than 0")
            Log.i(TAG, "Quantity is ${quality.toInt()}")
        }

        return email.isNotEmpty() && desc.isNotEmpty() && price.isNotEmpty() && category.isNotEmpty() && quality.isNotEmpty() && quality.toInt() > 0 && price.toInt() > 0
    }

    private fun validForm(key: FormKey, value: String) {
        when (key) {
            FormKey.NAME -> binding.tilProductNameLayout.error = value
            FormKey.DESCRIPTION -> binding.tilProductDescriptionLayout.error = value
            FormKey.PRICE -> binding.tilProductPriceLayout.error = value
            FormKey.CATEGORY -> binding.tilProductCategoryLayout.error = value
            FormKey.QUANTITY -> binding.tilProductQuantityLayout.error = value
            else -> {}
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@AddOrUpdateProductActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun setUpActivity(): Unit {
        if (intent.hasExtra("action")) {
            currentAction = intent.getStringExtra("action")!!
            Log.i(TAG, "Current action: $currentAction")
        }

        // get userId from SharedPreferences
        val sharedPreferences =
            this@AddOrUpdateProductActivity.getSharedPreferences("app", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", null).toString()

        imageView = binding.productImage

        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    imageView.setImageURI(currentImageUri)
                    Log.i(TAG, "Image captured successfully using camera $currentImageUri")
                }
            }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentImageUri = it
                imageView.setImageURI(it)
            }
        }

        fetchCategories()

        when (currentAction) {
            ActionType.UPDATE -> {
                val productId = intent.getStringExtra("productId")
                binding.bSubmit.text = "Update"
                binding.bSubmit.icon = getDrawable(R.drawable.edit_icon)
                Log.i(TAG, "Update -> process -> Product id: $productId")
                if (productId != null) {
                    updateProductId = productId
                    fetchParticularProduct(productId)
                }
            }

            ActionType.ADDFROMCATEGORY -> {
                val productId = intent.getStringExtra("productId")
                val category = intent.getStringExtra("category")
                Log.i(
                    TAG,
                    "Add from category -> process -> Product id: $productId, Category: $category"
                )
                if (productId != null && category != null) {
                    fetchProductData(productId, category)
                }
            }
        }
        binding.tvLocationAddress.text = "long: $selectedLatitude \nlati: $selectedLongitute"

        binding.bSubmit.setOnClickListener {
            handleNewProduct()
        }

        binding.productImage.setOnClickListener {
            // select image from gallery
            pickImageFromGallery()
        }

        binding.editImageIcon.setOnClickListener {
            // capture image from camera
            //captureImage()
            requestCameraPermission()
        }

        binding.ibBack.setOnClickListener {
            finish()
        }
        binding.bCancel.setOnClickListener {
            finish()
        }

        binding.tbAddLocation.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latitude", selectedLatitude)
            intent.putExtra("longitude", selectedLongitute)
            mapResultLauncher.launch(intent)
        }
    }

    private fun showProgressBar(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.scrollViewWrapper.visibility = android.view.View.GONE
        } else {
            binding.progressBar.visibility = android.view.View.GONE
            binding.scrollViewWrapper.visibility = android.view.View.VISIBLE
        }
    }

    companion object {
        private val CAMERA_PERMISSION = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}