package com.example.tifs.DataClass

data class User(
    val email: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val fullName: String = "",
    val profilePicture: String = "",
    val id: String = ""
)


