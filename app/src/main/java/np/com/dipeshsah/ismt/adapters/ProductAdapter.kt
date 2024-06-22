package np.com.dipeshsah.ismt.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import np.com.dipeshsah.ismt.R
import np.com.dipeshsah.ismt.models.ProductData
import np.com.dipeshsah.ismt.viewHolders.ProductViewHolder

class ProductAdapter(private val productList: List<ProductData>) : RecyclerView.Adapter<ProductViewHolder>() {
    interface OnItemClickListener {
        fun onProductDeleteClick(product: ProductData)
        fun onProductAddClick(product: ProductData)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: ProductAdapter.OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.horizontal_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productCategory.text = product.category
        holder.productPrice.text = "${product.price.toString()}"
        Glide.with(holder.itemView.context)
            .load(product.image)
            .placeholder(R.drawable.no_image_blue)
            .error(R.drawable.no_image_blue)
            .into(holder.productImage)

       holder.addButton.setOnClickListener {
            listener?.onProductAddClick(product)
       }

        holder.deleteButton.setOnClickListener {
            listener?.onProductDeleteClick(product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }
}
