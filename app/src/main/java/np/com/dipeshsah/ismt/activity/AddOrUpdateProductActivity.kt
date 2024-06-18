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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.databinding.ActivityAddOrUpdateProductBinding
import np.com.dipeshsah.ismt.dto.ActionType
import np.com.dipeshsah.ismt.dto.FormKey
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.utils.FirebaseDatabaseHelper
import np.com.dipeshsah.ismt.utils.FirebaseStorageHelper
import np.com.dipeshsah.ismt.utils.createImageFile
import np.com.dipeshsah.ismt.utils.getUriForFile

class AddOrUpdateProductActivity : AppCompatActivity() {
    private var TAG = "AddOrUpdateProduct"
    private lateinit var binding: ActivityAddOrUpdateProductBinding
    private var currentAction = ActionType.ADD
    private lateinit var userId: String

    private lateinit var imageView: ImageView
    private lateinit var captureImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOrUpdateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpActivity()
    }

    private fun fetchCategories() {
        FirebaseDatabaseHelper.getDatabaseReference("categories").get().addOnSuccessListener { snapshot ->
            val categories = mutableListOf<String>()
            for(categorySnapshot in snapshot.children){
                categorySnapshot.key?.let { categories.add(it) }
            }
            Log.i(TAG, "Categories: $categories")
            val arrayAdapter = ArrayAdapter(this, R.layout.list_item, categories)
            binding.actvProductCategory.setAdapter(arrayAdapter)
        }.addOnFailureListener {exception ->
            // Handle error
            Log.e(TAG, "Error fetching data", exception)
        }
    }
    private fun handleNewProduct() {
        val name = binding.tietProductName.text.toString()
        val description = binding.tietProductDescription.text.toString()
        val price = binding.tietProductPrice.text.toString()
        val category = binding.actvProductCategory.text.toString()

        val isValid = validateForm(
            name,
            description,
            price,
            category
        )
        if(!isValid){
            return
        }

        val productReference = FirebaseDatabaseHelper.getDatabaseReference("products");
        val id = productReference.push().key
        id?.let {
            // first upload image
            uploadProfileImage(it)
        }
    }
    private fun createProduct(productId: String, image: String?) {
        val productReference = FirebaseDatabaseHelper.getDatabaseReference("products");
        val productData = ProductData(
            productId = productId,
            userId = userId,
            name = binding.tietProductName.text.toString(),
            category = binding.actvProductCategory.text.toString(),
            price = binding.tietProductPrice.text.toString().toInt(),
            image = image,
            description = binding.tietProductDescription.text.toString()
        )
        productReference.child(productId).setValue(productData).addOnSuccessListener {

            Log.i(TAG, "New item created successfully ${it.toString()}")

            val resultIntent = Intent()
            resultIntent.putExtra("addSuccess", true)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error creating product", exception)
        }
    }
    private fun uploadProfileImage(productId: String) {
        currentImageUri?.let {
            FirebaseStorageHelper.uploadImage(it, "product/${userId}-${productId}.jpg",
                onSuccess = { imageUrl ->
                    Log.i(TAG, "Image uploaded successfully $it")
                    createProduct(productId, imageUrl)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error uploading image ${exception.message}")
                })
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

    private fun validateForm(email: String, desc: String, price: String, category: String): Boolean {
        if (email.isEmpty()) {
            validForm(FormKey.NAME, "Name is required")
        }
        if (desc.isEmpty()) {
            validForm(FormKey.DESCRIPTION, "Description is required")
        }
        if (price.isEmpty()) {
            validForm(FormKey.PRICE, "Price is required")
        }
        if (category.isEmpty()) {
            validForm(FormKey.CATEGORY, "Category is required")
        }
        if(price.toInt() <= 0){
            validForm(FormKey.PRICE, "Price should be greater than 0")
        }

        return email.isNotEmpty() && desc.isNotEmpty() && price.isNotEmpty() && category.isNotEmpty()
    }
    private fun validForm(key: FormKey, value: String){
        when (key){
            FormKey.NAME -> binding.tilProductNameLayout.error = value
            FormKey.DESCRIPTION -> binding.tilProductDescriptionLayout.error = value
            FormKey.PRICE -> binding.tilProductPriceLayout.error = value
            FormKey.CATEGORY -> binding.tilProductCategoryLayout.error = value
            else -> {}
        }
    }
    private fun setUpActivity(): Unit {
        if(intent.hasExtra("action")){
            currentAction = intent.getStringExtra("action") as ActionType
        }
        // get userId from SharedPreferences

        val sharedPreferences = this@AddOrUpdateProductActivity.getSharedPreferences("app", Context.MODE_PRIVATE)
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
    }

    companion object {
        private val CAMERA_PERMISSION = arrayOf(
            Manifest.permission.CAMERA
        )
    }
}