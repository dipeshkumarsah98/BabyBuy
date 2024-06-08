package np.com.dipeshsah.ismt.dashboard.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import np.com.dipeshsah.ismt.adapters.ProductAdapter
import np.com.dipeshsah.ismt.databinding.FragmentHomeBinding
import np.com.dipeshsah.ismt.models.ProductData

class HomeFragment : Fragment() {
    private  lateinit var binding:FragmentHomeBinding
    private  lateinit var productAdapter:ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false);
        // Inflate the layout for this fragment

        val images = arrayOf("https://www.homechoice.co.za/file/v1370537154084896159/general/Baby+%26+Kids+-+Tippy+Toes+walker.jpg")
        val products = listOf(
            ProductData("1", "ABcd", "Baby", 100, images),
            ProductData("2", "Xyz", "Baby", 100, images),
            ProductData("3", "fgh", "Baby", 100, images)
        )

        binding.rvSuggestion.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(products)
        binding.rvSuggestion.adapter = productAdapter

        return binding.root
    }

}