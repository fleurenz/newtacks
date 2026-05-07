package com.example.newtacks.client

import android.os.Bundle
import android.view.View
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

class ClientHistoryFragment : Fragment(R.layout.fragment_client_history) {

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReceiptAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReceiptAdapter { receipt ->
            ReceiptDetailActivity.open(requireContext(), receipt.receiptId)
        }

        recyclerView.adapter = adapter

        listenReceipts()
    }

    private fun listenReceipts() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        listener = db.collection("receipts")
            .whereEqualTo("clientId", uid)
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