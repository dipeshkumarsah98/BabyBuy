package np.com.dipeshsah.ismt.utils

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import np.com.dipeshsah.ismt.models.ProductData
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseDatabaseHelper {
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    fun initialize() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference
    }

    fun getDatabaseReference(path: String): DatabaseReference {
        if (!this::databaseReference.isInitialized) {
            initialize()
        }
        return databaseReference.child(path)
    }

    suspend fun createProduct(productId: String, productData: ProductData){
        return suspendCoroutine { continuation ->
            getDatabaseReference("products").child(productId).setValue(productData)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }.addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }
}
suspend fun <T> Task<T>.await(): T {
    return suspendCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}

object FirebaseStorageHelper {

    private val storageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private val storageReference: StorageReference by lazy {
        storageInstance.reference
    }

    suspend fun uploadImage(uri: Uri, fileName: String): Uri{
        return suspendCoroutine { continuation ->
            val imageReference = storageReference.child(fileName)
            val uploadTask = imageReference.putFile(uri)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageReference.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    downloadUri?.let { continuation.resume(it) }
                } else {
                    task.exception?.let { continuation.resumeWithException(it) }
                }
            }
        }
    }

    fun deleteImage(fileName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val imageReference = storageReference.child(fileName)
        imageReference.delete().addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onFailure(it)
        }
    }

   fun downloadImage(fileName: String, onSuccess: (Uri) -> Unit, onFailure: (Exception) -> Unit) {
        val imageReference = storageReference.child(fileName)
        imageReference.downloadUrl.addOnSuccessListener {
            onSuccess(it)
        }.addOnFailureListener {
            onFailure(it)
        }
    }
}

