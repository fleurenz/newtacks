package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
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
            acceptJob(job)
        }

        recyclerView.adapter = adapter

        listenForJobs()

        return view
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }

    private fun acceptJob(job: Job) {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val workerId = currentUser.uid

        // FETCH REAL USER NAME FROM FIRESTORE
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
}