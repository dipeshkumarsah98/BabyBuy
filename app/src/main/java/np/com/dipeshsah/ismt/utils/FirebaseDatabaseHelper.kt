package np.com.dipeshsah.ismt.utils

import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

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
}

object FirebaseStorageHelper {

    private val storageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private val storageReference: StorageReference by lazy {
        storageInstance.reference
    }

    fun uploadImage(uri: Uri, fileName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
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
                downloadUri?.let { onSuccess(it.toString()) }
            } else {
                task.exception?.let { onFailure(it) }
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

