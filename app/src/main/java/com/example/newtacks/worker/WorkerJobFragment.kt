package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.example.newtacks.models.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class WorkerJobFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvDetails: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnDone: Button
    private lateinit var layoutContent: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutBottomButtons: LinearLayout

    private var currentJobId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_worker_job, container, false)

        tvTitle = view.findViewById(R.id.tvJobTitle)
        tvDetails = view.findViewById(R.id.tvJobDetails)
        tvStatus = view.findViewById(R.id.tvJobStatus)
        btnDone = view.findViewById(R.id.btnRequestDone)
        layoutContent = view.findViewById(R.id.layoutContent)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        layoutBottomButtons = view.findViewById(R.id.layoutBottomButtons)

        listenForActiveJob()

        btnDone.setOnClickListener { requestDone() }


        return view
    }

    // --------------------------------------------------
    // 🔥 ACTIVE JOB LISTENER
    // --------------------------------------------------
    private fun listenForActiveJob() {
        val workerId = auth.currentUser?.uid ?: return
        listener = firestore.collection("jobs")
            .whereEqualTo("workerId", workerId)
            .whereIn("status", listOf("IN_PROGRESS", "PENDING_VERIFICATION"))
            .limit(1)
            .addSnapshotListener { snapshots, _ ->
                val job = snapshots?.documents?.firstOrNull()
                    ?.toObject(Job::class.java)
                if (job == null) {
                    showEmptyState()
                } else {
                    showActiveJob(job)
                }
            }
    }

    // --------------------------------------------------
    // UI STATE: ACTIVE JOB
    // --------------------------------------------------
    private fun showActiveJob(job: Job) {
        currentJobId = job.jobId
        layoutContent.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        layoutBottomButtons.visibility = View.VISIBLE


        tvTitle.text = job.jobTitle
        tvDetails.text = """
            Client: ${job.clientName}
            Service: ${job.serviceCategory}
            ₱${job.offeredAmount}
        """.trimIndent()

        // status badge
        tvStatus.visibility = View.VISIBLE
        when (job.status) {
            "IN_PROGRESS" -> {
                tvStatus.text = "Working on job..."
                tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
            }
            "PENDING_VERIFICATION" -> {
                tvStatus.text = "Waiting for client confirmation"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
            }
            else -> {
                tvStatus.text = "Active"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_blue)
            }
        }

        btnDone.visibility =
            if (job.status == "IN_PROGRESS") View.VISIBLE else View.GONE
    }

    // --------------------------------------------------
    // UI STATE: EMPTY
    // --------------------------------------------------
    private fun showEmptyState() {
        currentJobId = null
        layoutContent.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        layoutBottomButtons.visibility = View.GONE  // ← clean

        tvTitle.text = "No Active Job"
        tvStatus.visibility = View.GONE
        tvStatus.background = null
        btnDone.visibility = View.GONE
    }



    // --------------------------------------------------
    // ACTION: REQUEST DONE
    // --------------------------------------------------
    private fun requestDone() {
        val jobId = currentJobId ?: return
        firestore.collection("jobs")
            .document(jobId)
            .update(
                mapOf(
                    "status" to "PENDING_VERIFICATION",
                    "completedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Marked as done. Waiting for client verification.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}