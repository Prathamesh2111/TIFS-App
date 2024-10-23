package com.example.tifs.DataClass

data class Comment(
        var id: String = "",
        val commentText: String = "",
        val userId: String = "",
        val userName: String = "",
        val userProfilePicture: String = "",
        val timestamp: com.google.firebase.Timestamp? = null,
)
