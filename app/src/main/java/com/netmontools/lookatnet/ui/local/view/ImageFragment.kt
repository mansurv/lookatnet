package com.netmontools.lookatnet.ui.local.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.netmontools.lookatnet.App
import com.netmontools.lookatnet.R
import com.netmontools.lookatnet.ui.local.model.Folder
import com.netmontools.lookatnet.ui.local.viewmodel.LocalViewModel
import com.netmontools.lookatnet.utils.getScaledBitmap
import java.io.File
import java.util.*

private const val TAG = "ImageFragment"
private const val ARG_FOLDER_ID = "folder_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

class ImageFragment : Fragment() {

    private lateinit var folder: Folder
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var photoView: ImageView

    private val localViewModel: LocalViewModel by lazy {
        ViewModelProviders.of(this).get(LocalViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        folder = Folder()
        //val path: String = folder.getFolder(folderId)!!.path.toString()
        //photoFile = File(path)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        photoView = view.findViewById(R.id.image_photo) as ImageView

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val folderId = arguments?.getSerializable(ARG_FOLDER_ID) as UUID
        val path: String = folder.getFolder(folderId)!!.path.toString()
        updateUI()

    }


    override fun onStop() {
        super.onStop()
        //crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        // Revoke photo permissions if the user leaves without taking a photo
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }


    private fun updateUI() {
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Specify which fields you want your query to return values for.
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Perform your query - the contactUri is like a "where" clause here
                val cursor = requireActivity().contentResolver
                    .query(contactUri!!, queryFields, null, null, null)
                cursor?.use {
                    // Double-check that you actually got results
                    if (it.count == 0) {
                        return
                    }

                    // Pull out the first column of the first row of data -
                    // that is your suspect's name.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    //crime.suspect = suspect
                    //crimeDetailViewModel.saveCrime(crime)
                    //suspectButton.text = suspect
                }
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                updatePhotoView()
            }
        }
    }


    companion object {

        fun newInstance(folderId: UUID): ImageFragment {
            val args = Bundle().apply {
                putSerializable(ARG_FOLDER_ID, folderId)
            }
            return ImageFragment().apply {
                arguments = args
            }
        }
    }
}