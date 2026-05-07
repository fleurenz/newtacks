package com.example.newtacks.models

data class User(
    val uid: String = "",
    val role: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",

    // 🏢 COMPANY ONLY
    val companyName: String? = null,
    val hrName: String? = null,

    // 🔧 WORKER ONLY
    val serviceCategories: List<String>? = null,
    val serviceExperience: Int? = null,
    val rating: Double = 0.0,
    val totalRatings: Int = 0
)