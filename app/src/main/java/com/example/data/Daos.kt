package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY startTime ASC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions")
    fun getAllPredictionsFlow(): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsForUserFlow(userId: String): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE userId = :userId AND matchId = :matchId")
    suspend fun getPrediction(userId: String, matchId: Int): PredictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Delete
    suspend fun deletePrediction(prediction: PredictionEntity)
}

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants ORDER BY points DESC, exactHits DESC, trendHits DESC")
    fun getParticipantsFlow(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE userId = :userId")
    suspend fun getParticipantById(userId: String): ParticipantEntity?

    @Query("SELECT * FROM participants WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<ParticipantEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Update
    suspend fun updateParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants")
    suspend fun deleteAllParticipants()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
}
