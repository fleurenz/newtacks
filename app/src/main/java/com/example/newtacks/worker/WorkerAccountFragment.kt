package com.example.newtacks.worker

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.newtacks.R

class WorkerAccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_worker_account, container, false)

        val text = view.findViewById<TextView>(R.id.tvAccountPlaceholder)
        text.text = "Worker Account (Profile + Logout)"

        return view
    }
}