package com.example.newtacks.receipt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newtacks.R
import com.example.newtacks.models.Receipt

class ReceiptAdapter(
    private val onClick: (Receipt) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

    private val receipts = mutableListOf<Receipt>()

    fun submitList(newList: List<Receipt>) {
        receipts.clear()
        receipts.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)

        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(receipts[position])
    }

    override fun getItemCount(): Int = receipts.size

    inner class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.tvReceiptTitle)
        private val amount: TextView = itemView.findViewById(R.id.tvReceiptAmount)
        private val worker: TextView = itemView.findViewById(R.id.tvReceiptWorker)

        fun bind(receipt: Receipt) {

            title.text = receipt.jobTitle
            amount.text = "₱${receipt.amount}"
            worker.text = "Worker: ${receipt.workerName}"

            itemView.setOnClickListener {
                onClick(receipt)
            }
        }
    }
}