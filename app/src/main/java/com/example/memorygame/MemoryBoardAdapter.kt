package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import models.BoardSize
import models.MemoryCard
import kotlin.math.min

/**
 * Adapter for populating the RecyclerView with MemoryCard items.
 *
 * @property context The context of the application or activity.
 * @property boardSize The size of the game board.
 * @property cards The list of MemoryCard objects representing the game cards.
 * @property drawableResource The resource ID of the drawable used for card backs.
 * @property cardClickListener Listener for handling card clicks.
 */
class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    drawableResource: Int?,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object {
        // Margin size for each card
        private const val MARGIN_SIZE = 20
        private const val TAG = "MemoryBoardAdapter"
    }

    /**
     * Interface for handling card clicks.
     */
    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    // Use the provided drawableResource if not null, otherwise use the defaultDrawableResource
    private val resolvedDrawableResource: Int = drawableResource ?: R.drawable.card_layout_1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Calculate the dimensions for each card based on the size of the game board
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)

        // Inflate the layout for a single card view
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    /**
     * ViewHolder class for caching view references.
     *
     * @param itemView The view for a single card item.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        /**
         * Binds the data to the views within a ViewHolder.
         *
         * @param position The position of the card in the RecyclerView.
         */
        fun bind(position: Int) {
            val memoryCard = cards[position]
            if (memoryCard.isFaceUp) {
                // If the card is face up, load the image if available, otherwise use the identifier
                if (memoryCard.imageUrl != null) {
                    Picasso.get().load(memoryCard.imageUrl).placeholder(R.drawable.ic_image).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                // If the card is face down, display the card back drawable
                Log.i(TAG, "DrawableResource $resolvedDrawableResource")
                imageButton.setImageResource(resolvedDrawableResource)
            }

            // Adjust alpha value based on whether the card is matched
            imageButton.alpha = if (memoryCard.isMatched) .4f else 1.0f

            // Set background tint color for matched cards
            val colorStateList = if (memoryCard.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            // Set click listener for card clicks
            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
