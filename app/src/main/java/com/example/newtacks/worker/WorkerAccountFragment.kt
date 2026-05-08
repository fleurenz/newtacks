package com.example.newtacks.worker

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.example.newtacks.authentication.OnboardingActivity
import com.example.newtacks.models.Review
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
    private lateinit var tvSeeAllReviews: TextView
    private lateinit var filterAll: TextView
    private lateinit var filter5: TextView
    private lateinit var filter4: TextView
    private lateinit var filter3: TextView
    private lateinit var filter2: TextView
    private lateinit var filter1: TextView
    private lateinit var btnLogout: Button
    private var currentRatingFilter: Int? = null
    private var showingAll: Boolean = false
    private lateinit var layoutHeader: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_worker_account, container, false)

        tvWorkerName    = view.findViewById(R.id.tvWorkerName)
        tvWorkerRating  = view.findViewById(R.id.tvWorkerRating)
        tvAcceptedJobs  = view.findViewById(R.id.tvAcceptedJobs)
        tvCompletedJobs = view.findViewById(R.id.tvCompletedJobs)
        reviewContainer = view.findViewById(R.id.reviewContainer)
        tvSeeAllReviews = view.findViewById(R.id.tvSeeAllReviews)
        filterAll       = view.findViewById(R.id.filterAll)
        filter5         = view.findViewById(R.id.filter5)
        filter4         = view.findViewById(R.id.filter4)
        filter3         = view.findViewById(R.id.filter3)
        filter2         = view.findViewById(R.id.filter2)
        filter1         = view.findViewById(R.id.filter1)
        btnLogout       = view.findViewById(R.id.btnLogout)
        layoutHeader    = view.findViewById(R.id.layoutHeader)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            layoutHeader.setPadding(
                layoutHeader.paddingLeft,
                systemBars.top + resources.getDimensionPixelSize(R.dimen.header_padding_top),
                layoutHeader.paddingRight,
                layoutHeader.paddingBottom
            )
            insets
        }

        // ✅ First load when fragment is created
        loadProfile()
        loadStats()
        loadReviews()
        setupLogout()
        setupSeeAllReviews()
        setupFilters()

        return view
    }

    private fun setupSeeAllReviews() {
        tvSeeAllReviews.setOnClickListener {
            showingAll = !showingAll
            tvSeeAllReviews.text = if (showingAll) "Show less" else "See all"
            loadReviews(showingAll, currentRatingFilter)
        }
    }

    private fun setupFilters() {
        val filters = listOf(filterAll, filter5, filter4, filter3, filter2, filter1)
        val ratings = listOf(null, 5, 4, 3, 2, 1)

        filters.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                currentRatingFilter = ratings[index]
                showingAll = false
                tvSeeAllReviews.text = "See all"
                
                // Update UI selection
                filters.forEach { it.setBackgroundResource(R.drawable.bg_badge_white) }
                filters.forEach { it.setTextColor(android.graphics.Color.parseColor("#64748B")) }
                
                textView.setBackgroundResource(R.drawable.bg_badge_blue)
                textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                
                loadReviews(showingAll, currentRatingFilter)
            }
        }
    }

    // ✅ Fires every time this fragment is shown via show() in add/hide/show pattern
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadProfile()
            loadStats()
            loadReviews(showingAll, currentRatingFilter)
        }
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
                val name  = doc.getString("name") ?: "Worker"
                val avg   = doc.getDouble("ratingAverage") ?: 0.0
                val count = doc.getLong("ratingCount") ?: 0
                tvWorkerName.text   = name
                tvWorkerRating.text = "%.1f (%d reviews)".format(avg, count)
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
                tvAcceptedJobs.text = "${it.size()}"
            }
        firestore.collection("jobs")
            .whereEqualTo("workerId", uid)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .addOnSuccessListener {
                tvCompletedJobs.text = "${it.size()}"
            }
    }

    // --------------------------------------------------
    // REVIEWS
    // --------------------------------------------------
    @SuppressLint("MissingInflatedId")
    private fun loadReviews(showAll: Boolean = false, ratingFilter: Int? = null) {
        val uid = auth.currentUser?.uid ?: return
        
        var query = firestore.collection("reviews")
            .whereEqualTo("workerId", uid)
            
        if (ratingFilter != null) {
            query = query.whereEqualTo("rating", ratingFilter.toDouble())
        }

        query.get().addOnSuccessListener { snapshot ->
                reviewContainer.removeAllViews()
                
                if (snapshot.isEmpty) {
                    val empty = TextView(requireContext())
                    empty.text      = if (ratingFilter == null) "No reviews yet." else "No $ratingFilter star reviews yet."
                    empty.textSize  = 13f
                    empty.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                    reviewContainer.addView(empty)
                    tvSeeAllReviews.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val allDocs = snapshot.documents
                val displayDocs = if (showAll) allDocs else allDocs.take(5)

                if (allDocs.size > 5) {
                    tvSeeAllReviews.visibility = View.VISIBLE
                } else {
                    tvSeeAllReviews.visibility = View.GONE
                }

                for (doc in displayDocs) {
                    val review = doc.toObject(Review::class.java) ?: continue
                    val isAnonymous = review.isAnonymous
                    val clientName = if (isAnonymous) "Anonymous User" else review.clientName
                    val comment    = review.comment
                    val rating     = review.rating
                    val card = layoutInflater.inflate(
                        R.layout.item_review_card,
                        reviewContainer,
                        false
                    )
                    card.findViewById<TextView>(R.id.tvReviewRating).text =
                        "⭐ %.1f".format(rating)
                    card.findViewById<TextView>(R.id.tvReviewClient).text =
                        clientName
                    card.findViewById<TextView>(R.id.tvReviewComment).text =
                        comment
                    reviewContainer.addView(card)
                }
            }
    }

    // --------------------------------------------------
    // LOGOUT
    // --------------------------------------------------
    private fun setupLogout() {
        btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showLogoutConfirmDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_role_select)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<ImageView>(R.id.dialogIcon).setImageResource(R.drawable.ic_nav_account)
        dialog.findViewById<TextView>(R.id.dialogTitle).text   = "Logout"
        dialog.findViewById<TextView>(R.id.dialogMessage).text = "Are you sure you want to log out?"

        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogBtnPositive)
            .setOnClickListener {
                dialog.dismiss()
                auth.signOut()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogBtnNegative)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}