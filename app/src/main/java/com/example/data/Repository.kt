package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class SportsRepository(private val db: AppDatabase, context: Context? = null) {

    private val matchDao = db.matchDao()
    private val predictionDao = db.predictionDao()
    private val participantDao = db.participantDao()
    private val transactionDao = db.transactionDao()

    val syncService = context?.let { FirebaseSyncService(it, db) }

    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()
    val allParticipants: Flow<List<ParticipantEntity>> = participantDao.getParticipantsFlow()
    val currentUser: Flow<ParticipantEntity?> = participantDao.getCurrentUserFlow()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getTransactionsFlow()
    val allPredictions: Flow<List<PredictionEntity>> = predictionDao.getAllPredictionsFlow()

    fun getPredictionsForUser(userId: String): Flow<List<PredictionEntity>> {
        return predictionDao.getPredictionsForUserFlow(userId)
    }

    suspend fun savePrediction(prediction: PredictionEntity) {
        predictionDao.insertPrediction(prediction)
        syncService?.uploadPrediction(prediction)
    }

    suspend fun submitTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        syncService?.uploadTransaction(transaction)
        
        // When user submits transaction, update registration status to PENDING_APPROVAL
        val user = participantDao.getParticipantById(transaction.userId)
        if (user != null) {
            val updatedUser = user.copy(
                registrationStatus = "PENDING_APPROVAL",
                paymentMethod = transaction.type,
                transactionRef = transaction.reference
            )
            participantDao.updateParticipant(updatedUser)
            syncService?.uploadParticipant(updatedUser)
        }
    }

    suspend fun approveTransaction(transactionId: Int, adminActionUser: String) {
        val txs = transactionDao.getTransactionsFlow().firstOrNull() ?: emptyList()
        val tx = txs.find { it.id == transactionId } ?: return
        
        // 1. Update transaction status to APPROVED
        val updatedTx = tx.copy(status = "APPROVED")
        transactionDao.insertTransaction(updatedTx)
        syncService?.uploadTransaction(updatedTx)
        
        // 2. Update participant registrationStatus to APPROVED
        val user = participantDao.getParticipantById(tx.userId)
        if (user != null) {
            val updatedUser = user.copy(registrationStatus = "APPROVED")
            participantDao.updateParticipant(updatedUser)
            syncService?.uploadParticipant(updatedUser)
        }
    }

    suspend fun rejectTransaction(transactionId: Int) {
        val txs = transactionDao.getTransactionsFlow().firstOrNull() ?: emptyList()
        val tx = txs.find { it.id == transactionId } ?: return
        
        // 1. Update transaction status to REJECTED
        val updatedTx = tx.copy(status = "REJECTED")
        transactionDao.insertTransaction(updatedTx)
        syncService?.uploadTransaction(updatedTx)
        
        // 2. Clear pending status for participant
        val user = participantDao.getParticipantById(tx.userId)
        if (user != null) {
            val updatedUser = user.copy(registrationStatus = "UNPAID")
            participantDao.updateParticipant(updatedUser)
            syncService?.uploadParticipant(updatedUser)
        }
    }

    suspend fun updateMatchResult(matchId: Int, scoreA: Int, scoreB: Int) {
        val match = matchDao.getMatchById(matchId) ?: return
        
        // Update match status to COMPLETED and save scores
        val updatedMatch = match.copy(
            scoreA = scoreA,
            scoreB = scoreB,
            status = "COMPLETED"
        )
        matchDao.updateMatch(updatedMatch)
        syncService?.uploadMatch(updatedMatch)

        // Recalculate scores for all participants based on all COMPLETED matches
        recalculateAllPoints()
    }

    suspend fun resetMatchResult(matchId: Int) {
        val match = matchDao.getMatchById(matchId) ?: return
        val updatedMatch = match.copy(
            scoreA = null,
            scoreB = null,
            status = "PENDING"
        )
        matchDao.updateMatch(updatedMatch)
        syncService?.uploadMatch(updatedMatch)
        recalculateAllPoints()
    }

    suspend fun recalculateAllPoints() {
        val allCompletedMatches = matchDao.getAllMatches().firstOrNull()?.filter { it.status == "COMPLETED" } ?: emptyList()
        val predictions = predictionDao.getAllPredictionsFlow().firstOrNull() ?: emptyList()
        val participants = participantDao.getParticipantsFlow().firstOrNull() ?: emptyList()

        for (participant in participants) {
            var totalPoints = 0
            var exactCount = 0
            var trendCount = 0

            val userPredictions = predictions.filter { it.userId == participant.userId }
            
            for (match in allCompletedMatches) {
                val pred = userPredictions.find { it.matchId == match.id } ?: continue
                val realScoreA = match.scoreA ?: continue
                val realScoreB = match.scoreB ?: continue

                val realHomeWin = realScoreA > realScoreB
                val realAwayWin = realScoreA < realScoreB
                val realDraw = realScoreA == realScoreB

                when (pred.betMode) {
                    "PUNTAJE_EXACTO" -> {
                        val predictedHomeWin = pred.scoreA > pred.scoreB
                        val predictedAwayWin = pred.scoreA < pred.scoreB
                        val predictedDraw = pred.scoreA == pred.scoreB

                        if (pred.scoreA == realScoreA && pred.scoreB == realScoreB) {
                            totalPoints += 5
                            exactCount++
                        } else if ((predictedHomeWin && realHomeWin) || (predictedAwayWin && realAwayWin) || (predictedDraw && realDraw)) {
                            totalPoints += 2
                            trendCount++
                        }
                    }
                    "DOBLE_OPORTUNIDAD" -> {
                        val isHit = when (pred.doubleChanceTrend) {
                            "1X" -> realHomeWin || realDraw
                            "X2" -> realAwayWin || realDraw
                            "12" -> realHomeWin || realAwayWin
                            else -> false
                        }
                        if (isHit) {
                            totalPoints += 3
                            trendCount++
                        }
                    }
                    "SIN_EMPATE" -> {
                        if (realDraw) {
                            totalPoints += 1
                        } else {
                            val selectedIsWinner = when (pred.drawNoBetSelection) {
                                "1" -> realHomeWin
                                "2" -> realAwayWin
                                else -> false
                            }
                            if (selectedIsWinner) {
                                totalPoints += 4
                                exactCount++ // count as high value hit
                            }
                        }
                    }
                }
            }

            val updatedParticipant = participant.copy(
                points = totalPoints,
                exactHits = exactCount,
                trendHits = trendCount
            )
            participantDao.updateParticipant(updatedParticipant)
            syncService?.uploadParticipant(updatedParticipant)
        }
    }

    suspend fun setCurrentUser(userId: String, name: String, registrationStatus: String = "UNPAID") {
        val participants = participantDao.getParticipantsFlow().firstOrNull() ?: emptyList()
        for (p in participants) {
            if (p.isCurrentUser && p.userId != userId) {
                participantDao.updateParticipant(p.copy(isCurrentUser = false))
            }
        }
        
        val existing = participantDao.getParticipantById(userId)
        val participant = if (existing != null) {
            existing.copy(name = name, isCurrentUser = true)
        } else {
            ParticipantEntity(
                userId = userId,
                name = name,
                isCurrentUser = true,
                avatarColorOrdinal = (name.length % 5),
                registrationStatus = registrationStatus
            )
        }
        participantDao.insertParticipant(participant)
        syncService?.uploadParticipant(participant)
        recalculateAllPoints()
    }

    suspend fun prepopulateData() {
        val existingMatches = matchDao.getAllMatches().firstOrNull()
        if (existingMatches != null && existingMatches.size >= 20) return

        if (!existingMatches.isNullOrEmpty() && existingMatches.size < 20) {
            matchDao.deleteAllMatches()
        }

        // 1. Initial Matches (Mix of future, near-future, live, and past)
        val systemTime = System.currentTimeMillis()
        val oneHour = 3600000L
        val oneDay = 86400000L

        val initialMatches = listOf(
            // MATCHES IN THE PAST (completed to showcase points calculation right away!)
            MatchEntity(
                id = 1,
                teamA = "México", teamB = "Australia",
                flagA = "🇲🇽", flagB = "🇦🇺",
                groupName = "Grupo A • Amistoso",
                startTime = systemTime - (2 * oneDay),
                scoreA = 2, scoreB = 0,
                status = "COMPLETED"
            ),
            MatchEntity(
                id = 2,
                teamA = "Canadá", teamB = "Marruecos",
                flagA = "🇨🇦", flagB = "🇲🇦",
                groupName = "Grupo B • Amistoso",
                startTime = systemTime - oneDay,
                scoreA = 1, scoreB = 1,
                status = "COMPLETED"
            ),
            // MATCHES IN PROGRESS OR IN IMMINENT LOCKOUT (Starts in 10 minutes - locked!)
            MatchEntity(
                id = 3,
                teamA = "Estados Unidos", teamB = "Italia",
                flagA = "🇺🇸", flagB = "🇮🇹",
                groupName = "Grupo C • Preparación",
                startTime = systemTime + (10 * 60000L), // Starts in +10 minutes (Locked!)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            // FUTURE MATCHES AVAILABLE FOR BETTING
            MatchEntity(
                id = 4,
                teamA = "Argentina", teamB = "Croacia",
                flagA = "🇦🇷", flagB = "🇭🇷",
                groupName = "Grupo F • Amistoso",
                startTime = systemTime + (3 * oneHour), // Starts in 3 hours
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 5,
                teamA = "España", teamB = "Japón",
                flagA = "🇪🇸", flagB = "🇯🇵",
                groupName = "Grupo H • Amistoso",
                startTime = systemTime + oneDay, // Starts in 1 day
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 6,
                teamA = "Brasil", teamB = "Camerún",
                flagA = "🇧🇷", flagB = "🇨🇲",
                groupName = "Grupo J • Amistoso",
                startTime = systemTime + (2 * oneDay), // Starts in 2 days
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 7,
                teamA = "Francia", teamB = "Colombia",
                flagA = "🇫🇷", flagB = "🇨🇴",
                groupName = "Grupo L • Preparación",
                startTime = systemTime + (3 * oneDay), // Starts in 3 days
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 8,
                teamA = "Inglaterra", teamB = "Uruguay",
                flagA = "🇬🇧", flagB = "🇺🇾",
                groupName = "Grupo E • Preparación",
                startTime = systemTime + (4 * oneDay), // Starts in 4 days
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            // OFFICIAL 2026 INAUGURAL CHRONOLOGY (FIFA World Cup 2026 Opening Fixtures)
            MatchEntity(
                id = 9,
                teamA = "México", teamB = "Por clasificar (A2)",
                flagA = "🇲🇽", flagB = "🌍",
                groupName = "M1: Grupo A • Estadio Azteca 🏟️ (CDMX)",
                startTime = systemTime + (5 * oneDay), // (Thursday June 11, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 10,
                teamA = "Por clasificar (A3)", teamB = "Por clasificar (A4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M2: Grupo A • Estadio Akron 🏟️ (Guadalajara)",
                startTime = systemTime + (5 * oneDay) + (4 * oneHour), // (Thursday June 11, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 11,
                teamA = "Canadá", teamB = "Por clasificar (B2)",
                flagA = "🇨🇦", flagB = "🌍",
                groupName = "M3: Grupo B • BMO Field 🏟️ (Toronto)",
                startTime = systemTime + (6 * oneDay), // (Friday June 12, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 12,
                teamA = "Por clasificar (B3)", teamB = "Por clasificar (B4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M4: Grupo B • Gillette Stadium 🏟️ (Boston)",
                startTime = systemTime + (6 * oneDay) + (2 * oneHour), // (Friday June 12, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 13,
                teamA = "Estados Unidos", teamB = "Por clasificar (C2)",
                flagA = "🇺🇸", flagB = "🌍",
                groupName = "M5: Grupo C • SoFi Stadium 🏟️ (Los Ángeles)",
                startTime = systemTime + (6 * oneDay) + (5 * oneHour), // (Friday June 12, 2026 - US Opening Game)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 14,
                teamA = "Por clasificar (C3)", teamB = "Por clasificar (C4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M6: Grupo C • Levi's Stadium 🏟️ (San Francisco)",
                startTime = systemTime + (6 * oneDay) + (7 * oneHour), // (Friday June 12, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 15,
                teamA = "Por clasificar (D1)", teamB = "Por clasificar (D2)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M7: Grupo D • BC Place 🏟️ (Vancouver)",
                startTime = systemTime + (7 * oneDay), // (Saturday June 13, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 16,
                teamA = "Por clasificar (D3)", teamB = "Por clasificar (D4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M8: Grupo D • Lumen Field 🏟️ (Seattle)",
                startTime = systemTime + (7 * oneDay) + (2 * oneHour), // (Saturday June 13, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 17,
                teamA = "Por clasificar (E1)", teamB = "Por clasificar (E2)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M9: Grupo E • AT&T Stadium 🏟️ (Dallas)",
                startTime = systemTime + (7 * oneDay) + (4 * oneHour), // (Saturday June 13, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 18,
                teamA = "Por clasificar (E3)", teamB = "Por clasificar (E4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M10: Grupo E • NRG Stadium 🏟️ (Houston)",
                startTime = systemTime + (7 * oneDay) + (6 * oneHour), // (Saturday June 13, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 19,
                teamA = "Por clasificar (F1)", teamB = "Por clasificar (F2)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M11: Grupo F • Arrowhead Stadium 🏟️ (Kansas City)",
                startTime = systemTime + (7 * oneDay) + (8 * oneHour), // (Saturday June 13, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            ),
            MatchEntity(
                id = 20,
                teamA = "Por clasificar (F3)", teamB = "Por clasificar (F4)",
                flagA = "🌍", flagB = "🌍",
                groupName = "M12: Grupo F • Mercedes-Benz Stadium 🏟️ (Atlanta)",
                startTime = systemTime + (8 * oneDay), // (Sunday June 14, 2026)
                scoreA = null, scoreB = null,
                status = "PENDING"
            )
        )
        matchDao.insertMatches(initialMatches)

        // 2. Pre-populate Participants (including current user and simulated opponents)
        val user1 = ParticipantEntity(
            userId = "current_user_id_123",
            name = "Tú (Usuario)",
            isCurrentUser = true,
            avatarColorOrdinal = 0,
            registrationStatus = "UNPAID" // The user starts unpaid to showcase payment system!
        )
        val user2 = ParticipantEntity(
            userId = "opp_1",
            name = "Carlos Mendoza",
            points = 7,
            exactHits = 1,
            trendHits = 1,
            avatarColorOrdinal = 1,
            registrationStatus = "APPROVED"
        )
        val user3 = ParticipantEntity(
            userId = "opp_2",
            name = "Sofía Rodríguez",
            points = 5,
            exactHits = 1,
            trendHits = 0,
            avatarColorOrdinal = 2,
            registrationStatus = "APPROVED"
        )
        val user4 = ParticipantEntity(
            userId = "opp_3",
            name = "Miguel Silva",
            points = 2,
            exactHits = 0,
            trendHits = 1,
            avatarColorOrdinal = 3,
            registrationStatus = "APPROVED"
        )
        
        participantDao.insertParticipant(user1)
        participantDao.insertParticipant(user2)
        participantDao.insertParticipant(user3)
        participantDao.insertParticipant(user4)

        // 3. Pre-populate predictions for simulated opponents so they have scores
        predictionDao.insertPrediction(PredictionEntity("opp_1_1", "opp_1", 1, 2, 0, "PUNTAJE_EXACTO")) // hit 5
        predictionDao.insertPrediction(PredictionEntity("opp_1_3", "opp_1", 3, 3, 1, "PUNTAJE_EXACTO")) // future
        predictionDao.insertPrediction(PredictionEntity("opp_1_2", "opp_1", 2, 0, 0, "DOBLE_OPORTUNIDAD", "1X")) // draw, combo 1X contains Draw -> hit! +3 points. Total = 8

        // Opponent 2 (Sofía): USA (2-0, exact) matches!
        predictionDao.insertPrediction(PredictionEntity("opp_2_1", "opp_2", 1, 2, 0, "PUNTAJE_EXACTO")) // hit 5 points
        
        // Opponent 3 (Miguel): USA (1-0, trend) -> hit 2 points
        predictionDao.insertPrediction(PredictionEntity("opp_3_1", "opp_3", 1, 1, 0, "PUNTAJE_EXACTO")) // hit 2 points

        // Pre-populate some baseline predictions for currently logged in user for completed games
        predictionDao.insertPrediction(PredictionEntity("current_user_id_123_1", "current_user_id_123", 1, 2, 0, "PUNTAJE_EXACTO")) // user got exact hit on game 1! +5pts
        predictionDao.insertPrediction(PredictionEntity("current_user_id_123_2", "current_user_id_123", 2, 1, 1, "PUNTAJE_EXACTO")) // user predicted 1-1 for game 2! +5pts
        
        recalculateAllPoints()
        
        // Push newly prepopulated baseline to Firestore securely
        syncService?.pushLocalDataToFirestore()
    }
}
