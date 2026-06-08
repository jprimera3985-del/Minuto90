package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseSyncService(private val context: Context, private val db: AppDatabase) {

    private var firestore: FirebaseFirestore? = null
    var isInitialized = false
        private set

    init {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
                val appId = try { BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Exception) { "" }
                val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }

                if (apiKey.isNotEmpty() && appId.isNotEmpty() && projectId.isNotEmpty() &&
                    apiKey != "MY_FIREBASE_API_KEY" && appId != "MY_FIREBASE_APP_ID" && projectId != "MY_FIREBASE_PROJECT_ID"
                ) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(context, options)
                } else {
                    FirebaseApp.initializeApp(context)
                }
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                isInitialized = true
                Log.d("FirebaseSyncService", "Firebase Firestore initialized successfully.")
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "Failed to initialize Firebase Firestore: ${e.message}. App will fall back to local database persistence.")
            isInitialized = false
        }
    }

    fun isOnline(): Boolean {
        return isInitialized && firestore != null
    }

    // 1. Upload methods (Pushes local actions to Firestore)
    fun uploadMatch(match: MatchEntity) {
        val fs = firestore ?: return
        val data = hashMapOf(
            "id" to match.id,
            "teamA" to match.teamA,
            "teamB" to match.teamB,
            "flagA" to match.flagA,
            "flagB" to match.flagB,
            "groupName" to match.groupName,
            "startTime" to match.startTime,
            "scoreA" to match.scoreA,
            "scoreB" to match.scoreB,
            "status" to match.status
        )
        fs.collection("matches").document(match.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("FirebaseSync", "Match ${match.id} saved to Firestore.") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to save match ${match.id}", e) }
    }

    fun uploadPrediction(prediction: PredictionEntity) {
        val fs = firestore ?: return
        val data = hashMapOf(
            "id" to prediction.id,
            "userId" to prediction.userId,
            "matchId" to prediction.matchId,
            "scoreA" to prediction.scoreA,
            "scoreB" to prediction.scoreB,
            "betMode" to prediction.betMode,
            "doubleChanceTrend" to prediction.doubleChanceTrend,
            "drawNoBetSelection" to prediction.drawNoBetSelection
        )
        fs.collection("predictions").document(prediction.id)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("FirebaseSync", "Prediction ${prediction.id} saved to Firestore.") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to save prediction ${prediction.id}", e) }
    }

    fun uploadParticipant(participant: ParticipantEntity) {
        val fs = firestore ?: return
        val data = hashMapOf(
            "userId" to participant.userId,
            "name" to participant.name,
            "points" to participant.points,
            "exactHits" to participant.exactHits,
            "trendHits" to participant.trendHits,
            // Keep isCurrentUser local to the client so pulling other participants won't override currentUser status
            "avatarColorOrdinal" to participant.avatarColorOrdinal,
            "registrationStatus" to participant.registrationStatus,
            "paymentMethod" to participant.paymentMethod,
            "transactionRef" to participant.transactionRef
        )
        fs.collection("participants").document(participant.userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("FirebaseSync", "Participant ${participant.userId} saved to Firestore.") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to save participant ${participant.userId}", e) }
    }

    fun uploadTransaction(transaction: TransactionEntity) {
        val fs = firestore ?: return
        val txIdStr = if (transaction.id == 0) "temp_${System.currentTimeMillis()}" else transaction.id.toString()
        val data = hashMapOf(
            "id" to transaction.id,
            "userId" to transaction.userId,
            "name" to transaction.name,
            "type" to transaction.type,
            "reference" to transaction.reference,
            "amountUnit" to transaction.amountUnit,
            "amount" to transaction.amount,
            "timestamp" to transaction.timestamp,
            "status" to transaction.status,
            "screenshotPath" to transaction.screenshotPath
        )
        fs.collection("transactions").document(txIdStr)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("FirebaseSync", "Transaction $txIdStr saved to Firestore.") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to save transaction $txIdStr", e) }
    }

    // 2. Download and Synchronize Database
    suspend fun pullDataFromFirestore(): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            // A. Pull Matches
            val matchesSnapshot = suspendCoroutine { cont ->
                fs.collection("matches").get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val matches = matchesSnapshot.documents.mapNotNull { doc ->
                try {
                    MatchEntity(
                        id = doc.getLong("id")?.toInt() ?: return@mapNotNull null,
                        teamA = doc.getString("teamA") ?: "",
                        teamB = doc.getString("teamB") ?: "",
                        flagA = doc.getString("flagA") ?: "",
                        flagB = doc.getString("flagB") ?: "",
                        groupName = doc.getString("groupName") ?: "",
                        startTime = doc.getLong("startTime") ?: 0L,
                        scoreA = doc.getLong("scoreA")?.toInt(),
                        scoreB = doc.getLong("scoreB")?.toInt(),
                        status = doc.getString("status") ?: "PENDING"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (matches.isNotEmpty()) {
                db.matchDao().insertMatches(matches)
            }

            // B. Pull Participants
            val participantsSnapshot = suspendCoroutine { cont ->
                fs.collection("participants").get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val participants = participantsSnapshot.documents.mapNotNull { doc ->
                try {
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    // Read local ParticipantEntity first to preserve isCurrentUser flag
                    val localPart = db.participantDao().getParticipantById(userId)
                    ParticipantEntity(
                        userId = userId,
                        name = doc.getString("name") ?: "Competidor",
                        points = doc.getLong("points")?.toInt() ?: 0,
                        exactHits = doc.getLong("exactHits")?.toInt() ?: 0,
                        trendHits = doc.getLong("trendHits")?.toInt() ?: 0,
                        isCurrentUser = localPart?.isCurrentUser ?: false,
                        avatarColorOrdinal = doc.getLong("avatarColorOrdinal")?.toInt() ?: 0,
                        registrationStatus = doc.getString("registrationStatus") ?: "UNPAID",
                        paymentMethod = doc.getString("paymentMethod"),
                        transactionRef = doc.getString("transactionRef")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            for (p in participants) {
                db.participantDao().insertParticipant(p)
            }

            // C. Pull Predictions
            val predictionsSnapshot = suspendCoroutine { cont ->
                fs.collection("predictions").get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val predictions = predictionsSnapshot.documents.mapNotNull { doc ->
                try {
                    PredictionEntity(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        userId = doc.getString("userId") ?: "",
                        matchId = doc.getLong("matchId")?.toInt() ?: 0,
                        scoreA = doc.getLong("scoreA")?.toInt() ?: 0,
                        scoreB = doc.getLong("scoreB")?.toInt() ?: 0,
                        betMode = doc.getString("betMode") ?: "PUNTAJE_EXACTO",
                        doubleChanceTrend = doc.getString("doubleChanceTrend"),
                        drawNoBetSelection = doc.getString("drawNoBetSelection")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            for (pred in predictions) {
                db.predictionDao().insertPrediction(pred)
            }

            // D. Pull Transactions
            val transactionsSnapshot = suspendCoroutine { cont ->
                fs.collection("transactions").get()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val transactions = transactionsSnapshot.documents.mapNotNull { doc ->
                try {
                    TransactionEntity(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "PAGO_MOVIL",
                        reference = doc.getString("reference") ?: "",
                        amountUnit = doc.getString("amountUnit") ?: "Bs.",
                        amount = doc.getDouble("amount") ?: 0.0,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        status = doc.getString("status") ?: "PENDING",
                        screenshotPath = doc.getString("screenshotPath")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            for (tx in transactions) {
                db.transactionDao().insertTransaction(tx)
            }

            Log.d("FirebaseSyncService", "Database pull/sync completed successfully from Cloud Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "Error during Firestore data sync: ${e.message}")
            false
        }
    }

    // 3. Complete bidirectional push sync
    suspend fun pushLocalDataToFirestore() = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            // Push matches
            val matches = db.matchDao().getAllMatches().firstOrNull() ?: emptyList()
            matches.forEach { uploadMatch(it) }

            // Push participants
            val participants = db.participantDao().getParticipantsFlow().firstOrNull() ?: emptyList()
            participants.forEach { uploadParticipant(it) }

            // Push predictions
            val predictions = db.predictionDao().getAllPredictionsFlow().firstOrNull() ?: emptyList()
            predictions.forEach { uploadPrediction(it) }

            // Push transactions
            val transactions = db.transactionDao().getTransactionsFlow().firstOrNull() ?: emptyList()
            transactions.forEach { uploadTransaction(it) }

            Log.d("FirebaseSyncService", "Local database pushed successfully to Cloud Firestore.")
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "Error while pushing to Firestore: ${e.message}")
        }
    }
}
