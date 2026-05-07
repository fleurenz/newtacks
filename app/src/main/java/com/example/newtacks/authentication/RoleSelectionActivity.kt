package com.example.newtacks.authentication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.newtacks.R

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var roleGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        roleGroup = findViewById(R.id.roleGroup)

        // Card click → check the radio inside
        findViewById<LinearLayout>(R.id.cardClient).setOnClickListener {
            roleGroup.check(R.id.rbClient)
        }
        findViewById<LinearLayout>(R.id.cardWorker).setOnClickListener {
            roleGroup.check(R.id.rbWorker)
        }
        findViewById<LinearLayout>(R.id.cardCompany).setOnClickListener {
            roleGroup.check(R.id.rbCompany)
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            handleContinue()
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleContinue() {
        val selectedRole = when (roleGroup.checkedRadioButtonId) {
            R.id.rbClient -> "CLIENT"
            R.id.rbWorker -> "WORKER"
            R.id.rbCompany -> "COMPANY"
            else -> null
        }
        if (selectedRole == null) {
            showDialog("Please select a role first")
            return
        }
        confirmRole(selectedRole)
    }

    private fun confirmRole(role: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Role")
            .setMessage("Are you sure you want to register as $role?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, SignupActivity::class.java)
                intent.putExtra("ROLE", role)
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDialog(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Missing Selection")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}