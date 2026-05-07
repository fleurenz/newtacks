package com.example.newtacks.client

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.newtacks.authentication.OnboardingActivity
import com.example.newtacks.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientAccountFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView

    private lateinit var tvTotalRequests: TextView
    private lateinit var tvCompletedRequests: TextView

    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_client_account,
            container,
            false
        )

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvAddress = view.findViewById(R.id.tvAddress)

        tvTotalRequests = view.findViewById(R.id.tvTotalRequests)
        tvCompletedRequests = view.findViewById(R.id.tvCompletedRequests)

        btnLogout = view.findViewById(R.id.btnLogout)

        loadProfile()
        loadStats()
        setupLogout()

        return view
    }

    // --------------------------------------------------
    // PROFILE
    // --------------------------------------------------

    private fun loadProfile() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                tvName.text = doc.getString("name") ?: "Client"
                tvEmail.text = doc.getString("email") ?: ""
                tvAddress.text = doc.getString("address") ?: ""
            }
    }

    // --------------------------------------------------
    // STATS
    // --------------------------------------------------

    private fun loadStats() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("jobs")
            .whereEqualTo("clientId", uid)
            .get()
            .addOnSuccessListener { snapshot ->

                tvTotalRequests.text =
                    "Total Requests: ${snapshot.size()}"
            }

        firestore.collection("jobs")
            .whereEqualTo("clientId", uid)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .addOnSuccessListener { snapshot ->

                tvCompletedRequests.text =
                    "Completed Requests: ${snapshot.size()}"
            }
    }

    // --------------------------------------------------
    // 🔥 LOGOUT
    // --------------------------------------------------

    private fun setupLogout() {

        btnLogout.setOnClickListener {

            auth.signOut()

            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), OnboardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}