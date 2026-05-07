package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newtacks.R
import com.example.newtacks.receipt.ReceiptAdapter
import com.example.newtacks.receipt.ReceiptDetailActivity
import com.example.newtacks.models.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WorkerHistoryFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReceiptAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_worker_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerWorkerHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReceiptAdapter { receipt ->
            ReceiptDetailActivity.open(requireContext(), receipt.receiptId)
        }

        recyclerView.adapter = adapter

        listenWorkerReceipts()

        return view
    }

    private fun listenWorkerReceipts() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        listener = db.collection("receipts")
            .whereEqualTo("workerId", uid)
            .addSnapshotListener { snapshots, _ ->

                val receipts = snapshots?.toObjects(Receipt::class.java) ?: emptyList()

                adapter.submitList(receipts)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}