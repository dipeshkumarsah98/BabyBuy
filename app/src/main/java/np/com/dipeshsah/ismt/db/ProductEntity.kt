package np.com.dipeshsah.ismt.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val price: Int? = null,
    val quantity: Int? = null,
    val image: String? = null,
    val userId: String? = null,
    @ColumnInfo(name = "store_location_lat") val storeLocationLat: String? = null,
    @ColumnInfo(name = "store_location_lng") val storeLocationLng: String? = null,
    @ColumnInfo(name = "mark_as_purchased") val markAsPurchased: Boolean? = null,
)
