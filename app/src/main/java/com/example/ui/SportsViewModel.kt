package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab {
    FIXTURE,
    LEADERBOARD,
    MY_POTE,
    PROFILE
}

class SportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = SportsRepository(db, application)

    // Authentication State
    private var auth: FirebaseAuth? = null
    val currentUserId = MutableStateFlow<String>("current_user_id_123")
    val authError = MutableStateFlow<String?>(null)
    val isAuthLoading = MutableStateFlow(false)
    val sessionUserEmail = MutableStateFlow<String?>(null)
    val sessionUserName = MutableStateFlow<String?>(null)
    val isUserLoggedIn = MutableStateFlow(false)

    // Sync with Firestore state
    val isSyncing = MutableStateFlow(false)
    val syncSuccessMessage = MutableStateFlow<String?>(null)

    fun syncWithFirestore() {
        viewModelScope.launch(coroutineExceptionHandler) {
            isSyncing.value = true
            syncSuccessMessage.value = null
            try {
                val success = repository.syncService?.pullDataFromFirestore() ?: false
                if (success) {
                    syncSuccessMessage.value = "¡Sincronización en la nube completada!"
                    repository.recalculateAllPoints()
                } else {
                    syncSuccessMessage.value = "Falla al conectar/sincronizar con Firestore (Modo Local)"
                }
            } catch (e: Exception) {
                _globalError.value = "Fallo en sincronización Firestore: ${e.message}"
            } finally {
                isSyncing.value = false
            }
        }
    }

    // Current screen orientation / Tab state
    private val _currentTab = MutableStateFlow(AppTab.FIXTURE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Global Error Handling State (ErrorBoundary data source)
    private val _globalError = MutableStateFlow<String?>(null)
    val globalError: StateFlow<String?> = _globalError.asStateFlow()

    // Exception handler for coroutines
    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        _globalError.value = "Fallo de Corrutina Inesperado: ${throwable.localizedMessage ?: "Causa desconocida"}"
    }

    fun triggerSimulatedError(errorMessage: String) {
        _globalError.value = errorMessage
    }

    fun clearGlobalError() {
        _globalError.value = null
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.recalculateAllPoints()
            } catch (e: Exception) {
                _globalError.value = "Fallo de API / Base de datos al recargar: ${e.message}"
            }
        }
    }

    // Database Streams
    val matches = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val participants = repository.allParticipants.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentUser = repository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val transactions = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val predictions = repository.allPredictions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active betting edit sheet/dialog state
    private val _selectedMatchForBet = MutableStateFlow<MatchEntity?>(null)
    val selectedMatchForBet: StateFlow<MatchEntity?> = _selectedMatchForBet.asStateFlow()

    val betScoreA = MutableStateFlow(0)
    val betScoreB = MutableStateFlow(0)
    val betMode = MutableStateFlow("PUNTAJE_EXACTO") // "PUNTAJE_EXACTO", "DOBLE_OPORTUNIDAD", "SIN_EMPATE"
    val doubleChanceTrend = MutableStateFlow("1X") // "1X", "X2", "12"
    val drawNoBetSelection = MutableStateFlow("1") // "1", "2"

    // UI Input fields for Payment submissions
    val mobilePaymentPhone = MutableStateFlow("")
    val mobilePaymentCI = MutableStateFlow("")
    val mobilePaymentBank = MutableStateFlow("Banco Venezolano de Crédito (0104)")
    val mobilePaymentReference = MutableStateFlow("")
    val mobilePaymentScreenshot = MutableStateFlow<String?>(null)

    // Simulated Admin Panel toggle (so developers/users can test the admin approval & points calculation flow)
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // Score update editing for Admin
    val adminUpdatingMatch = MutableStateFlow<MatchEntity?>(null)
    val adminScoreA = MutableStateFlow("0")
    val adminScoreB = MutableStateFlow("0")

    init {
        try {
            if (com.google.firebase.FirebaseApp.getApps(application).isNotEmpty()) {
                auth = FirebaseAuth.getInstance()
                val user = auth?.currentUser
                if (user != null) {
                    currentUserId.value = user.uid
                    sessionUserEmail.value = user.email
                    sessionUserName.value = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                    isUserLoggedIn.value = true
                }
            }
        } catch (e: Exception) {
            Log.e("SportsViewModel", "FirebaseAuth initialization failed: ${e.message}")
        }

        viewModelScope.launch {
            repository.prepopulateData()
            if (isUserLoggedIn.value && auth?.currentUser != null) {
                val uid = currentUserId.value
                val name = sessionUserName.value ?: "Usuario"
                repository.setCurrentUser(uid, name)
                if (repository.syncService?.isOnline() == true) {
                    syncWithFirestore()
                }
            } else {
                if (repository.syncService?.isOnline() == true) {
                    syncWithFirestore()
                }
            }
        }
    }

    fun signUpWithEmailAndPassword(email: String, pss: String, fullName: String, onSuccess: () -> Unit) {
        val authInstance = auth
        if (authInstance == null) {
            authError.value = "Firebase no disponible en este dispositivo/configuración."
            return
        }
        if (email.isBlank() || pss.isBlank() || fullName.isBlank()) {
            authError.value = "Todos los campos son obligatorios."
            return
        }
        isAuthLoading.value = true
        authError.value = null
        
        authInstance.createUserWithEmailAndPassword(email.trim(), pss)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                viewModelScope.launch(coroutineExceptionHandler) {
                                    try {
                                        currentUserId.value = firebaseUser.uid
                                        sessionUserEmail.value = firebaseUser.email
                                        sessionUserName.value = fullName
                                        isUserLoggedIn.value = true
                                        isAuthLoading.value = false
                                        
                                        repository.setCurrentUser(firebaseUser.uid, fullName)
                                        if (repository.syncService?.isOnline() == true) {
                                            syncWithFirestore()
                                        }
                                        onSuccess()
                                    } catch (e: Exception) {
                                        authError.value = "Error al iniciar sesión local: ${e.message}"
                                        isAuthLoading.value = false
                                    }
                                }
                            }
                    } else {
                        isAuthLoading.value = false
                        authError.value = "No se pudo crear el usuario."
                    }
                } else {
                    isAuthLoading.value = false
                    authError.value = task.exception?.localizedMessage ?: "Error al registrarse."
                }
            }
    }

    fun signInWithEmailAndPassword(email: String, pss: String, onSuccess: () -> Unit) {
        val authInstance = auth
        if (authInstance == null) {
            authError.value = "Firebase no disponible en este dispositivo. Por favor, use el botón de Modo Demo (Offline)."
            return
        }
        if (email.isBlank() || pss.isBlank()) {
            authError.value = "Correo y contraseña requeridos."
            return
        }
        isAuthLoading.value = true
        authError.value = null
        
        authInstance.signInWithEmailAndPassword(email.trim(), pss)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Usuario"
                        viewModelScope.launch(coroutineExceptionHandler) {
                            try {
                                currentUserId.value = firebaseUser.uid
                                sessionUserEmail.value = firebaseUser.email
                                sessionUserName.value = name
                                isUserLoggedIn.value = true
                                isAuthLoading.value = false
                                
                                repository.setCurrentUser(firebaseUser.uid, name)
                                if (repository.syncService?.isOnline() == true) {
                                    syncWithFirestore()
                                }
                                onSuccess()
                            } catch (e: Exception) {
                                authError.value = "Error al iniciar sesión local: ${e.message}"
                                isAuthLoading.value = false
                            }
                        }
                    } else {
                        isAuthLoading.value = false
                        authError.value = "No se pudo obtener el usuario."
                    }
                } else {
                    isAuthLoading.value = false
                    authError.value = task.exception?.localizedMessage ?: "Credenciales inválidas"
                }
            }
    }

    fun continueAsGuest() {
        isAuthLoading.value = true
        authError.value = null
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                currentUserId.value = "current_user_id_123"
                sessionUserEmail.value = "demo@minuto90.com"
                sessionUserName.value = "Jugador de Pruebas (Local)"
                isUserLoggedIn.value = true
                isAuthLoading.value = false
                
                repository.setCurrentUser("current_user_id_123", "Tú (Usuario)")
            } catch (e: Exception) {
                authError.value = "Error ingresando localmente: ${e.message}"
                isAuthLoading.value = false
            }
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("SportsViewModel", "SignOut error: ${e.message}")
        }
        currentUserId.value = "current_user_id_123"
        sessionUserEmail.value = null
        sessionUserName.value = null
        isUserLoggedIn.value = false
        
        viewModelScope.launch(coroutineExceptionHandler) {
            repository.setCurrentUser("current_user_id_123", "Tú (Usuario)")
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    // Opens placing/editing prediction
    fun openBetSheet(match: MatchEntity) {
        viewModelScope.launch {
            _selectedMatchForBet.value = match
            val existing = predictions.value.find { it.matchId == match.id && it.userId == currentUserId.value }
            if (existing != null) {
                betScoreA.value = existing.scoreA
                betScoreB.value = existing.scoreB
                betMode.value = existing.betMode
                doubleChanceTrend.value = existing.doubleChanceTrend ?: "1X"
                drawNoBetSelection.value = existing.drawNoBetSelection ?: "1"
            } else {
                betScoreA.value = 0
                betScoreB.value = 0
                betMode.value = "PUNTAJE_EXACTO"
                doubleChanceTrend.value = "1X"
                drawNoBetSelection.value = "1"
            }
        }
    }

    fun closeBetSheet() {
        _selectedMatchForBet.value = null
    }

    // Saves prediction
    fun savePrediction() {
        val match = _selectedMatchForBet.value ?: return
        
        // Anti-Cheat Check: Cannot save if current time is within 15 minutes of match start!
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesInMillis = 15 * 60 * 1000L
        if (currentTime >= (match.startTime - fifteenMinutesInMillis)) {
            // Immutable!
            return
        }

        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                val pred = PredictionEntity(
                    id = "${currentUserId.value}_${match.id}",
                    userId = currentUserId.value,
                    matchId = match.id,
                    scoreA = betScoreA.value,
                    scoreB = betScoreB.value,
                    betMode = betMode.value,
                    doubleChanceTrend = if (betMode.value == "DOBLE_OPORTUNIDAD") doubleChanceTrend.value else null,
                    drawNoBetSelection = if (betMode.value == "SIN_EMPATE") drawNoBetSelection.value else null
                )
                repository.savePrediction(pred)
                repository.recalculateAllPoints()
                closeBetSheet()
            } catch (e: Exception) {
                _globalError.value = "Fallo de Base de datos al guardar predicción: ${e.message}"
            }
        }
    }

    // Submit Payment via Pago Móvil (Venezuela)
    fun submitPagoMovilPayment() {
        if (mobilePaymentPhone.value.isBlank() || mobilePaymentCI.value.isBlank() || mobilePaymentReference.value.isBlank()) return
        val screenshot = mobilePaymentScreenshot.value ?: "pago_movil_bancamiga_exitoso.png"
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                val tx = TransactionEntity(
                    userId = currentUserId.value,
                    name = sessionUserName.value ?: "Tú (Usuario)",
                    type = "PAGO_MOVIL",
                    reference = "Ref: ${mobilePaymentReference.value} (${mobilePaymentBank.value}) - CI: ${mobilePaymentCI.value}",
                    amountUnit = "Bs.",
                    amount = 450.0, // Base calculation in local VED equivalent of 10 USDT
                    status = "PENDING",
                    screenshotPath = screenshot
                )
                repository.submitTransaction(tx)
                mobilePaymentPhone.value = ""
                mobilePaymentCI.value = ""
                mobilePaymentReference.value = ""
                mobilePaymentScreenshot.value = null // reset after successful registration
            } catch (e: Exception) {
                _globalError.value = "Fallo de API al procesar el pago: ${e.message}"
            }
        }
    }

    // ADMIN ACTIONS
    fun adminApproveTransaction(txId: Int) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.approveTransaction(txId, currentUserId.value)
            } catch (e: Exception) {
                _globalError.value = "Error al aprobar transacción: ${e.message}"
            }
        }
    }

    fun adminRejectTransaction(txId: Int) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.rejectTransaction(txId)
            } catch (e: Exception) {
                _globalError.value = "Error al rechazar transacción: ${e.message}"
            }
        }
    }

    fun openAdminMatchScoreEditor(match: MatchEntity) {
        adminUpdatingMatch.value = match
        adminScoreA.value = (match.scoreA ?: 0).toString()
        adminScoreB.value = (match.scoreB ?: 0).toString()
    }

    fun closeAdminMatchScoreEditor() {
        adminUpdatingMatch.value = null
    }

    fun saveAdminMatchScore() {
        val match = adminUpdatingMatch.value ?: return
        val scoreAVal = adminScoreA.value.toIntOrNull() ?: 0
        val scoreBVal = adminScoreB.value.toIntOrNull() ?: 0
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.updateMatchResult(match.id, scoreAVal, scoreBVal)
                closeAdminMatchScoreEditor()
            } catch (e: Exception) {
                _globalError.value = "Error al actualizar marcador: ${e.message}"
            }
        }
    }

    fun resetMatchResult(matchId: Int) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.resetMatchResult(matchId)
            } catch (e: Exception) {
                _globalError.value = "Error al reiniciar marcador: ${e.message}"
            }
        }
    }

    fun resetWholeDatabase() {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                db.clearAllTables()
                repository.prepopulateData()
            } catch (e: Exception) {
                _globalError.value = "Error crítico de Base de datos: ${e.message}"
            }
        }
    }
}
