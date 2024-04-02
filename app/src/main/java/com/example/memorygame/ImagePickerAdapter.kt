package com.example.memorygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import models.BoardSize
import kotlin.math.min

/**
 * Adapter for populating the RecyclerView with ImagePicker items.
 *
 * @property context The context of the application or activity.
 * @property imageUris The list of URIs representing the selected images.
 * @property boardSize The size of the game board.
 * @property imageClickListener Listener for handling image clicks.
 */
class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceholderClicked()
    }

    /**
     * Interface for handling image clicks.
     */


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for a single image item
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        // Calculate the dimensions for each image item based on the size of the game board
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the data to the views within a ViewHolder
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    /**
     * ViewHolder class for caching view references.
     *
     * @param itemView The view for a single image item.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        /**
         * Binds the data to the views within a ViewHolder when an image URI is provided.
         *
         * @param uri The URI of the selected image.
         */
        fun bind(uri: Uri) {
            // Load the selected image into the ImageView
            ivCustomImage.setImageURI(uri)
            // Disable click listener for the image
            ivCustomImage.setOnClickListener(null)
        }

        /**
         * Binds the data to the views within a ViewHolder when no image URI is provided (placeholder).
         */
        fun bind() {
            // Set click listener for the placeholder image
            ivCustomImage.setOnClickListener {
                // Notify the listener when the placeholder image is clicked
                imageClickListener.onPlaceholderClicked()
                // Optionally, launch an intent for the user to select photos
            }
        }
    }
}
