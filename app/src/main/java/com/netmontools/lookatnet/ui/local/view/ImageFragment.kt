package com.netmontools.lookatnet.ui.local.view

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.netmontools.lookatnet.R
import com.netmontools.lookatnet.databinding.FragmentImageBinding

class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null


    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentImageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val imageView: SubsamplingScaleImageView =
            binding.root.findViewById(R.id.imageView)

        val path = requireArguments().getString("arg")

        binding.imageView.setImage(ImageSource.uri(path!!))

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            try {
//                val navController =
//                    findNavController(
//                        requireActivity(),
//                        R.id.nav_host_fragment
//                    )
//
//                //val bundle = Bundle()
//                //bundle.putString("arg", file.getPath())
//                navController.navigate(R.id.action_nav_image_to_nav_local)
//            } catch (npe: NullPointerException) {
//                npe.printStackTrace()
//            }
//            return true
//        }
//
//        return super.onKeyDown(keyCode, event)
//    }

}