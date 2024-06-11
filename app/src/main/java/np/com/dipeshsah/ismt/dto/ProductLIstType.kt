package np.com.dipeshsah.ismt.dto

import android.os.Parcel
import android.os.Parcelable

data class ProductListType(
    val pageName: String,
    val category: String
): Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString().toString(),
        parcel.readString().toString(),
    ){
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pageName)
        parcel.writeString(category)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProductListType> {
        override fun createFromParcel(parcel: Parcel): ProductListType {
            return ProductListType(parcel)
        }

        override fun newArray(size: Int): Array<ProductListType?> {
            return arrayOfNulls(size)
        }
    }
}