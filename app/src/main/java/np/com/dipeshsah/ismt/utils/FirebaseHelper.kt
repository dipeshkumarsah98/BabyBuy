package np.com.dipeshsah.ismt.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {
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
