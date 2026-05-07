package com.example.newtacks.client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.newtacks.CreateJobActivity
import com.example.newtacks.R

class ClientHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_client_home, container, false)

        val redButton = view.findViewById<Button>(R.id.btnCreateJob)

        redButton.setOnClickListener {

            val intent = Intent(requireContext(), CreateJobActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}