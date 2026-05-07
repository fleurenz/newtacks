package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.newtacks.R
import com.example.newtacks.models.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkerJobFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvTitle: TextView
    private lateinit var tvDetails: TextView
    private lateinit var btnRequestDone: Button

    private var currentJobId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_worker_job, container, false)

        tvTitle = view.findViewById(R.id.tvJobTitle)
        tvDetails = view.findViewById(R.id.tvJobDetails)
        btnRequestDone = view.findViewById(R.id.btnRequestDone)

        loadActiveJob()

        btnRequestDone.setOnClickListener {
            requestJobDone()
        }

        return view
    }

    private fun loadActiveJob() {

        val workerId = auth.currentUser?.uid ?: return

        db.collection("jobs")
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", "IN_PROGRESS")
            .addSnapshotListener { snapshots, _ ->

                if (snapshots == null || snapshots.isEmpty) {
                    tvTitle.text = "No Active Job"
                    tvDetails.text = ""
                    btnRequestDone.visibility = View.GONE
                    return@addSnapshotListener
                }

                val doc = snapshots.documents[0]
                val job = doc.toObject(Job::class.java)

                if (job != null) {

                    currentJobId = job.jobId

                    tvTitle.text = job.jobTitle
                    tvDetails.text =
                        "${job.serviceCategory}\n${job.clientAddress}\n₱${job.offeredAmount}"

                    btnRequestDone.visibility = View.VISIBLE
                }
            }
    }

    private fun requestJobDone() {

        val jobId = currentJobId ?: return

        db.collection("jobs")
            .document(jobId)
            .update("status", "PENDING_VERIFICATION")
            .addOnSuccessListener {

                Toast.makeText(requireContext(),
                    "Waiting for client verification",
                    Toast.LENGTH_SHORT).show()
            }
    }
}