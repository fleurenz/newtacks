package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newtacks.R
import com.example.newtacks.models.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WorkerFeedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkerJobAdapter
    private val db = FirebaseFirestore.getInstance()

    private var listener: ListenerRegistration? = null
    private val jobList = mutableListOf<Job>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_worker_feed, container, false)

        recyclerView = view.findViewById(R.id.workerFeedRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = WorkerJobAdapter(jobList) { job ->
            showJobPreview(job)
        }

        recyclerView.adapter = adapter

        listenForJobs()

        return view
    }

    // --------------------------------------------------
    // LIVE JOB FEED
    // --------------------------------------------------

    private fun listenForJobs() {

        listener = db.collection("jobs")
            .whereEqualTo("status", "AVAILABLE")
            .addSnapshotListener { snapshots, _ ->

                if (snapshots == null) return@addSnapshotListener

                jobList.clear()

                for (doc in snapshots) {
                    val job = doc.toObject(Job::class.java)
                    jobList.add(job)
                }

                adapter.notifyDataSetChanged()
            }
    }

    // --------------------------------------------------
    // JOB PREVIEW POPUP
    // --------------------------------------------------

    private fun showJobPreview(job: Job) {

        val view = layoutInflater.inflate(R.layout.dialog_job_preview, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDetails = view.findViewById<TextView>(R.id.tvDetails)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        tvTitle.text = job.jobTitle

        tvDetails.text = """
            Category: ${job.serviceCategory}
            Client: ${job.clientName}
            Address: ${job.clientAddress}
            Price: ₱${job.offeredAmount}
            Description: ${job.description}
        """.trimIndent()

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnAccept.setOnClickListener {
            dialog.dismiss()
            acceptJob(job)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    // --------------------------------------------------
    // ACCEPT JOB (FIRESTORE TRANSACTION)
    // --------------------------------------------------

    private fun acceptJob(job: Job) {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val workerId = currentUser.uid

        db.collection("users")
            .document(workerId)
            .get()
            .addOnSuccessListener { doc ->

                val workerName = doc.getString("name") ?: "Worker"

                db.runTransaction { transaction ->

                    val ref = db.collection("jobs").document(job.jobId)
                    val snapshot = transaction.get(ref)

                    val status = snapshot.getString("status")

                    if (status != "AVAILABLE") {
                        throw Exception("Job already taken")
                    }

                    transaction.update(ref, mapOf(
                        "status" to "IN_PROGRESS",
                        "workerId" to workerId,
                        "workerName" to workerName,
                        "acceptedAt" to System.currentTimeMillis()
                    ))
                }
            }
    }

    // --------------------------------------------------
    // CLEANUP
    // --------------------------------------------------

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}