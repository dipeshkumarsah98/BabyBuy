package np.com.dipeshsah.ismt.models

data class ProductData(
    var productId: String? = null,
    var name: String? = null,
    var description: String? = null,
    var category: String? = null,
    var price: Int? = null,
    var image: String? = null,
    var storeLocationLat: String? = null,
    var storeLocationLng: String? = null,
)
