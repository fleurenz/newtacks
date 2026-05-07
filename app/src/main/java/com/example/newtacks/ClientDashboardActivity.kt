package com.example.newtacks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.newtacks.client.ClientAccountFragment
import com.example.newtacks.client.ClientHistoryFragment
import com.example.newtacks.client.ClientHomeFragment
import com.example.newtacks.client.ClientRequestsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ClientDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_client_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.clientBottomNav)

        val fragmentToOpen = intent.getStringExtra(OPEN_FRAGMENT)

        if(fragmentToOpen == "REQUESTS"){
            replaceFragment(ClientRequestsFragment())
        }
        else{
            replaceFragment(ClientHomeFragment())
        }

        bottomNav.setOnItemSelectedListener {

            when(it.itemId){

                R.id.nav_home -> {
                    replaceFragment(ClientHomeFragment())
                    true
                }

                R.id.nav_requests -> {
                    replaceFragment(ClientRequestsFragment())
                    true
                }

                R.id.nav_history -> {
                    replaceFragment(ClientHistoryFragment())
                    true
                }

                R.id.nav_account -> {
                    replaceFragment(ClientAccountFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.clientFragmentContainer, fragment)
            .commit()
    }
    companion object {
        const val OPEN_FRAGMENT = "OPEN_FRAGMENT"
    }
}