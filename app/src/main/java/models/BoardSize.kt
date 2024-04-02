package models

// Enumeration representing different board sizes for the Memory Game
enum class BoardSize(val numCards: Int) {
    EASY (8),       // Easy board size with 8 cards
    MEDIUM(18),     // Medium board size with 18 cards
    HARD(24);       // Hard board size with 24 cards

    // Companion object containing utility methods for the enum
    companion object {
        // Method to get a BoardSize enum by its numerical value
        fun getByValue (value: Int) = values().first { it.numCards == value }
    }

    // Method to get the width of the board based on its size
    fun getWidth(): Int {
        return when (this) {
            EASY -> 2       // Easy board has 2 columns
            MEDIUM -> 3     // Medium board has 3 columns
            HARD -> 4       // Hard board has 4 columns
        }
    }

    // Method to get the height of the board based on its size
    fun getHeight(): Int {
        return numCards / getWidth()   // Height is determined by number of cards divided by width
    }

    // Method to get the number of pairs of cards in the board
    fun getNumPairs(): Int {
        return numCards / 2    // Number of pairs is half of the total number of cards
    }
}
