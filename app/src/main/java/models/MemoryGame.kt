package models

import memory.utils.DEFAULT_ICONS

// Class representing the Memory Game
class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {

    // Number of pairs of cards found
    var numPairsFound = 0

    // List of MemoryCard objects representing the game cards
    val cards: List<MemoryCard>

    // Number of card flips
    private var numCardFlips = 0

    // Index of the single selected card, null if no card is selected
    private var indexOfSingleSelectedCard: Int? = null

    // Initialization block
    init {
        // If customImages is null, use default icons
        if (customImages == null) {
            // Shuffle and take a subset of icons based on board size
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            // Double the chosen images and shuffle again
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            // Create MemoryCard objects from the images
            cards = randomizedImages.map { MemoryCard(it) }
        } else {
            // Double the custom images and shuffle
            val randomizedImages = (customImages + customImages).shuffled()
            // Create MemoryCard objects from the images with their hash code as identifier
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it) }
        }
    }

    // Method to flip a card at the specified position
    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        var foundMatch = false

        // If no card is currently selected
        if (indexOfSingleSelectedCard == null) {
            // Flip the card and mark it as the single selected card
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            // Check for a match between the previously selected card and the current one
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        // Flip the card and return whether a match was found
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    // Method to check for a match between two cards at the specified positions
    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        // If the identifiers of the two cards don't match, return false
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        // Mark both cards as matched and increment the number of pairs found
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    // Method to restore all non-matched cards to their face-down state
    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    // Method to check if the player has won the game
    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    // Method to check if the card at the specified position is face-up
    fun isCardFacedUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    // Method to get the number of moves (card flips) made in the game
    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}
