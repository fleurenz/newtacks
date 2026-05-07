package com.example.newtacks.authentication

import com.example.newtacks.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    fun register(
        email: String,
        password: String,
        role: String,
        name: String,
        phone: String,
        address: String,
        companyName: String?,
        hrName: String?,
        categories: List<String>?,
        experience: Int?,
        onResult: (Result<Unit>) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid ?: return@addOnSuccessListener

                // 🧠 BUILD ROLE-SPECIFIC USER OBJECT
                val user = User(
                    uid = uid,
                    role = role,
                    name = name,
                    email = email,
                    phone = phone,
                    address = address,

                    companyName = if (role == "COMPANY") companyName else null,
                    hrName = if (role == "COMPANY") hrName else null,

                    serviceCategories = if (role == "WORKER") categories else null,
                    serviceExperience = if (role == "WORKER") experience else null
                )

                // 💾 SAVE TO FIRESTORE (SINGLE COLLECTION)
                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
    fun login(
    email: String,
    password: String,
    onResult: (Result<String>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid

                if (uid != null) {
                    onResult(Result.success(uid))
                } else {
                    onResult(Result.failure(Exception("User ID is null")))
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
    fun getUserRole(
        uid: String,
        onResult: (Result<String>) -> Unit
    ) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val role = document.getString("role")

                if (role != null) {
                    onResult(Result.success(role))
                } else {
                    onResult(Result.failure(Exception("Role not found")))
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
}