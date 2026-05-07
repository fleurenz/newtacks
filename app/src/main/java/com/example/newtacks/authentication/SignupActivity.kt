package com.example.newtacks.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newtacks.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var viewModel: SignupViewModel
    private var selectedRole: String = "CLIENT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        selectedRole = intent.getStringExtra("ROLE") ?: "CLIENT"

        val repo = AuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SignupViewModel(repo) as T
                }
            }
        )[SignupViewModel::class.java]

        setupUIByRole()

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btn = findViewById<Button>(R.id.btnRegister)

        btn.setOnClickListener {

            val emailText = email.text.toString()
            val passwordText = password.text.toString()
            val confirmText = confirm.text.toString()

            var name = ""
            var phone = ""
            var address = ""
            var companyName = ""
            var hrName = ""
            var experience: Int? = null
            val categories = mutableListOf<String>()

            when (selectedRole) {

                "CLIENT" -> {
                    name = findViewById<EditText>(R.id.etClientName).text.toString()
                    phone = findViewById<EditText>(R.id.etClientPhone).text.toString()
                    address = findViewById<EditText>(R.id.etClientAddress).text.toString()
                }

                "WORKER" -> {
                    name = findViewById<EditText>(R.id.etWorkerName).text.toString()
                    phone = findViewById<EditText>(R.id.etWorkerPhone).text.toString()
                    address = findViewById<EditText>(R.id.etWorkerAddress).text.toString()

                    val expText = findViewById<EditText>(R.id.etExperience).text.toString()
                    experience = if (expText.isNotEmpty()) expText.toInt() else 0

                    if (findViewById<CheckBox>(R.id.cbPlumbing).isChecked) categories.add("Plumbing")
                    if (findViewById<CheckBox>(R.id.cbElectrical).isChecked) categories.add("Electrical")
                    if (findViewById<CheckBox>(R.id.cbCarpentry).isChecked) categories.add("Carpentry")
                }

                "COMPANY" -> {
                    companyName = findViewById<EditText>(R.id.etCompanyName).text.toString()
                    hrName = findViewById<EditText>(R.id.etHRName).text.toString()
                    phone = findViewById<EditText>(R.id.etCompanyPhone).text.toString()
                    address = findViewById<EditText>(R.id.etCompanyAddress).text.toString()
                }
            }

            viewModel.register(
                email = emailText,
                password = passwordText,
                confirmPassword = confirmText,
                role = selectedRole,
                name = name,
                phone = phone,
                address = address,
                companyName = companyName,
                hrName = hrName,
                categories = categories,
                experience = experience
            )
        }

        observeState()

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }

    private fun setupUIByRole() {

        val clientGroup = findViewById<View>(R.id.clientGroup)
        val workerGroup = findViewById<View>(R.id.workerGroup)
        val companyGroup = findViewById<View>(R.id.companyGroup)

        clientGroup.visibility = View.GONE
        workerGroup.visibility = View.GONE
        companyGroup.visibility = View.GONE

        when (selectedRole) {

            "CLIENT" -> clientGroup.visibility = View.VISIBLE
            "WORKER" -> workerGroup.visibility = View.VISIBLE
            "COMPANY" -> companyGroup.visibility = View.VISIBLE
        }
    }

    private fun observeState() {

        viewModel.signupState.observe(this) { state ->

            when (state) {

                is SignupState.Loading -> {
                    Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()
                }

                is SignupState.Success -> {
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }

                is SignupState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }
}