package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: Int,
    val teamA: String,
    val teamB: String,
    val flagA: String, // Emoji flag
    val flagB: String, // Emoji flag
    val groupName: String,
    val startTime: Long, // Epoch millis
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val status: String = "PENDING" // "PENDING", "COMPLETED"
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey val id: String, // Combined key: "${userId}_${matchId}"
    val userId: String,
    val matchId: Int,
    val scoreA: Int,
    val scoreB: Int,
    val betMode: String = "PUNTAJE_EXACTO", // "PUNTAJE_EXACTO" (Exact score), "DOBLE_OPORTUNIDAD" (Double chance), "SIN_EMPATE" (Draw no bet)
    val doubleChanceTrend: String? = null, // "1X" (Home or Draw), "X2" (Draw or Away), "12" (Home or Away)
    val drawNoBetSelection: String? = null // "1" (Home), "2" (Away)
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val points: Int = 0,
    val exactHits: Int = 0,
    val trendHits: Int = 0,
    val isCurrentUser: Boolean = false,
    val avatarColorOrdinal: Int = 0, // Code to select from pre-defined colors
    val registrationStatus: String = "UNPAID", // "UNPAID", "PENDING_APPROVAL", "APPROVED"
    val paymentMethod: String? = null,
    val transactionRef: String? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String, // Name of the person making transaction
    val type: String, // "BYBIT_USDT", "PAGO_MOVIL"
    val reference: String,
    val amountUnit: String, // "USDT", "VED" (for local currency)
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val screenshotPath: String? = null
)
