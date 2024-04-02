package models

import com.google.firebase.firestore.PropertyName

// Data class representing a list of user images
data class UserImageList (
   @PropertyName("images") val images: List<String>? = null  // List of image URLs, annotated with PropertyName for Firebase Firestore
)
