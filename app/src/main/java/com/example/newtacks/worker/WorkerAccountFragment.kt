package com.example.newtacks.worker

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkerAccountFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvWorkerName: TextView
    private lateinit var tvWorkerRating: TextView
    private lateinit var tvAcceptedJobs: TextView
    private lateinit var tvCompletedJobs: TextView
    private lateinit var reviewContainer: LinearLayout
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_worker_account,
            container,
            false
        )

        // ---------------- VIEWS ----------------
        tvWorkerName = view.findViewById(R.id.tvWorkerName)
        tvWorkerRating = view.findViewById(R.id.tvWorkerRating)
        tvAcceptedJobs = view.findViewById(R.id.tvAcceptedJobs)
        tvCompletedJobs = view.findViewById(R.id.tvCompletedJobs)
        reviewContainer = view.findViewById(R.id.reviewContainer)
        btnLogout = view.findViewById(R.id.btnLogout)

        // ---------------- LOAD DATA ----------------
        loadProfile()
        loadStats()
        loadReviews()

        // ---------------- LOGOUT ----------------
        btnLogout.setOnClickListener {

            auth.signOut()

            val intent = Intent(
                requireContext(),
                com.example.newtacks.authentication.OnboardingActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }

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

                val name = doc.getString("name") ?: "Worker"

                val avg = doc.getDouble("ratingAverage") ?: 0.0
                val count = doc.getLong("ratingCount") ?: 0

                tvWorkerName.text = name
                tvWorkerRating.text =
                    "⭐ %.1f (%d reviews)".format(avg, count)
            }
    }

    // --------------------------------------------------
    // STATS
    // --------------------------------------------------

    private fun loadStats() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("jobs")
            .whereEqualTo("workerId", uid)
            .get()
            .addOnSuccessListener {

                tvAcceptedJobs.text =
                    "Accepted Jobs: ${it.size()}"
            }

        firestore.collection("jobs")
            .whereEqualTo("workerId", uid)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .addOnSuccessListener {

                tvCompletedJobs.text =
                    "Completed Jobs: ${it.size()}"
            }
    }

    // --------------------------------------------------
    // REVIEWS
    // --------------------------------------------------

    private fun loadReviews() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("reviews")
            .whereEqualTo("workerId", uid)
            .get()
            .addOnSuccessListener { snapshot ->

                reviewContainer.removeAllViews()

                for (doc in snapshot.documents) {

                    val clientName =
                        doc.getString("clientName") ?: "Anonymous"

                    val comment =
                        doc.getString("comment") ?: ""

                    val rating =
                        doc.getDouble("rating") ?: 0.0

                    val textView = TextView(requireContext())

                    textView.text =
                        """
                        ⭐ $rating
                        By: $clientName

                        $comment
                        """.trimIndent()

                    textView.textSize = 16f
                    textView.setPadding(0, 0, 0, 32)

                    reviewContainer.addView(textView)
                }
            }
    }
}