package com.kx.snapspend.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.kx.snapspend.data.repository.BudgetTrackerRepository
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import com.kx.snapspend.model.MonthlyTotal
import com.kx.snapspend.workers.LocationProcessingWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.kx.snapspend.workers.BudgetAlertWorker
import com.kx.snapspend.data.UserPreferences // Add this import
import com.kx.snapspend.data.repository.FirestoreRepository
import com.kx.snapspend.model.Member
import com.kx.snapspend.model.SharedCollection
import com.kx.snapspend.model.SharedExpense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers // Add this import
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext // Add this import
import kotlinx.coroutines.flow.*
import java.util.*


// UPDATED: This now includes the budget and progress
data class CollectionSpending(
    val collectionName: String,
    val totalAmount: Double,
    val budget: Double,
    val progress: Float,
    val iconName: String,
    val colorHex: String
)

// UPDATED: DashboardSummary is the same, but its contents are richer
data class DashboardSummary(
    val currentMonthTotal: Double = 0.0,
    val totalBudget: Double = 0.0, // Add this
    val percentageChange: Double = 0.0,
    val changeType: ChangeType = ChangeType.SAME,
    val spendingBreakdown: List<CollectionSpending> = emptyList()
)

enum class ChangeType { INCREASE, DECREASE, SAME }

class MainViewModel(
    private val repository: BudgetTrackerRepository,
    private val firestoreRepository: FirestoreRepository, // Add this
    private val context: Context
) : ViewModel() {

    val allCollections: LiveData<List<Collections>> = repository.allCollections
    private val _viewingDate = MutableStateFlow(Calendar.getInstance())
    val viewingDate: StateFlow<Calendar> = _viewingDate

    val monthlyExpenses: StateFlow<List<Expenses>> = _viewingDate
        .flatMapLatest { date ->
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date.time)
            repository.getExpensesForMonth(yearMonth)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chartData: StateFlow<List<MonthlyTotal>>
    val dashboardSummary: StateFlow<DashboardSummary>
    // NEW: A StateFlow to hold the user's name for the UI to observe
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName
    private var syncJob: Job? = null

    init {
        val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
        chartData = repository.getSpendingForLastSixMonths(sixMonthsAgo)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

        // ** THIS IS THE NEW LOGIC **
        // We now combine three sources of data to build our summary:
        // 1. The monthly expenses
        // 2. The list of all collections (to get their budgets)
        // 3. The currently viewed date
        dashboardSummary = combine(
            monthlyExpenses,
            allCollections.asFlow(),
            viewingDate
        ) { expenses, collections, date ->
            calculateSummary(expenses, collections, date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardSummary())

        _userName.value = UserPreferences.getUserName(context)
        startMasterSync()
    }

    private fun startMasterSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            allCollections.asFlow().collect { collections ->
                val sharedPins = collections.mapNotNull { it.sharePin }
                // For each shared collection, call the listener function
                sharedPins.forEach { pin ->
                    // Pass the viewModelScope to the listener function
                    listenForUpdates(this, pin)
                }
            }
        }
    }

    private fun listenForUpdates(scope: CoroutineScope, pin: String) {
        // Now, launch can be called on the provided scope.
        scope.launch {
            firestoreRepository.listenToSharedExpenses(pin).collect { sharedExpenses ->
                syncFirebaseExpensesToLocalRoom(pin, sharedExpenses)
            }
        }
        scope.launch {
            firestoreRepository.listenToSharedCollectionDetails(pin).collect { sharedCollection ->
                if (sharedCollection != null) {
                    syncFirebaseDetailsToLocalRoom(sharedCollection)
                }
            }
        }
    }


    // NEW: Function to save the user's name
    fun saveUserName(name: String) {
        UserPreferences.saveUserName(context, name)
        _userName.value = name // Update the state so the UI refreshes
    }

    // NEW: Function to save the consent status
    fun saveConsent(hasConsented: Boolean) {
        UserPreferences.saveConsent(context, hasConsented)
    }

    // NEW: Function to check consent status
    fun hasUserConsented(): Boolean {
        return UserPreferences.hasConsented(context)
    }

    // UPDATED: This function now accepts the list of collections
    private suspend fun calculateSummary(
        currentExpenses: List<Expenses>,
        allCollections: List<Collections>,
        viewingDate: Calendar
    ): DashboardSummary {
        // Create a quick lookup map of collection names to their full object
        val collectionMap = allCollections.associateBy { it.name }

        val spendingBreakdown = currentExpenses
            .groupBy { it.collectionName }
            .map { (name, expenses) ->
                val totalAmount = expenses.sumOf { it.amount }
                val collection = collectionMap[name]
                val budget = collection?.budget ?: 0.0
                val progress = if (budget > 0) (totalAmount / budget).toFloat() else 0f
                CollectionSpending(
                    collectionName = name,
                    totalAmount = totalAmount,
                    budget = budget,
                    progress = progress,
                    iconName = collection?.iconName ?: "Label", // Pass the icon name
                    colorHex = collection?.colorHex ?: "#FF6200EE" // Pass the color
                )
            }
            .sortedByDescending { it.totalAmount }

        val totalBudget = allCollections.sumOf { it.budget }
        // The rest of the summary calculation remains the same...
        val currentMonthTotal = currentExpenses.sumOf { it.amount }
        val calendar = viewingDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -1)
        val prevMonthEnd = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val prevMonthStart = calendar.timeInMillis
        val previousMonthTotal = repository.getTotalForDateRange(prevMonthStart, prevMonthEnd)

        val (percentageChange, changeType) = if (previousMonthTotal > 0) {
            val change = ((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100
            val type = when {
                change > 0 -> ChangeType.INCREASE
                change < 0 -> ChangeType.DECREASE
                else -> ChangeType.SAME
            }
            Pair(change, type)
        } else if (currentMonthTotal > 0) {
            Pair(100.0, ChangeType.INCREASE)
        } else {
            Pair(0.0, ChangeType.SAME)
        }

        return DashboardSummary(
            currentMonthTotal = currentMonthTotal,
            totalBudget = totalBudget, // Add it to the result
            percentageChange = percentageChange,
            changeType = changeType,
            spendingBreakdown = spendingBreakdown
        )
    }

    // All other functions (addExpense, deleteCollection, etc.) remain the same
    fun previousMonth() {
        val newDate = _viewingDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, -1)
        _viewingDate.value = newDate
    }

    fun nextMonth() {
        val newDate = _viewingDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, 1)
        _viewingDate.value = newDate
    }

    fun getTransactionsForCollection(collectionName: String): kotlinx.coroutines.flow.Flow<List<Expenses>> {
        return viewingDate.flatMapLatest { date ->
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date.time)
            repository.getTransactionsForCollectionInMonth(yearMonth, collectionName)
        }
    }

    fun getExpenseById(expenseId: Long): kotlinx.coroutines.flow.Flow<Expenses?> {
        return repository.getExpenseByIdFlow(expenseId)
    }

    fun updateExpense(expense: Expenses) {
        viewModelScope.launch {
            repository.updateExpense(expense)

            // 2. Check if this expense belongs to a shared collection.
            val collection = repository.getCollectionByName(expense.collectionName).first()
            if (collection?.sharePin != null && expense.cloudId != null) {
                // 3. If it's shared, push the entire updated expense to Firestore.
                val userName = UserPreferences.getUserName(context)
                val userId = Firebase.auth.currentUser?.uid ?: ""
                val sharedExpense = SharedExpense(
                    id = expense.cloudId,
                    amount = expense.amount,
                    timestamp = expense.timestamp,
                    adderUserId = userId, // Note: This marks the current user as the last editor.
                    adderUserName = userName,
                    locationCategory = expense.locationCategory,
                    latitude = expense.latitude,
                    longitude = expense.longitude
                )
                firestoreRepository.updateSharedExpense(collection.sharePin, sharedExpense)
            }
        }
    }

    fun deleteExpense(expense: Expenses) {
        viewModelScope.launch {
            repository.deleteExpense(expense)

            // 2. If it was a shared expense, delete it from Firestore as well.
            val collection = repository.getCollectionByName(expense.collectionName).first()
            if (collection?.sharePin != null && expense.cloudId != null) {
                firestoreRepository.deleteSharedExpense(collection.sharePin, expense.cloudId)
            }
        }
    }

    fun addCollection(name: String) {
        viewModelScope.launch {
            val newCollection = Collections(name = name, budget = 0.0)
            repository.insertCollection(newCollection)
        }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch {
            repository.deleteCollection(name)
        }
    }

    fun getCollectionByName(name: String): kotlinx.coroutines.flow.Flow<Collections?> {
        return repository.getCollectionByName(name)
    }

    fun addExpense(collectionName: String, amount: Double, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = repository.getCollectionByName(collectionName).first()
            val cloudId = UUID.randomUUID().toString() // Generate the unique cloud ID up front

            // 1. Save the expense locally with the cloudId but null location data
            val localExpense = Expenses(
                amount = amount,
                collectionName = collectionName,
                timestamp = System.currentTimeMillis(),
                cloudId = cloudId, // Assign the cloudId immediately
                latitude = null,
                longitude = null,
                locationCategory = null
            )
            val newExpenseId = repository.insertExpense(localExpense)

            // 2. If the collection is shared, sync it to Firestore IMMEDIATELY
            if (collection?.sharePin != null) {
                val userName = UserPreferences.getUserName(context)
                val userId = Firebase.auth.currentUser?.uid ?: ""
                val sharedExpense = SharedExpense(
                    id = cloudId,
                    amount = amount,
                    timestamp = localExpense.timestamp,
                    adderUserId = userId,
                    adderUserName = userName,
                    locationCategory = null, // Sync with null location for now
                    latitude = null,
                    longitude = null
                )
                firestoreRepository.addExpenseToSharedCollection(collection.sharePin, sharedExpense)
            }

            // 3. Start the background worker to find the location and UPDATE both databases
            startLocationProcessingWorker(newExpenseId, context)

            // 4. Trigger the budget alert check
            val budgetCheckRequest = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
            WorkManager.getInstance(context).enqueue(budgetCheckRequest)
        }
    }

    private suspend fun syncFirebaseExpensesToLocalRoom(pin: String, sharedExpenses: List<SharedExpense>) {
        withContext(Dispatchers.IO) {
            val collection = repository.getCollectionByPin(pin) ?: return@withContext
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(System.currentTimeMillis()))
            val localExpenses = repository.getTransactionsForCollectionInMonthSync(yearMonth, collection.name)
            val localCloudIds = localExpenses.mapNotNull { it.cloudId }.toSet()
            val remoteCloudIds = sharedExpenses.map { it.id }.toSet()

            // 1. Handle additions and modifications from the cloud
            sharedExpenses.forEach { remoteExpense ->
                val localMatch = localExpenses.find { it.cloudId == remoteExpense.id }
                if (localMatch == null) {
                    // ADD: This expense exists in the cloud but not locally. Add it.
                    val newLocalExpense = Expenses(
                        collectionName = collection.name,
                        amount = remoteExpense.amount,
                        timestamp = remoteExpense.timestamp,
                        locationCategory = remoteExpense.locationCategory,
                        cloudId = remoteExpense.id,
                        latitude = remoteExpense.latitude,
                        longitude = remoteExpense.longitude
                    )
                    repository.insertExpense(newLocalExpense)
                } else {
                    // MODIFY: The expense exists locally. Check if it needs updating.
                    if (localMatch.amount != remoteExpense.amount || localMatch.locationCategory != remoteExpense.locationCategory) {
                        val updatedLocalExpense = localMatch.copy(
                            amount = remoteExpense.amount,
                            locationCategory = remoteExpense.locationCategory
                        )
                        repository.updateExpense(updatedLocalExpense)
                    }
                }
            }

            // 2. Handle deletions from the cloud
            val idsToDelete = localCloudIds - remoteCloudIds
            idsToDelete.forEach { cloudId ->
                val expenseToDelete = localExpenses.find { it.cloudId == cloudId }
                if (expenseToDelete != null) {
                    repository.deleteExpense(expenseToDelete)
                }
            }

            // 4. Trigger the budget alert check
            val budgetCheckRequest = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
            WorkManager.getInstance(context).enqueue(budgetCheckRequest)
        }
    }

    // NEW: Function to sync incoming detail changes to the local database
    private suspend fun syncFirebaseDetailsToLocalRoom(sharedCollection: SharedCollection) {
        withContext(Dispatchers.IO) {
            val localCollection = repository.getCollectionByName(sharedCollection.name).first()
            if (localCollection != null) {
                // Check if an update is needed to avoid unnecessary writes
                if (localCollection.budget != sharedCollection.budget ||
                    localCollection.iconName != sharedCollection.iconName ||
                    localCollection.colorHex != sharedCollection.colorHex) {

                    val updatedLocalCollection = localCollection.copy(
                        budget = sharedCollection.budget,
                        iconName = sharedCollection.iconName,
                        colorHex = sharedCollection.colorHex
                    )
                    repository.updateCollection(updatedLocalCollection)
                }
            }
        }
    }

    private fun startLocationProcessingWorker(expenseId: Long, context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workData = workDataOf(LocationProcessingWorker.KEY_EXPENSE_ID to expenseId)
        val workRequest = OneTimeWorkRequestBuilder<LocationProcessingWorker>()
            .setInputData(workData)
            .build()
        workManager.enqueue(workRequest)
    }

    // This function now syncs historical expenses with their location data.
    fun shareCollection(collection: Collections): String? {
        val userName = UserPreferences.getUserName(context)
        val userId = Firebase.auth.currentUser?.uid

        if (userName.isBlank() || userId == null) {
            return null
        }

        val pin = (1..10).map { (0..9).random() }.joinToString("")

        viewModelScope.launch(Dispatchers.IO) {
            val sharedCollection = SharedCollection(
                pin = pin,
                name = collection.name,
                budget = collection.budget,
                iconName = collection.iconName,
                colorHex = collection.colorHex,
                members = listOf(Member(userId = userId, name = userName))
            )
            firestoreRepository.createSharedCollection(sharedCollection)

            val updatedLocalCollection = collection.copy(sharePin = pin)
            repository.updateCollection(updatedLocalCollection)

            val historicalExpenses = repository.getExpensesForCollectionSync(collection.name)
            historicalExpenses.forEach { expense ->
                val cloudId = expense.cloudId ?: UUID.randomUUID().toString()
                repository.updateExpense(expense.copy(cloudId = cloudId))

                val sharedExpense = SharedExpense(
                    id = cloudId,
                    amount = expense.amount,
                    timestamp = expense.timestamp,
                    adderUserId = userId,
                    adderUserName = "Initial Sync",
                    locationCategory = expense.locationCategory,
                    latitude = expense.latitude,    // Add this
                    longitude = expense.longitude // Add this
                )


                firestoreRepository.addExpenseToSharedCollection(pin, sharedExpense)
            }
        }

        // 3. Return the PIN immediately so the UI can display it
        return pin
    }

    // UPDATED: This now syncs detail changes to the cloud
    fun updateCollection(collection: Collections) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the local database
            repository.updateCollection(collection)

            // 2. If the collection is shared, push the changes to Firestore
            if (collection.sharePin != null) {
                firestoreRepository.updateSharedCollectionDetails(collection.sharePin, collection)
            }
        }
    }

    // UPDATED: This now syncs both expenses AND collection details
    private fun startSyncingSharedCollections() {
        viewModelScope.launch {
            repository.allCollections.asFlow().collect { collections ->
                val sharedPins = collections.mapNotNull { it.sharePin }
                sharedPins.forEach { pin ->
                    // Sync expenses (same as before)
                    firestoreRepository.listenToSharedExpenses(pin).collect { sharedExpenses ->
                        syncFirebaseExpensesToLocalRoom(pin, sharedExpenses)
                    }
                    // NEW: Sync collection details
                    firestoreRepository.listenToSharedCollectionDetails(pin).collect { sharedCollection ->
                        if (sharedCollection != null) {
                            syncFirebaseDetailsToLocalRoom(sharedCollection)
                        }
                    }
                }
            }
        }
    }

    // The return value indicates success or failure for the UI
    suspend fun joinCollection(pin: String): Boolean {
        val userName = UserPreferences.getUserName(context)
        val userId = Firebase.auth.currentUser?.uid

        if (userName.isBlank() || userId == null) {
            return false // Can't join without a name and user ID
        }

        // Run the logic on a background thread
        return withContext(Dispatchers.IO) {
            val sharedCollection = firestoreRepository.getSharedCollectionByPin(pin)
            if (sharedCollection != null) {
                // Add the current user to the members list in Firestore
                val newMember = Member(userId = userId, name = userName)
                firestoreRepository.addMemberToCollection(pin, newMember)

                // Create a local copy of this collection
                val localCollection = Collections(
                    name = sharedCollection.name,
                    budget = sharedCollection.budget,
                    iconName = sharedCollection.iconName,
                    colorHex = sharedCollection.colorHex,
                    sharePin = sharedCollection.pin // Link it with the share PIN
                )
                repository.insertCollection(localCollection)
                true // Return true for success
            } else {
                false // Return false if PIN not found
            }
        }
    }

}