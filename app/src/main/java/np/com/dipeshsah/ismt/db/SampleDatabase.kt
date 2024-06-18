package np.com.dipeshsah.ismt.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 1)
abstract class SampleDatabase : RoomDatabase(){
    companion object {
        fun getInstance(applicationContext: Application): SampleDatabase {
            return Room.databaseBuilder(
                applicationContext,
                SampleDatabase::class.java,
                "babybuy_db"
            ).build()
        }
    }
    abstract fun getProductDao(): ProductDao
}