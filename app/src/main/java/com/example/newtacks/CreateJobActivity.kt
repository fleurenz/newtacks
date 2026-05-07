package com.example.newtacks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.newtacks.models.Job
import com.example.newtacks.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.*

class CreateJobActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etJobTitle: EditText
    private lateinit var etClientName: EditText
    private lateinit var etClientAddress: EditText

    private lateinit var spinnerServiceType: Spinner
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var etDuration: EditText
    private lateinit var etOfferAmount: EditText
    private lateinit var etDescription: EditText

    private lateinit var btnCancel: Button
    private lateinit var btnSubmit: Button

    private var selectedDate = ""
    private var selectedTime = ""

    private var isUserEditingTitle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_job)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // ===== TOOLBAR =====
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        initializeViews()
        loadClientInformation()
        setupServiceSpinner()
        setupDatePicker()
        setupTimePicker()

        btnCancel.setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submitJob() }
    }

    // ---------------- INIT ----------------

    private fun initializeViews() {

        etJobTitle = findViewById(R.id.etJobTitle)
        etClientName = findViewById(R.id.etClientName)
        etClientAddress = findViewById(R.id.etClientAddress)

        spinnerServiceType = findViewById(R.id.spinnerServiceType)

        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)

        etDuration = findViewById(R.id.etDuration)
        etOfferAmount = findViewById(R.id.etOfferAmount)
        etDescription = findViewById(R.id.etDescription)

        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)

        // detect manual editing
        etJobTitle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) isUserEditingTitle = true
        }
    }

    // ---------------- USER DATA ----------------

    private fun loadClientInformation() {

        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->

                val user = doc.toObject(User::class.java)

                if (user != null) {
                    etClientName.setText(user.name)
                    etClientAddress.setText(user.address)

                    etClientName.isEnabled = false
                    etClientAddress.isEnabled = false
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- SERVICE TYPE + TITLE ----------------

    private fun setupServiceSpinner() {

        val services = arrayOf(
            "Plumbing",
            "Electrical",
            "Carpentry"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            services
        )

        spinnerServiceType.adapter = adapter

        spinnerServiceType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val selectedService = services[position]
                    val generatedTitle = "$selectedService Request"

                    if (!isUserEditingTitle) {
                        etJobTitle.setText(generatedTitle)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ---------------- DATE PICKER ----------------

    private fun setupDatePicker() {

        btnSelectDate.setOnClickListener {

            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->

                    selectedDate = "${month + 1}/$day/$year"
                    btnSelectDate.text = selectedDate

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // ---------------- TIME PICKER ----------------

    private fun setupTimePicker() {

        btnSelectTime.setOnClickListener {

            val calendar = Calendar.getInstance()

            TimePickerDialog(
                this,
                { _, hour, minute ->

                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                    btnSelectTime.text = selectedTime

                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // ---------------- SUBMIT JOB ----------------

    private fun submitJob() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val jobTitle = etJobTitle.text.toString().trim()
        val clientName = etClientName.text.toString().trim()
        val clientAddress = etClientAddress.text.toString().trim()

        val serviceCategory = spinnerServiceType.selectedItem.toString()

        val durationInput = etDuration.text.toString().trim()
        val offerInput = etOfferAmount.text.toString().trim()

        val description = etDescription.text.toString().trim()

        // ---------------- VALIDATION ----------------

        if (
            jobTitle.isEmpty() ||
            clientName.isEmpty() ||
            clientAddress.isEmpty() ||
            selectedDate.isEmpty() ||
            selectedTime.isEmpty() ||
            durationInput.isEmpty() ||
            offerInput.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val estimatedDuration = durationInput.toDoubleOrNull()
        val offeredAmount = offerInput.toDoubleOrNull()

        if (estimatedDuration == null || offeredAmount == null) {
            Toast.makeText(this, "Invalid number input", Toast.LENGTH_SHORT).show()
            return
        }

        // ---------------- UI LOCK ----------------

        // ---------------- CHECK ACTIVE JOB FIRST ----------------

        firestore.collection("jobs")
            .whereEqualTo("clientId", currentUser.uid)
            .whereIn(
                "status",
                listOf(
                    "AVAILABLE",
                    "IN_PROGRESS",
                    "PENDING_VERIFICATION"
                )
            )
            .get()
            .addOnSuccessListener { snapshots ->

                // CLIENT ALREADY HAS ACTIVE JOB
                if (!snapshots.isEmpty) {

                    Toast.makeText(
                        this,
                        "You already have an active request",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // ---------------- CREATE NEW JOB ----------------

                btnSubmit.isEnabled = false

                val jobId = firestore.collection("jobs").document().id

                val job = Job(

                    jobId = jobId,

                    clientId = currentUser.uid,
                    clientName = clientName,
                    clientAddress = clientAddress,

                    jobTitle = jobTitle,
                    serviceCategory = serviceCategory,

                    scheduledDate = selectedDate,
                    scheduledTime = selectedTime,

                    estimatedDurationHours = estimatedDuration,
                    offeredAmount = offeredAmount,

                    description = description,

                    status = "AVAILABLE"
                )

                firestore.collection("jobs")
                    .document(jobId)
                    .set(job)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Job Submitted Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(
                            this,
                            ClientDashboardActivity::class.java
                        )

                        intent.putExtra(
                            ClientDashboardActivity.OPEN_FRAGMENT,
                            "REQUESTS"
                        )

                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                    }
                    .addOnFailureListener {

                        btnSubmit.isEnabled = true

                        Toast.makeText(
                            this,
                            "Failed to submit job",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            .addOnSuccessListener {

                Toast.makeText(this, "Job Submitted Successfully", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, ClientDashboardActivity::class.java)

                intent.putExtra(
                    ClientDashboardActivity.OPEN_FRAGMENT,
                    "REQUESTS"
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
            }
            .addOnFailureListener {

                btnSubmit.isEnabled = true

                Toast.makeText(this, "Failed to submit job", Toast.LENGTH_SHORT).show()
            }
    }
}