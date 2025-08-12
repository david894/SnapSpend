package com.kx.snapspend.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.R
import com.kx.snapspend.viewmodel.MainViewModel
import com.kx.snapspend.viewmodel.MainViewModelFactory

class ExpenseEntryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COLLECTION_NAME = "extra_collection_name"
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as BudgetTrackerApplication).repository,
            (application as BudgetTrackerApplication).firestoreRepository, // Pass it
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_entry)

        val collectionName = intent.getStringExtra(EXTRA_COLLECTION_NAME)
        if (collectionName == null) {
            finish() // Close if no collection name is provided
            return
        }

        val titleTextView: TextView = findViewById(R.id.entry_title)
        val amountEditText: EditText = findViewById(R.id.entry_amount)
        val confirmButton: Button = findViewById(R.id.entry_confirm_button)

        titleTextView.text = "Add to $collectionName"

        confirmButton.setOnClickListener {
            val amountText = amountEditText.text.toString()
            val amount = amountText.toDoubleOrNull()

            if (amount != null && amount > 0) {
                mainViewModel.addExpense(collectionName, amount, this)
                Toast.makeText(this, "Expense Added!", Toast.LENGTH_SHORT).show()
                finish() // Close the activity after adding
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}