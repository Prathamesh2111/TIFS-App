package com.example.tifs.DataClass

data class Post (
    val id: String = "",
    val questionBody: String = "",
    val subject: String = "",
    val userName: String = "",
    val userId: String = "",
    val userProfilePicture: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    var likesCount: Int = 0,
    var likedBy: List<String> = listOf()  // List of user IDs who liked the post
)