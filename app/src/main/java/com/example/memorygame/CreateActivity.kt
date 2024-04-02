package com.example.memorygame

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import memory.utils.BitmapScaler
import memory.utils.EXTRA_BOARD_SIZE
import memory.utils.EXTRA_GAME_NAME
import memory.utils.MusicService
import memory.utils.isPermissionGranted
import memory.utils.requestPermission
import models.BoardSize
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_MEDIA_IMAGES
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
        const val TAG = "CreateActivity"
    }

    //Create a binder to MusicService
    private var musicService: MusicService? = null
    private var isServiceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
            // Now you can call methods of MusicService using musicService reference
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            musicService = null
            isServiceBound = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }

    //Start the song
    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, MusicService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    // Layout components
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar
    private lateinit var boardSize: BoardSize
    private lateinit var adapter: ImagePickerAdapter
    private lateinit var pickImageLauncher : ActivityResultLauncher<Intent>

    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    private val accessPhotos = R.string.toast_access_photos
    private val choosePics = R.string.choose_pics
    private val uploadComplete = R.string.upload_complete
    private val errorMemoryGame = R.string.error_memory_game
    private val gameExists = R.string.game_exists
    private val nameTaken = R.string.name_taken
    private val failedUpload = R.string.failed_upload_image
    private val chooseAnother = R.string.choose_another
    private val failedCreation = R.string.failed_game_creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        //Prepare the launcher to pictures intent
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                }
            }

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)


        //Enable the home button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            Log.i(MainActivity.TAG, "VERSION API >= 33")
            boardSize = intent.getParcelableExtra(EXTRA_BOARD_SIZE, BoardSize::class.java) as BoardSize}
        else {
            Log.i(MainActivity.TAG, "VERSION API < 33")
            boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        }

        numImagesRequired = boardSize.getNumPairs()

        val choosePics = getString(choosePics)
        supportActionBar?.title = "$choosePics (0 / $numImagesRequired)"

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }

        //Set the maximum length of game name
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))

        //Game name listener
        etGameName.addTextChangedListener(object: TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}


            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        //Custom images adapter
        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClicked() {

                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION )) {
                    launchIntentForPhotos()
                } else {
                    Log.i(TAG, "Clicked0")
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })

        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    //Result of request permission to access the media pictures
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this, getString(accessPhotos), Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    //Get the result after the photos intent
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "Result Code $resultCode")
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        //Get the images selected by the user
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)

                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }

        adapter.notifyDataSetChanged()
        val choosePics = getString(choosePics)
        supportActionBar?.title= "$choosePics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()

    }

    //Check if user filled the game name to enable the Save Button
    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }

        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    //Intent to user choose the pictures from his library
    private fun launchIntentForPhotos() {
        Log.i(TAG, "here")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        val chooserIntent = Intent.createChooser(intent, choosePics.toString())
        pickImageLauncher.launch(chooserIntent)

    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        val gameExists = getString(gameExists)
        val chooseAnother = getString(chooseAnother)
        // Check that we're not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener {  document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder (this)
                    .setTitle(getString(nameTaken))
                    .setMessage("$gameExists $customGameName. $chooseAnother")
                    .setPositiveButton("OK", null)
                    .show()
                btnSave.isEnabled = true
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, getString(errorMemoryGame), exception)
            Toast.makeText(this, getString(errorMemoryGame), Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }

    }

    //Handle the image uploading to Firebase
    // Handle the image uploading to Firebase
    private fun handleImageUploading(gameName: String) {
        // Show progress bar when uploading starts
        pbUploading.visibility = View.VISIBLE
        // Flag to track if any error occurred during the upload process
        var didEncounterError = false
        // List to store uploaded image URLs
        val uploadedImageUrls = mutableListOf<String>()

        // Iterate through each chosen image URI
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            // Convert image URI to byte array
            val imageByteArray = getImageByteArray(photoUri)
            // Define file path for the image in Firebase storage
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            // Reference to the Firebase storage location for the image
            val photoReference = storage.reference.child(filePath)

            // Upload the image byte array to Firebase storage
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    // Get the download URL of the uploaded image
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    // Check if the download URL retrieval task was successful
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
                        // Display a toast indicating failed upload
                        Toast.makeText(this, getString(failedUpload), Toast.LENGTH_SHORT).show()
                        // Set error flag to true
                        didEncounterError = true
                        // Exit the task completion listener
                        return@addOnCompleteListener
                    }
                    // Check if any error occurred during the upload process
                    if (didEncounterError) {
                        // Hide progress bar when encountering an error
                        pbUploading.visibility = View.GONE
                        // Exit the task completion listener
                        return@addOnCompleteListener
                    }

                    // Retrieve the download URL of the uploaded image
                    val downloadUrl = downloadUrlTask.result.toString()
                    // Add the download URL to the list of uploaded image URLs
                    uploadedImageUrls.add(downloadUrl)
                    // Update progress bar based on the number of uploaded images
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    // Log the completion of uploading the current image
                    Log.i(TAG, "Finished uploading $photoUri, num uploaded ${uploadedImageUrls.size}")
                    // Check if all images have been uploaded
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        // Handle the situation when all images have been uploaded
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }
        }
    }


    //After all the images were uploaded, get their urls
    // Handle the completion of uploading all images and creation of the game
    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // Set Firebase document with the game name and corresponding image URLs
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                // Hide the progress bar after completing the game creation
                pbUploading.visibility = View.GONE
                // Check if the game creation task was successful
                if (!gameCreationTask.isSuccessful) {
                    // Log any exceptions that occurred during game creation
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    // Display a toast indicating failed game creation
                    Toast.makeText(this, getString(failedCreation), Toast.LENGTH_SHORT).show()
                    // Exit the task completion listener
                    return@addOnCompleteListener
                }
                // Log the successful creation of the game
                Log.i(TAG, "Successfully create game $gameName")
                // Display a dialog indicating successful game creation
                val uploadComplete = getString(uploadComplete)
                AlertDialog.Builder(this)
                    .setTitle("$uploadComplete $gameName")
                    .setPositiveButton("OK") { _, _ ->
                        // Create a result intent containing the game name
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        // Set the result as OK and pass the result intent
                        setResult(Activity.RESULT_OK, resultData)
                        // Finish the activity
                        finish()
                    }.show()
            }
    }


    //Convert the image to Bitmap before upload it
    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${originalBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()

    }
}