package com.example.newtacks.models

data class User(
    val uid: String = "",
    val role: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",

    // 🖼 PROFILE IMAGE (ALL USERS)
    val profileImage: String = "",

    // 🏢 COMPANY ONLY
    val companyName: String? = null,
    val hrName: String? = null,

    // 🔧 WORKER ONLY
    val serviceCategories: List<String>? = null,
    val serviceExperience: Int? = null,

    // ⭐ WORKER RATING SYSTEM
    val rating: Double = 0.0,
    val totalRatings: Int = 0
)