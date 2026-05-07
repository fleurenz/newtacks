package com.example.newtacks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.newtacks.worker.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class WorkerDashboardActivity : AppCompatActivity() {

    companion object {
        const val OPEN_FRAGMENT = "OPEN_FRAGMENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.workerBottomNav)

        val startFragment = intent.getStringExtra(OPEN_FRAGMENT)

        when (startFragment) {
            "JOB" -> replaceFragment(WorkerJobFragment())
            "HISTORY" -> replaceFragment(WorkerHistoryFragment())
            "ACCOUNT" -> replaceFragment(WorkerAccountFragment())
            else -> replaceFragment(WorkerFeedFragment())
        }

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_feed -> {
                    replaceFragment(WorkerFeedFragment())
                    true
                }

                R.id.nav_job -> {
                    replaceFragment(WorkerJobFragment())
                    true
                }

                R.id.nav_history -> {
                    replaceFragment(WorkerHistoryFragment())
                    true
                }

                R.id.nav_account -> {
                    replaceFragment(WorkerAccountFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.workerFragmentContainer, fragment)
            .commit()
    }
}