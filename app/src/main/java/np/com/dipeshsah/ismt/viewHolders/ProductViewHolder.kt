package np.com.dipeshsah.ismt.viewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import np.com.dipeshsah.ismt.R

class ProductViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val productName: TextView = itemView.findViewById(R.id.tv_productName)
    val productCategory: TextView = itemView.findViewById(R.id.chip_category)
    val productPrice: TextView = itemView.findViewById(R.id.tv_productPrice)
    val productImage: ImageView = itemView.findViewById(R.id.iv_productImage)
}