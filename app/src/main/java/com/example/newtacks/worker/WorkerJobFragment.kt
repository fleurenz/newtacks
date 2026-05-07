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

        listenForActiveJob()

        btnDone.setOnClickListener {
            requestDone()
        }

        return view
    }

    // --------------------------------------------------
    // 🔥 ACTIVE JOB LISTENER (SINGLE LIFECYCLE)
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
    // UI STATE
    // --------------------------------------------------

    private fun showActiveJob(job: Job) {

        currentJobId = job.jobId

        tvTitle.text = job.jobTitle

        tvDetails.text = """
            Client: ${job.clientName}
            Service: ${job.serviceCategory}
            ₱${job.offeredAmount}
        """.trimIndent()

        tvStatus.text = when (job.status) {

            "IN_PROGRESS" -> "Working on job..."
            "PENDING_VERIFICATION" -> "Waiting for client confirmation"
            else -> "Active"
        }

        btnDone.visibility =
            if (job.status == "IN_PROGRESS") View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {

        currentJobId = null

        tvTitle.text = "No Active Job"
        tvDetails.text = ""
        tvStatus.text = ""

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