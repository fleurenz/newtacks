package com.example.newtacks.client

import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.example.newtacks.models.Job
import com.example.newtacks.models.Receipt
import com.example.newtacks.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class ClientRequestsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvDetails: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnReject: Button
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutContent: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var layoutProgressLabels: LinearLayout

    private var currentJobId: String? = null
    private var lastCancelTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_client_requests, container, false)

        tvTitle          = view.findViewById(R.id.tvRequestTitle)
        tvDetails        = view.findViewById(R.id.tvRequestDetails)
        btnConfirm       = view.findViewById(R.id.btnConfirm)
        btnReject        = view.findViewById(R.id.btnReject)
        progressText     = view.findViewById(R.id.tvProgress)
        progressBar      = view.findViewById(R.id.progressBar)
        layoutContent    = view.findViewById(R.id.layoutContent)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        layoutProgressLabels = view.findViewById(R.id.layoutProgressLabels)

        listenForActiveJob()
        btnConfirm.setOnClickListener { confirmJob() }
        btnReject.setOnClickListener { rejectJob() }

        return view
    }

    // --------------------------------------------------
    // 🔥 REALTIME ACTIVE JOB LISTENER
    // --------------------------------------------------
    private fun listenForActiveJob() {
        val clientId = auth.currentUser?.uid ?: return
        listener = firestore.collection("jobs")
            .whereEqualTo("clientId", clientId)
            .whereIn("status", listOf("AVAILABLE", "IN_PROGRESS", "PENDING_VERIFICATION"))
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                val job = snapshots?.documents?.firstOrNull()?.toObject(Job::class.java)
                if (job == null) {
                    showEmptyState()
                } else {
                    showActiveJob(job)
                    animateUpdate()
                }
            }
    }

    // --------------------------------------------------
    // 🔥 UI STATE: ACTIVE JOB
    // --------------------------------------------------
    private fun showActiveJob(job: Job) {
        // Show content, hide empty state
        layoutContent.visibility    = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        progressText.visibility     = View.VISIBLE
        progressBar.visibility      = View.VISIBLE
        layoutProgressLabels.visibility = View.VISIBLE

        currentJobId  = job.jobId
        tvTitle.text  = job.jobTitle
        tvDetails.text = """
            Service: ${job.serviceCategory}
            Worker: ${job.workerName ?: "Waiting for worker..."}
            Price: ₱${job.offeredAmount}
        """.trimIndent()

        // ===== BADGE TEXT + STYLE =====
        when (job.status) {
            "AVAILABLE" -> {
                progressText.text = "Waiting for worker..."
                progressText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                progressText.setBackgroundResource(R.drawable.bg_badge_blue)
            }
            "IN_PROGRESS" -> {
                progressText.text = "Worker is working"
                progressText.setTextColor(android.graphics.Color.parseColor("#D97706"))
                progressText.setBackgroundResource(R.drawable.bg_badge_yellow)
            }
            "PENDING_VERIFICATION" -> {
                progressText.text = "Ready for confirmation"
                progressText.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                progressText.setBackgroundResource(R.drawable.bg_badge_green)
            }
            else -> {
                progressText.text = "Active"
                progressText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                progressText.setBackgroundResource(R.drawable.bg_badge_blue)
            }
        }

        progressBar.progress = when (job.status) {
            "AVAILABLE"            -> 25
            "IN_PROGRESS"          -> 60
            "PENDING_VERIFICATION" -> 100
            else                   -> 0
        }

        val isPending = job.status == "PENDING_VERIFICATION"
        btnConfirm.visibility = if (isPending) View.VISIBLE else View.GONE
        btnReject.visibility  = if (isPending) View.VISIBLE else View.GONE
    }

    // --------------------------------------------------
    // 🔥 EMPTY STATE
    // --------------------------------------------------
    private fun showEmptyState() {
        currentJobId = null

        layoutContent.visibility    = View.GONE
        layoutEmptyState.visibility = View.VISIBLE

        progressText.visibility = View.GONE
        progressBar.visibility  = View.GONE
        tvTitle.text            = ""
        btnConfirm.visibility   = View.GONE
        btnReject.visibility    = View.GONE
        layoutProgressLabels.visibility = View.GONE
    }

    // --------------------------------------------------
    // 🔥 ANIMATION
    // --------------------------------------------------
    private fun animateUpdate() {
        val anim = AlphaAnimation(0.4f, 1.0f)
        anim.duration = 300
        view?.startAnimation(anim)
    }

    // --------------------------------------------------
    // 🔥 CONFIRM JOB
    // --------------------------------------------------
    private fun confirmJob() {
        val jobId = currentJobId ?: return
        firestore.collection("jobs")
            .document(jobId)
            .update(
                mapOf(
                    "status"      to "COMPLETED",
                    "completedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Job Completed", Toast.LENGTH_SHORT).show()
                fetchJobAndGenerateReceipt(jobId)
            }
    }

    // --------------------------------------------------
    // 🔥 FETCH JOB
    // --------------------------------------------------
    private fun fetchJobAndGenerateReceipt(jobId: String) {
        firestore.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { doc ->
                val job = doc.toObject(Job::class.java)
                if (job != null) generateReceipt(job)
            }
    }

    // --------------------------------------------------
    // 🔥 RECEIPT
    // --------------------------------------------------
    private fun generateReceipt(job: Job) {
        val workerId  = job.workerId ?: return
        val receiptId = firestore.collection("receipts").document().id
        val receipt   = Receipt(
            receiptId       = receiptId,
            jobId           = job.jobId ?: "",
            clientId        = job.clientId,
            workerId        = workerId,
            clientName      = job.clientName,
            workerName      = job.workerName ?: "",
            jobTitle        = job.jobTitle,
            serviceCategory = job.serviceCategory,
            amount          = job.offeredAmount
        )
        firestore.collection("receipts")
            .document(receiptId)
            .set(receipt)
            .addOnSuccessListener {
                sendVerificationNotification(job.clientId)
                showReviewDialog(job)
            }
    }

    // --------------------------------------------------
    // 🔥 NOTIFICATION
    // --------------------------------------------------
    private fun sendVerificationNotification(clientId: String) {
        firestore.collection("notifications")
            .add(
                mapOf(
                    "to"      to clientId,
                    "title"   to "Job Ready for Verification",
                    "message" to "Your worker has completed the job."
                )
            )
    }

    // --------------------------------------------------
    // 🔥 REJECT / COOLDOWN
    // --------------------------------------------------
    private fun rejectJob() {
        val now = System.currentTimeMillis()
        if (now - lastCancelTime < 10_000) {
            Toast.makeText(requireContext(), "Please wait before cancelling again", Toast.LENGTH_SHORT).show()
            return
        }
        lastCancelTime = now
        val jobId = currentJobId ?: return
        firestore.collection("jobs")
            .document(jobId)
            .update("status", "IN_PROGRESS")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Returned to worker", Toast.LENGTH_SHORT).show()
            }
    }

    // --------------------------------------------------
    // 🔥 REVIEW DIALOG
    // --------------------------------------------------
    private fun showReviewDialog(job: Job) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val ratingBar  = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etComment  = dialogView.findViewById<EditText>(R.id.etComment)

        AlertDialog.Builder(requireContext())
            .setTitle("Rate Worker")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val review = Review(
                    reviewId = firestore.collection("reviews").document().id,
                    jobId    = job.jobId ?: "",
                    clientId = job.clientId,
                    workerId = job.workerId ?: "",
                    rating   = ratingBar.rating,
                    comment  = etComment.text.toString()
                )
                saveReview(review)
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    // --------------------------------------------------
    // 🔥 SAVE REVIEW
    // --------------------------------------------------
    private fun saveReview(review: Review) {
        firestore.collection("reviews")
            .document(review.reviewId)
            .set(review)
            .addOnSuccessListener { updateWorkerRating(review) }
    }

    // --------------------------------------------------
    // 🔥 UPDATE WORKER RATING
    // --------------------------------------------------
    private fun updateWorkerRating(review: Review) {
        val workerRef = firestore.collection("users").document(review.workerId)
        firestore.runTransaction { transaction ->
            val snapshot   = transaction.get(workerRef)
            val currentAvg = snapshot.getDouble("ratingAverage") ?: 0.0
            val count      = snapshot.getLong("ratingCount") ?: 0
            val newCount   = count + 1
            val newAvg     = ((currentAvg * count) + review.rating) / newCount
            transaction.update(
                workerRef,
                mapOf(
                    "ratingAverage" to newAvg,
                    "ratingCount"   to newCount
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}