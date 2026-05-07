package com.example.newtacks.models

data class Review(

    val reviewId: String = "",

    val jobId: String = "",

    val clientId: String = "",
    val clientName: String = "",

    val workerId: String = "",

    val rating: Float = 0f,
    val comment: String = "",

    val createdAt: Long = System.currentTimeMillis()
)