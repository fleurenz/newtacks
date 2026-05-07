package com.example.newtacks.client

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.example.newtacks.models.Job
import com.example.newtacks.models.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ClientRequestsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var listener: ListenerRegistration? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvDetails: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnReject: Button

    private var currentJobId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_client_requests, container, false)

        tvTitle = view.findViewById(R.id.tvRequestTitle)
        tvDetails = view.findViewById(R.id.tvRequestDetails)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnReject = view.findViewById(R.id.btnReject)

        listenForPendingJobs()

        btnConfirm.setOnClickListener {
            confirmJob()
        }

        btnReject.setOnClickListener {
            rejectJob()
        }

        return view
    }

    // ---------------- REALTIME LISTENER ----------------

    private fun listenForPendingJobs() {

        val clientId = auth.currentUser?.uid ?: return

        listener = firestore.collection("jobs")
            .whereEqualTo("clientId", clientId)
            .whereEqualTo("status", "PENDING_VERIFICATION")
            .addSnapshotListener { snapshots, _ ->

                if (snapshots == null || snapshots.isEmpty) {
                    showEmptyState()
                    return@addSnapshotListener
                }

                val doc = snapshots.documents[0]
                val job = doc.toObject(Job::class.java)

                if (job != null) {
                    showPendingJob(job)
                }
            }
    }

    // ---------------- UI STATES ----------------

    private fun showPendingJob(job: Job) {

        currentJobId = job.jobId

        tvTitle.text = "Verify Completed Job"
        tvDetails.text =
            """
            ${job.jobTitle}
            ${job.serviceCategory}
            Worker: ${job.workerName ?: "N/A"}
            ₱${job.offeredAmount}
            """.trimIndent()

        btnConfirm.visibility = View.VISIBLE
        btnReject.visibility = View.VISIBLE
    }

    private fun showEmptyState() {

        currentJobId = null

        tvTitle.text = "No Pending Verification"
        tvDetails.text = ""

        btnConfirm.visibility = View.GONE
        btnReject.visibility = View.GONE
    }

    // ---------------- ACTIONS ----------------

    private fun confirmJob() {

        val jobId = currentJobId ?: return

        firestore.collection("jobs")
            .document(jobId)
            .update("status", "COMPLETED")
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Job Completed",
                    Toast.LENGTH_SHORT
                ).show()

                // FETCH FULL JOB FIRST
                firestore.collection("jobs")
                    .document(jobId)
                    .get()
                    .addOnSuccessListener { doc ->

                        val job = doc.toObject(Job::class.java)

                        if (job != null) {
                            generateReceipt(job)
                        }
                    }
            }
    }

    private fun rejectJob() {

        val jobId = currentJobId ?: return

        firestore.collection("jobs")
            .document(jobId)
            .update("status", "IN_PROGRESS")
            .addOnSuccessListener {

                Toast.makeText(
                    requireContext(),
                    "Returned to Worker",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ---------------- RECEIPT GENERATION ----------------

    private fun generateReceipt(job: Job) {

        val workerId = job.workerId ?: return

        val receiptId = firestore.collection("receipts").document().id

        val receipt = Receipt(

            receiptId = receiptId,
            jobId = job.jobId,

            clientId = job.clientId,
            workerId = workerId,

            clientName = job.clientName,
            workerName = job.workerName ?: "",

            jobTitle = job.jobTitle,
            serviceCategory = job.serviceCategory,

            amount = job.offeredAmount
        )

        firestore.collection("receipts")
            .document(receiptId)
            .set(receipt)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}