package np.com.dipeshsah.ismt.models

data class ProductData(
    val product_id: String? = null,
    val productName: String? = null,
    val category: String? = null,
    val price: Int? = null,
    val images: Array<String>? = null
)
