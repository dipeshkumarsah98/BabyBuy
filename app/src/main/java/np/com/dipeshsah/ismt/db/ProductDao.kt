package np.com.dipeshsah.ismt.db
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProductDao {
    // It act as a service class for the ProductEntity
    @Insert
    fun insertProduct(productEntity: ProductEntity)

    @Insert
    fun  insertProducts(productEntities: List<ProductEntity>)

    @Delete
    fun deleteProduct(productEntity: ProductEntity)

    @Query("SELECT * FROM products")
    fun getAllProducts(): List<ProductEntity>
}