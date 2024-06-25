package np.com.dipeshsah.ismt.adapters
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.viewHolders.MyProductViewHolder

class MyProductAdapter( private val productList: List<ProductData>) : RecyclerView.Adapter<MyProductViewHolder>() {
    interface OnItemClickListener {
        fun onDeleteClick(product: ProductData)
        fun onUpdateClick(product: ProductData)
        fun onPurchaseClick(product: ProductData)
        fun onShareClick(product: ProductData)
        fun onLocationClick(product: ProductData)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.my_product_list, parent, false)
        return MyProductViewHolder(view)
    }

    fun deleteItem(position: Int) {
        listener?.onDeleteClick(productList[position])
    }
    fun editItem(position: Int) {
        listener?.onUpdateClick(productList[position])
    }
    override fun onBindViewHolder(holder: MyProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productCategory.text = product.category
        product.quantity.let {
            holder.productQuantity.text = "Quantity: ${product.quantity.toString()}"
        }
        holder.productPrice.text = "${product.price.toString()}"
        Glide.with(holder.itemView.context)
            .load(product.image)
            .placeholder(R.drawable.no_image_blue)
            .error(R.drawable.no_image_blue)
            .into(holder.productImage)

        if(product.markAsPurchased){
            holder.cardView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.cardView.context, R.color.test))
        }

        if(product.storeLocationLat != 0.0 || product.storeLocationLng != 0.0) {
            holder.locationButton.setBackgroundColor(ContextCompat.getColor(holder.locationButton.context, R.color.accent2))
            holder.locationButton.iconTint = ContextCompat.getColorStateList(holder.locationButton.context, R.color.whitesh)
        }

        holder.purchaseButton.setOnClickListener {
            listener?.onPurchaseClick(product)
        }
        holder.locationButton.setOnClickListener {
            listener?.onLocationClick(product)
        }
        holder.shareButton.setOnClickListener {
            listener?.onShareClick(product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }
}
