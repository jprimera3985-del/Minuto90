package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import com.example.R
import java.text.SimpleDateFormat
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: SportsViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val selectedMatchForBet by viewModel.selectedMatchForBet.collectAsState()
    val globalError by viewModel.globalError.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    if (globalError != null) {
        ErrorBoundaryScreen(
            errorMessage = globalError ?: "Fallo de conexión o consulta de base de datos",
            onReload = { viewModel.clearGlobalError() }
        )
    } else if (!isUserLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MINUTO90",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AmberOrange,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Donde cada segundo y cada gol cuentan",
                            fontSize = 10.sp,
                            color = OffWhite.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Modo Admin",
                            fontSize = 11.sp,
                            color = if (isAdminMode) EmeraldGreen else SlateGray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = isAdminMode,
                            onCheckedChange = { viewModel.toggleAdminMode() },
                            modifier = Modifier.testTag("admin_toggle_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldGreen,
                                checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = SlateGray,
                                uncheckedTrackColor = SlateGray.copy(alpha = 0.2f)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSlateBlue,
                    titleContentColor = OffWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepSlateBlue,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.FIXTURE,
                    onClick = { viewModel.selectTab(AppTab.FIXTURE) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Fixture") },
                    label = { Text("Fixture") },
                    modifier = Modifier.testTag("tab_fixture")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.LEADERBOARD,
                    onClick = { viewModel.selectTab(AppTab.LEADERBOARD) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Tabla") },
                    label = { Text("Tabla") },
                    modifier = Modifier.testTag("tab_leaderboard")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.MY_POTE,
                    onClick = { viewModel.selectTab(AppTab.MY_POTE) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pote") },
                    label = { Text("Mi Pote") },
                    modifier = Modifier.testTag("tab_pote")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.PROFILE,
                    onClick = { viewModel.selectTab(AppTab.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        },
        containerColor = DeepSlateBlue
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isAdminMode) {
                    var showMetricsPanel by remember { mutableStateOf(true) }
                    val participants by viewModel.participants.collectAsState()
                    val transactions by viewModel.transactions.collectAsState()
                    val predictions by viewModel.predictions.collectAsState()
                    val matches by viewModel.matches.collectAsState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlateBlue)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmeraldGreen.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showMetricsPanel = !showMetricsPanel }
                                        .testTag("admin_header_toggle_metrics")
                                ) {
                                    Text(
                                        "🔧 Panel Admin de Liga ",
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Metrics",
                                        tint = EmeraldGreen,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(if (showMetricsPanel) 180f else 0f)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showMetricsPanel = !showMetricsPanel },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("toggle_metrics_button")
                                    ) {
                                        Text(
                                            text = if (showMetricsPanel) "Ocultar" else "Ver Métricas 📈",
                                            fontSize = 10.sp,
                                            color = OffWhite
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.resetWholeDatabase() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("reset_db_button")
                                    ) {
                                        Text("Reiniciar BD", fontSize = 10.sp, color = AmberOrange)
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showMetricsPanel,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            // Calculate metrics
                            val totalUsers = participants.size
                            val approvedUsers = participants.count { it.registrationStatus == "APPROVED" }
                            val pendingUsers = participants.count { it.registrationStatus == "PENDING_APPROVAL" }

                            val totalUsdtPool = approvedUsers * 10.0
                            val totalBsApproved = transactions.filter { it.status == "APPROVED" && it.amountUnit == "Bs." }.sumOf { it.amount }

                            val totalPredictionsCount = predictions.size
                            val completedMatchIds = matches.filter { it.status == "COMPLETED" || it.scoreA != null }.map { it.id }.toSet()
                            val processedPredictionsCount = predictions.count { it.matchId in completedMatchIds }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepSlateBlue)
                                    .border(BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f)))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. STAT: Registered Users
                                    AdminStatCard(
                                        title = "Usuarios",
                                        value = "$totalUsers",
                                        subtitle = "$approvedUsers Act / $pendingUsers Pend",
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // 2. STAT: Pot Recaudado
                                    AdminStatCard(
                                        title = "Pote Recaudado",
                                        value = "${totalUsdtPool.toInt()} USDT",
                                        subtitle = "${totalBsApproved.toInt()} Bs. Validados",
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // 3. STAT: Processed Predictions
                                    AdminStatCard(
                                        title = "Pronósticos",
                                        value = "$processedPredictionsCount",
                                        subtitle = "de $totalPredictionsCount realizados",
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 1.dp,
                                    color = OffWhite.copy(alpha = 0.08f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "🚨 PRUEBAS DE MANEJO DE ERRORES (GLOBAL ERROR BOUNDARY)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOrange,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.triggerSimulatedError("Falla de Conexión HTTP 504: El servidor de estadísticas del Mundial no respondió a tiempo.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("sim_api_error_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Falla API 🌐", fontSize = 9.sp, color = OffWhite)
                                    }
                                    Button(
                                        onClick = { viewModel.triggerSimulatedError("Error de integridad SQLite: Foreign key mismatch al registrar la predicción de 'current_user_id_123' en partidos bloqueados.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("sim_db_error_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Falla BD 🗄️", fontSize = 9.sp, color = OffWhite)
                                    }
                                    Button(
                                        onClick = { viewModel.triggerSimulatedError("Fallo de renderizado táctico: Jetpack Compose Navigation controller crash al iniciar el estado 'AppTab.PROFILE'.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("sim_nav_error_btn"),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Falla Naveg. 🚀", fontSize = 9.sp, color = OffWhite)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "ESTADÍSTICAS DEL TORNEO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        letterSpacing = 1.sp
                                    )
                                }

                                val completedMatches = matches.filter { it.status == "COMPLETED" || (it.scoreA != null && it.scoreB != null) }
                                val homeWins = completedMatches.count { (it.scoreA ?: 0) > (it.scoreB ?: 0) }
                                val draws = completedMatches.count { (it.scoreA ?: 0) == (it.scoreB ?: 0) }
                                val awayWins = completedMatches.count { (it.scoreA ?: 0) < (it.scoreB ?: 0) }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ResultsDistributionChart(homeWins = homeWins, draws = draws, awayWins = awayWins)
                                    ParticipationTrendChart(matches = matches, predictions = predictions)
                                }
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val targetIndex = targetState.ordinal
                        val initialStateIndex = initialState.ordinal
                        if (targetIndex > initialStateIndex) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn(animationSpec = tween(220))).togetherWith(
                                slideOutHorizontally { width -> -width / 2 } + fadeOut(animationSpec = tween(220))
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn(animationSpec = tween(220))).togetherWith(
                                slideOutHorizontally { width -> width / 2 } + fadeOut(animationSpec = tween(220))
                            )
                        }
                    },
                    label = "tab_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.FIXTURE -> FixtureScreen(viewModel = viewModel)
                        AppTab.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel)
                        AppTab.MY_POTE -> PoteScreen(viewModel = viewModel)
                        AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }

            // Prediction Dialog
            selectedMatchForBet?.let { match ->
                PredictionDialog(match = match, viewModel = viewModel)
            }
        }
    }
    }
}

@Composable
fun FixtureScreen(viewModel: SportsViewModel) {
    val matches by viewModel.matches.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val adminUpdatingMatch by viewModel.adminUpdatingMatch.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("world_cup_hero_banner"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                ) {
                    // 1. Hero Image
                    Image(
                        painter = painterResource(id = R.drawable.img_world_cup_banner),
                        contentDescription = "Mundial 2026 Banner",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 2. Translucent Gradient Scrim for accessibility
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepSlateBlue.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    // 3. Texts and Accent badge
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(AmberOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberOrange,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                "MUNDIAL 2026",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = AmberOrange,
                                letterSpacing = 1.2.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            "Predicciones del Mundial 2026",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = OffWhite
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            "Ingresa tus marcadores para los 104 partidos. Los cambios cierran de forma automática 15 minutos antes de cada encuentro.",
                            fontSize = 10.sp,
                            color = OffWhite.copy(alpha = 0.75f),
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        items(matches) { match ->
            val userPred = predictions.find { it.matchId == match.id && it.userId == currentUserId }
            MatchItemCard(
                match = match,
                prediction = userPred,
                currentUser = currentUser,
                isAdmin = isAdminMode,
                onEditPrediction = { viewModel.openBetSheet(match) },
                onAdminEditScore = { viewModel.openAdminMatchScoreEditor(match) },
                onAdminResetScore = { viewModel.resetMatchResult(match.id) }
            )
        }
    }

    // Admin Match Editor Dialog
    adminUpdatingMatch?.let { match ->
        AdminMatchScoreDialog(match = match, viewModel = viewModel)
    }
}

@Composable
fun MatchItemCard(
    match: MatchEntity,
    prediction: PredictionEntity?,
    currentUser: ParticipantEntity?,
    isAdmin: Boolean,
    onEditPrediction: () -> Unit,
    onAdminEditScore: () -> Unit,
    onAdminResetScore: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val lockTimeOffset = 15 * 60 * 1000L // 15 minutes in MS
    val isLocked = currentTime >= (match.startTime - lockTimeOffset)
    val isCompleted = match.status == "COMPLETED"

    val userPaid = currentUser?.registrationStatus == "APPROVED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date & Status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.groupName} • ${formatDate(match.startTime)}",
                    fontSize = 11.sp,
                    color = SlateGray,
                    fontWeight = FontWeight.SemiBold
                )

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SlateGray.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Terminado", color = OffWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isLocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberOrange.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Inmutable 🔒", color = AmberOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Abierto ✏️", color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Teams and Score Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team A
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.flagA, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        match.teamA,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Official Match Score or VS logo
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCompleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${match.scoreA}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = OffWhite
                            )
                            Text(
                                text = " - ",
                                fontSize = 20.sp,
                                color = SlateGray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "${match.scoreB}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = OffWhite
                            )
                        }
                        Text("Marcador Real", fontSize = 10.sp, color = SlateGray)
                    } else {
                        Text(
                            text = "VS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberOrange,
                            letterSpacing = 1.sp
                        )
                        CountdownText(startTime = match.startTime)
                    }
                }

                // Team B
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.flagB, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        match.teamB,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DeepSlateBlue, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer: User Prediction Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TU PRONÓSTICO",
                        fontSize = 10.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    if (prediction == null) {
                        Text(
                            text = "Sin predicción registrada",
                            fontSize = 12.sp,
                            color = OffWhite.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val betDetails = when (prediction.betMode) {
                            "PUNTAJE_EXACTO" -> "Marcador: ${prediction.scoreA} - ${prediction.scoreB}"
                            "DOBLE_OPORTUNIDAD" -> "Doble Oportunidad: ${prediction.doubleChanceTrend}"
                            "SIN_EMPATE" -> "Empate No Válido: Selección ${if (prediction.drawNoBetSelection == "1") match.teamA else match.teamB}"
                            else -> ""
                        }
                        Text(
                            text = betDetails,
                            fontSize = 13.sp,
                            color = OffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Calculate points specifically shown for this card if completed
                if (isCompleted && prediction != null) {
                    val pointsGained = calculatePointsForMatch(match, prediction)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (pointsGained > 0) EmeraldGreen.copy(alpha = 0.15f)
                                else SlateGray.copy(alpha = 0.1f)
                            )
                            .border(
                                1.dp,
                                if (pointsGained > 0) EmeraldGreen
                                else SlateGray.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+$pointsGained PT${if (pointsGained != 1) "S" else ""}",
                            color = if (pointsGained > 0) EmeraldGreen else SlateGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                } else if (!isCompleted) {
                    if (isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = SlateGray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else if (userPaid) {
                        Button(
                            onClick = onEditPrediction,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (prediction == null) "Apostar" else "Editar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Inactiva (Inscríbete)",
                            color = SlateGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Simulated Admin Controls inside the card if admin is enabled
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DeepSlateBlue, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔧 Herramientas Admin:", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    Row {
                        if (isCompleted) {
                            TextButton(onClick = onAdminResetScore) {
                                Text("Reiniciar Partido", color = AmberOrange, fontSize = 10.sp)
                            }
                        }
                        Button(
                            onClick = onAdminEditScore,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(if (isCompleted) "Ajustar Score" else "Cargar Resultado", fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownText(startTime: Long) {
    val currentTime = System.currentTimeMillis()
    val diff = startTime - currentTime
    val text = if (diff <= 0) {
        "En vivo o Cerrado"
    } else {
        val days = diff / (24 * 3600 * 1000)
        val hours = (diff / (3600 * 1000)) % 24
        val mins = (diff / (60 * 1000)) % 60
        if (days > 0) "${days}d ${hours}h" else "${hours}h ${mins}m"
    }
    Text(text, fontSize = 10.sp, color = AmberOrange.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
}

@Composable
fun LeaderboardScreen(viewModel: SportsViewModel) {
    val participants by viewModel.participants.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (participants.isNotEmpty()) {
            item {
                StaggeredFadeInItem(index = 0) {
                    PodiumSection(participants = participants.take(3))
                }
            }
        }

        item {
            StaggeredFadeInItem(index = 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "LIGA GENERAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        items(participants.drop(3)) { participant ->
            val index = participants.indexOf(participant)
            StaggeredFadeInItem(index = index + 2) {
                LeaderRowItem(participant = participant, rank = index + 1)
            }
        }

        if (participants.size <= 3) {
            item {
                StaggeredFadeInItem(index = 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No hay suficientes contrincantes para mostrar la liga.", color = SlateGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(participants: List<ParticipantEntity>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val second = participants.getOrNull(1)
        val first = participants.getOrNull(0)
        val third = participants.getOrNull(2)

        Box(modifier = Modifier.weight(1f)) {
            second?.let { PodiumCard(participant = it, rank = 2, height = 120.dp, badge = "🥈") }
        }
        Box(modifier = Modifier.weight(1.2f)) {
            first?.let { PodiumCard(participant = it, rank = 1, height = 145.dp, badge = "👑🥇") }
        }
        Box(modifier = Modifier.weight(1f)) {
            third?.let { PodiumCard(participant = it, rank = 3, height = 110.dp, badge = "🥉") }
        }
    }
}

@Composable
fun PodiumCard(participant: ParticipantEntity, rank: Int, height: androidx.compose.ui.unit.Dp, badge: String) {
    val gradientColor = when (rank) {
        1 -> listOf(AmberOrange, AmberOrange.copy(alpha = 0.6f))
        2 -> listOf(SlateGray.copy(alpha = 0.8f), SlateGray.copy(alpha = 0.4f))
        else -> listOf(SlateGray.copy(alpha = 0.4f), SlateGray.copy(alpha = 0.1f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.5.dp,
            if (participant.isCurrentUser) EmeraldGreen else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(badge, fontSize = 24.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = participant.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                if (participant.isCurrentUser) {
                    Text("(Tú)", fontSize = 9.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(gradientColor))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${participant.points} PTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (rank == 1) Color.Black else OffWhite
                )
            }
        }
    }
}

@Composable
fun LeaderRowItem(participant: ParticipantEntity, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (participant.isCurrentUser) SlateCard.copy(alpha = 0.9f) else SlateCard
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (participant.isCurrentUser) EmeraldGreen else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (participant.isCurrentUser) EmeraldGreen else SlateGray,
                modifier = Modifier.width(28.dp)
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(participant.avatarColorOrdinal)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.name.take(1).uppercase(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                    if (participant.isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(EmeraldGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("TÚ", color = EmeraldGreen, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    "Exactas: ${participant.exactHits} • Tendencias: ${participant.trendHits}",
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }

            Text(
                text = "${participant.points} PTS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (participant.isCurrentUser) EmeraldGreen else OffWhite
            )
        }
    }
}

@Composable
fun PoteScreen(viewModel: SportsViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()

    val approvedParticipantsCount = participants.count { it.registrationStatus == "APPROVED" }
    val totalUsdtPool = approvedParticipantsCount * 10.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SlateCard, SlateCard.copy(alpha = 0.5f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Pote Acumulado de la Liga",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${totalUsdtPool.toInt()} USDT",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = OffWhite
                            )
                            Text(
                                "Distribuido a costo cero vía Bybit",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isAdminMode) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pote_validation_card"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.5.dp, EmeraldGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                "🔧 CONTROL ADMINISTRATIVO: MI POTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val pendingPoteTxs = transactions.filter { it.status == "PENDING" }

                        if (pendingPoteTxs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepSlateBlue.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "✨ No existen registros de pago con estado Pendiente.",
                                    fontSize = 12.sp,
                                    color = SlateGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                "Revisa y gestiona las solicitudes de Pago Móvil de los participantes para sumarlos al pote acumulado de la liga:",
                                fontSize = 11.sp,
                                color = OffWhite.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            pendingPoteTxs.forEach { tx ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
                                    border = BorderStroke(1.dp, SlateGray.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = tx.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OffWhite
                                                )
                                                Text(
                                                    text = "ID: ${tx.userId}",
                                                    fontSize = 9.sp,
                                                    color = SlateGray
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AmberOrange.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PENDIENTE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberOrange
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Monto Registrado:",
                                                fontSize = 11.sp,
                                                color = SlateGray
                                            )
                                            Text(
                                                "${tx.amount} ${tx.amountUnit}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = EmeraldGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Referencia:",
                                                fontSize = 11.sp,
                                                color = SlateGray
                                            )
                                            Text(
                                                tx.reference,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OffWhite
                                            )
                                        }

                                        if (tx.screenshotPath != null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "📸 Captura de Pantalla del Recibo:",
                                                fontSize = 10.sp,
                                                color = AmberOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ReceiptMockPreview(
                                                phone = "Transferencia de Origen",
                                                ci = "Asociada a la Ref",
                                                ref = tx.reference,
                                                bank = "Pago Móvil",
                                                filename = tx.screenshotPath
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.adminRejectTransaction(tx.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .testTag("reject_pote_payment_${tx.id}"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Rechazar ❌", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.adminApproveTransaction(tx.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .height(40.dp)
                                                    .testTag("approve_pote_payment_${tx.id}"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Aprobar Pago 👍", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TU ESTADO DE INSCRIPCIÓN", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))

                        when (user.registrationStatus) {
                            "APPROVED" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.15f))
                                        .border(1.dp, EmeraldGreen, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("¡Participación Activada! ✅", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Ya eres elegible para apostar y ganar en el pote general.", color = OffWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            "PENDING_APPROVAL" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmberOrange.copy(alpha = 0.15f))
                                        .border(1.dp, AmberOrange, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AmberOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Verificación en Progreso ⏳", color = AmberOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("El administrador está validando tu referencia de transferencia.", color = OffWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateGray.copy(alpha = 0.15f))
                                        .border(1.dp, SlateGray, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = SlateGray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("No Inscrito / Pago Pendiente ⚠️", color = SlateGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Completa tu transferencia de 10 USDT y registra el recibo.", color = OffWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val status = currentUser?.registrationStatus ?: "UNPAID"
        if (status == "UNPAID" || status == "PENDING_APPROVAL") {
            item {
                Text(
                    text = "PAGO DE INSCRIPCIÓN (PAGO MÓVIL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PagoMovilPaymentSection(viewModel = viewModel)
                    }
                }
            }
        }

        if (transactions.isNotEmpty()) {
            item {
                Text(
                    text = "HISTORIAL DE TRANSACCIONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray,
                    letterSpacing = 1.sp
                )
            }

            items(transactions) { tx ->
                TransactionRowItem(tx = tx)
            }
        }
    }
}

@Composable
fun PagoMovilPaymentSection(viewModel: SportsViewModel) {
    val phone by viewModel.mobilePaymentPhone.collectAsState()
    val ci by viewModel.mobilePaymentCI.collectAsState()
    val ref by viewModel.mobilePaymentReference.collectAsState()
    val bank by viewModel.mobilePaymentBank.collectAsState()
    val screenshotPath by viewModel.mobilePaymentScreenshot.collectAsState()

    var showScreenshotPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Datos de Pago Móvil Administrador", fontSize = 11.sp, color = AmberOrange, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Banco: Banco Venezolano de Crédito (0104)", fontSize = 12.sp, color = OffWhite)
                Text("Teléfono Móvil: 0412-5232268", fontSize = 12.sp, color = OffWhite)
                Text("Cédula de Identidad (CI): V-17.363.468", fontSize = 12.sp, color = OffWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Text("* Pago local en Bs. Se calculará el equivalente a la tasa BCV oficial (450 Bs. aprox).", fontSize = 9.sp, color = SlateGray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { viewModel.mobilePaymentPhone.value = it },
            label = { Text("Número de teléfono emisor") },
            placeholder = { Text("Ej: 04141234567") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pago_movil_phone_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = SlateGray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = ci,
            onValueChange = { viewModel.mobilePaymentCI.value = it },
            label = { Text("Cédula de Identidad") },
            placeholder = { Text("Ej: 17363468") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pago_movil_ci_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = SlateGray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        val banks = listOf(
            "Banco de Venezuela (0102)",
            "Banco Venezolano de Crédito (0104)",
            "Banco Mercantil (0105)",
            "BBVA Provincial (0108)",
            "Bancaribe (0114)",
            "Banco Exterior (0115)",
            "Banco Caroní (0128)",
            "Banesco (0134)",
            "Banco Sofitasa (0137)",
            "Banco Plaza (0138)",
            "Bangente (0146)",
            "Banco Fondo Común (0151)",
            "100% Banco (0156)",
            "DelSur (0157)",
            "Banco del Tesoro (0163)",
            "Banco Agrícola de Venezuela (0166)",
            "Bancrecer (0168)",
            "R4 Banco Microfinanciero (0169)",
            "Banco Activo (0171)",
            "Bancamiga (0172)",
            "Banco Internacional de Desarrollo (0173)",
            "Banplus (0174)",
            "Banco Digital de los Trabajadores (0175)",
            "Banfanb (0177)",
            "N58 Banco Digital (0178)",
            "BNC (Banco Nacional de Crédito) (0191)",
            "Instituto Municipal de Crédito Popular (0601)"
        )

        StyledBankDropdownMenu(
            selectedBank = bank,
            onBankSelected = { selected ->
                viewModel.mobilePaymentBank.value = selected
            },
            banks = banks
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = ref,
            onValueChange = { viewModel.mobilePaymentReference.value = it },
            label = { Text("Últimos 6 dígitos de Referencia (ID Transacción)") },
            placeholder = { Text("Ej: 981242") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pago_movil_ref_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = SlateGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Upload Section
        Text(
            text = "CAPTURA DE PANTALLA DEL RECIBO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SlateCard)
                .border(
                    width = 1.dp,
                    color = if (screenshotPath != null) EmeraldGreen else SlateGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { showScreenshotPicker = true }
                .padding(16.dp)
                .testTag("pago_movil_upload_box"),
            contentAlignment = Alignment.Center
        ) {
            if (screenshotPath == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Cargar captura",
                        tint = AmberOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Adjuntar Captura de Pago Móvil 📸",
                        color = OffWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Haz clic para subir un capture de tu recibo bancario",
                        color = SlateGray,
                        fontSize = 10.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Captura cargada",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Captura Adjuntada ✓",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                screenshotPath ?: "",
                                color = OffWhite.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        "Cambiar 🔄",
                        color = AmberOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        screenshotPath?.let { path ->
            Spacer(modifier = Modifier.height(12.dp))
            ReceiptMockPreview(
                phone = phone.ifBlank { "04125556677" },
                ci = ci.ifBlank { "17363468" },
                ref = ref.ifBlank { "981242" },
                bank = bank,
                filename = path
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.submitPagoMovilPayment() },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_pm_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Registrar Pago Móvil", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    if (showScreenshotPicker) {
        ScreenshotPickerDialog(
            onDismiss = { showScreenshotPicker = false },
            onSelect = { selectedFile ->
                viewModel.mobilePaymentScreenshot.value = selectedFile
                showScreenshotPicker = false
            }
        )
    }
}

@Composable
fun ReceiptMockPreview(phone: String, ci: String, ref: String, bank: String, filename: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPROBANTE ELECTRÓNICO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = "PAGO MÓVIL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDEF7EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF03543F),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¡Operación Exitosa!",
                color = Color(0xFF03543F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = "450,00 Bs.",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEDF2F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            ReceiptFieldRow(label = "Firma Digital:", value = "Minuto90 Quiniela 2026")
            ReceiptFieldRow(label = "Banco Emisor:", value = bank)
            ReceiptFieldRow(label = "Teléfono Emisor:", value = phone)
            ReceiptFieldRow(label = "Cédula del Ordenante:", value = "V-$ci")
            ReceiptFieldRow(label = "Banco Receptor:", value = "Banco Venezolano de Crédito (0104)")
            ReceiptFieldRow(label = "Referencia / ID:", value = ref)
            ReceiptFieldRow(label = "Archivo Adjunto:", value = filename)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEDF2F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generado automáticamente por el gestor de subidas de Minuto 90",
                fontSize = 8.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StyledBankDropdownMenu(
    selectedBank: String,
    onBankSelected: (String) -> Unit,
    banks: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredBanks = remember(searchQuery, banks) {
        if (searchQuery.isBlank()) {
            banks
        } else {
            banks.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedBank,
            onValueChange = {},
            readOnly = true,
            label = { Text("Banco Emisor", color = if (expanded) EmeraldGreen else OffWhite.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("styled_bank_dropdown_selector")
                .clickable { expanded = !expanded },
            enabled = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = SlateGray,
                disabledBorderColor = SlateGray,
                focusedLabelColor = EmeraldGreen,
                unfocusedLabelColor = OffWhite.copy(alpha = 0.7f),
                focusedTextColor = OffWhite,
                unfocusedTextColor = OffWhite
            ),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Desplegar lista de bancos",
                        tint = if (expanded) EmeraldGreen else OffWhite,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = "" // Reset search when closed
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(DeepSlateBlue)
                .border(BorderStroke(1.dp, SlateGray.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                .heightIn(max = 280.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar banco...", color = EmeraldGreen, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("bank_search_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = OffWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = SlateGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = SlateGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = SlateGray.copy(alpha = 0.3f)
            )

            if (filteredBanks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron resultados",
                        color = SlateGray,
                        fontSize = 12.sp
                    )
                }
            } else {
                filteredBanks.forEach { b ->
                    val isSelected = b == selectedBank
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(24.dp))
                                }
                                Text(
                                    text = b,
                                    color = if (isSelected) EmeraldGreen else OffWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onBankSelected(b)
                            expanded = false
                            searchQuery = "" // Clear search
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
        Text(text = value, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScreenshotPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = BorderStroke(1.dp, AmberOrange)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar Captura de Pantalla",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simula el selector de capturas de tu teléfono Android",
                    fontSize = 11.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val mockFiles = listOf(
                    "captura_bancamiga_exitoso.png",
                    "recibo_banco_venezuela_981242.jpg",
                    "banesco_pago_movil_comprobante.png",
                    "mercantil_mobile_transfer.png",
                    "capture_pantalla_pago_exitoso.jpg"
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(mockFiles) { filename ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(filename) },
                            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateGray.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = filename,
                                        fontSize = 13.sp,
                                        color = OffWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Imagen • 1.2 MB",
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancelar", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity) {
    val statusColor = when (tx.status) {
        "APPROVED" -> EmeraldGreen
        "REJECTED" -> Color.Red
        else -> AmberOrange
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (tx.type == "BYBIT_USDT") "USDT (Transferencia Bybit)" else "Bs. (Pago Móvil)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
                Text(
                    text = "Ref: ${tx.reference}",
                    fontSize = 11.sp,
                    color = SlateGray,
                    maxLines = 1
                )
                if (tx.screenshotPath != null) {
                    Text(
                        text = "📸 Adjunto: ${tx.screenshotPath}",
                        fontSize = 10.sp,
                        color = AmberOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatDate(tx.timestamp),
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${tx.amount} ${tx.amountUnit}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = OffWhite
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (tx.status) {
                            "APPROVED" -> "Aprobado ✅"
                            "REJECTED" -> "Rechazado ❌"
                            else -> "Pendiente ⏳"
                        },
                        fontSize = 9.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: SportsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val sessionUserEmail by viewModel.sessionUserEmail.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(getAvatarColor(currentUser?.avatarColorOrdinal ?: 0)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initialLetter = (currentUser?.name ?: "T").firstOrNull()?.toString()?.uppercase() ?: "T"
                        Text(initialLetter, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.name ?: "Tú (Usuario)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )

                    sessionUserEmail?.let { email ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Puntaje de Quiniela Mundial 2026",
                        fontSize = 11.sp,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentUser?.points ?: 0}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AmberOrange
                            )
                            Text("Puntos Totales", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentUser?.exactHits ?: 0}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen
                            )
                            Text("Aciertos Exactos", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.signOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            val isSyncing by viewModel.isSyncing.collectAsState()
            val syncSuccessMessage by viewModel.syncSuccessMessage.collectAsState()
            val hasFirestoreEnabled = viewModel.repository.syncService?.isOnline() ?: false

            Card(
                modifier = Modifier.fillMaxWidth().testTag("firestore_sync_card"),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (hasFirestoreEnabled) EmeraldGreen.copy(alpha = 0.5f) else AmberOrange.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (hasFirestoreEnabled) "☁️" else "🖧", 
                                fontSize = 20.sp
                            )
                            Column {
                                Text(
                                    text = "Persistencia Firestore",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OffWhite
                                )
                                Text(
                                    text = if (hasFirestoreEnabled) "Nube Sincronizada" else "Modo Local Offline",
                                    fontSize = 11.sp,
                                    color = if (hasFirestoreEnabled) EmeraldGreen else AmberOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.syncWithFirestore() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasFirestoreEnabled) EmeraldGreen else SlateCard,
                                contentColor = if (hasFirestoreEnabled) DeepSlateBlue else OffWhite
                            ),
                            border = if (!hasFirestoreEnabled) BorderStroke(1.dp, SlateGray) else null,
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSyncing,
                            modifier = Modifier.height(36.dp).testTag("profile_sync_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = if (hasFirestoreEnabled) DeepSlateBlue else OffWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sincronizar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (syncSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = syncSuccessMessage ?: "",
                            fontSize = 12.sp,
                            color = if (syncSuccessMessage?.contains("completa") == true) EmeraldGreen else AmberOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La aplicación usa Firestore de forma nativa para almacenar partidos, predicciones y transacciones. Presiona sincronizar para refrescar o forzar la actualización offline de la liga global.",
                        fontSize = 11.sp,
                        color = SlateGray,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DIFERENTES FORMAS DE APOSTAR Y GANAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberOrange,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BetOptionExplainRow(
                        title = "🎯 1. Puntaje Exacto (Precisión Alta)",
                        reward = "+5 Puntos",
                        body = "Premia el resultado idéntico del marcador del partido. Recibes 2 puntos si fallas de marcador pero aciertas tendencia (Ganador o Empate)."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BetOptionExplainRow(
                        title = "🛡️ 2. Doble Oportunidad (Estrategia)",
                        reward = "+3 Puntos",
                        body = "Reduces el riesgo seleccionando 2 posibles resultados a la vez: Local/Empate (1X), Empate/Visitante (X2), o Local/Visitante (12). Si ocurre cualquiera, ganas!"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BetOptionExplainRow(
                        title = "⚽ 3. Empate No Válido (Seguro)",
                        reward = "+4 Puntos o Neutral",
                        body = "Eliges un ganador. Si tu equipo vence, ganas 4 puntos. Si el partido finaliza empatado, recuperas cobertura recibiendo 1 punto de seguro."
                    )
                }
            }
        }

        if (isAdminMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.5.dp, EmeraldGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔧 PANEL ADMIN: VALIDACIONES DE PAGO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Como administrador de la liga Minuto90, puedes validar o rechazar transferencias de pago móvil con un clic:",
                            fontSize = 11.sp,
                            color = OffWhite.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val pendingTxs = transactions.filter { it.status == "PENDING" }

                        if (pendingTxs.isEmpty()) {
                            Text(
                                "No hay solicitudes de pago pendientes en este momento.",
                                fontSize = 12.sp,
                                color = SlateGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            pendingTxs.forEach { tx ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DeepSlateBlue)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Usuario: ${tx.name} (${tx.type})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OffWhite
                                        )
                                        Text(
                                            text = "Ref: ${tx.reference}",
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                        Text(
                                            text = "Monto: ${tx.amount} ${tx.amountUnit}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )

                                        if (tx.screenshotPath != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("📸 Captura de Pantalla Adjunta:", fontSize = 10.sp, color = AmberOrange, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            ReceiptMockPreview(
                                                phone = "Enviado por emisor",
                                                ci = "Asociado a referencia",
                                                ref = tx.reference,
                                                bank = "Pago Móvil",
                                                filename = tx.screenshotPath
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { viewModel.adminRejectTransaction(tx.id) },
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text("Rechazar ❌", color = Color.Red, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.adminApproveTransaction(tx.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("approve_tx_button_${tx.id}")
                                            ) {
                                                Text("Aprobar Acceso 👍", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BetOptionExplainRow(title: String, reward: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OffWhite)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(EmeraldGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(reward, color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(body, fontSize = 11.sp, color = SlateGray, lineHeight = 16.sp)
    }
}

@Composable
fun PredictionDialog(match: MatchEntity, viewModel: SportsViewModel) {
    val mode by viewModel.betMode.collectAsState()
    val scoreA by viewModel.betScoreA.collectAsState()
    val scoreB by viewModel.betScoreB.collectAsState()
    val doubleChanceTrend by viewModel.doubleChanceTrend.collectAsState()
    val drawNoBetSelection by viewModel.drawNoBetSelection.collectAsState()

    Dialog(onDismissRequest = { viewModel.closeBetSheet() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Registrar Pronóstico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AmberOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${match.teamA} vs ${match.teamB}",
                    fontSize = 12.sp,
                    color = SlateGray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Selecciona el formato de apuesta:", fontSize = 11.sp, color = SlateGray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BetModeChip(
                        title = "📊 Marcador Exacto (+5 PTS)",
                        isSelected = mode == "PUNTAJE_EXACTO",
                        onClick = { viewModel.betMode.value = "PUNTAJE_EXACTO" }
                    )
                    BetModeChip(
                        title = "🛡️ Doble Oportunidad (+3 PTS)",
                        isSelected = mode == "DOBLE_OPORTUNIDAD",
                        onClick = { viewModel.betMode.value = "DOBLE_OPORTUNIDAD" }
                    )
                    BetModeChip(
                        title = "⚽ Empate No Válido (+4 o +1 PTS)",
                        isSelected = mode == "SIN_EMPATE",
                        onClick = { viewModel.betMode.value = "SIN_EMPATE" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (mode) {
                    "PUNTAJE_EXACTO" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScoreCounter(
                                teamFlag = match.flagA,
                                count = scoreA,
                                onIncrement = { viewModel.betScoreA.value = scoreA + 1 },
                                onDecrement = { if (scoreA > 0) viewModel.betScoreA.value = scoreA - 1 }
                            )

                            Text(
                                "vs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            ScoreCounter(
                                teamFlag = match.flagB,
                                count = scoreB,
                                onIncrement = { viewModel.betScoreB.value = scoreB + 1 },
                                onDecrement = { if (scoreB > 0) viewModel.betScoreB.value = scoreB - 1 }
                            )
                        }
                    }
                    "DOBLE_OPORTUNIDAD" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Combo Seleccionado:", fontSize = 11.sp, color = SlateGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DoubleChanceButton(text = "1X (Local o Empate)", isSelected = doubleChanceTrend == "1X", onClick = { viewModel.doubleChanceTrend.value = "1X" }, modifier = Modifier.weight(1f))
                                DoubleChanceButton(text = "X2 (Empate o Visita)", isSelected = doubleChanceTrend == "X2", onClick = { viewModel.doubleChanceTrend.value = "X2" }, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            DoubleChanceButton(text = "12 (Local o Visita)", isSelected = doubleChanceTrend == "12", onClick = { viewModel.doubleChanceTrend.value = "12" }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    "SIN_EMPATE" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Elige al ganador (el empate te otorga +1 punto de seguro):", fontSize = 11.sp, color = SlateGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DoubleChanceButton(text = "${match.flagA} ${match.teamA}", isSelected = drawNoBetSelection == "1", onClick = { viewModel.drawNoBetSelection.value = "1" }, modifier = Modifier.weight(1f))
                                DoubleChanceButton(text = "${match.flagB} ${match.teamB}", isSelected = drawNoBetSelection == "2", onClick = { viewModel.drawNoBetSelection.value = "2" }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.closeBetSheet() }) {
                        Text("Cancelar", color = SlateGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.savePrediction() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        modifier = Modifier.testTag("save_prediction_button")
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BetModeChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else DeepSlateBlue)
            .border(1.dp, if (isSelected) EmeraldGreen else SlateGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = if (isSelected) EmeraldGreen else OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DoubleChanceButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AmberOrange.copy(alpha = 0.2f) else DeepSlateBlue)
            .border(1.dp, if (isSelected) AmberOrange else SlateGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) AmberOrange else OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ScoreCounter(teamFlag: String, count: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(teamFlag, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onDecrement,
                colors = ButtonDefaults.buttonColors(containerColor = DeepSlateBlue),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontSize = 18.sp, color = OffWhite)
            }

            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onIncrement,
                colors = ButtonDefaults.buttonColors(containerColor = DeepSlateBlue),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, color = OffWhite)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMatchScoreDialog(match: MatchEntity, viewModel: SportsViewModel) {
    val scoreA by viewModel.adminScoreA.collectAsState()
    val scoreB by viewModel.adminScoreB.collectAsState()

    Dialog(onDismissRequest = { viewModel.closeAdminMatchScoreEditor() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🔧 Cargar Marcador Oficial",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${match.teamA} vs ${match.teamB}",
                    fontSize = 12.sp,
                    color = SlateGray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(match.teamA, fontSize = 13.sp, color = OffWhite)
                        OutlinedTextField(
                            value = scoreA,
                            onValueChange = { viewModel.adminScoreA.value = it },
                            modifier = Modifier
                                .width(70.dp)
                                .testTag("admin_score_a_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Text("vs", fontSize = 18.sp, color = SlateGray, fontWeight = FontWeight.Bold)

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(match.teamB, fontSize = 13.sp, color = OffWhite)
                        OutlinedTextField(
                            value = scoreB,
                            onValueChange = { viewModel.adminScoreB.value = it },
                            modifier = Modifier
                                .width(70.dp)
                                .testTag("admin_score_b_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.closeAdminMatchScoreEditor() }) {
                        Text("Cancelar", color = SlateGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.saveAdminMatchScore() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.testTag("admin_save_score_button")
                    ) {
                        Text("Cargar Marcador", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Helper Functions -----------------

fun calculatePointsForMatch(match: MatchEntity, prediction: PredictionEntity): Int {
    val realScoreA = match.scoreA ?: return 0
    val realScoreB = match.scoreB ?: return 0

    val realHomeWin = realScoreA > realScoreB
    val realAwayWin = realScoreA < realScoreB
    val realDraw = realScoreA == realScoreB

    return when (prediction.betMode) {
        "PUNTAJE_EXACTO" -> {
            val predictedHomeWin = prediction.scoreA > prediction.scoreB
            val predictedAwayWin = prediction.scoreA < prediction.scoreB
            val predictedDraw = prediction.scoreA == prediction.scoreB

            if (prediction.scoreA == realScoreA && prediction.scoreB == realScoreB) {
                5
            } else if ((predictedHomeWin && realHomeWin) || (predictedAwayWin && realAwayWin) || (predictedDraw && realDraw)) {
                2
            } else {
                0
            }
        }
        "DOBLE_OPORTUNIDAD" -> {
            val isHit = when (prediction.doubleChanceTrend) {
                "1X" -> realHomeWin || realDraw
                "X2" -> realAwayWin || realDraw
                "12" -> realHomeWin || realAwayWin
                else -> false
            }
            if (isHit) 3 else 0
        }
        "SIN_EMPATE" -> {
            if (realDraw) {
                1
            } else {
                val selectedIsWinner = when (prediction.drawNoBetSelection) {
                    "1" -> realHomeWin
                    "2" -> realAwayWin
                    else -> false
                }
                if (selectedIsWinner) 4 else 0
            }
        }
        else -> 0
    }
}

fun formatDate(millis: Long): String {
    val date = Date(millis)
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "VE"))
    return sdf.format(date)
}

fun getAvatarColor(ord: Int): Color {
    return when (ord) {
        0 -> Color(0xFFFFD700) // Gold
        1 -> Color(0xFF87CEEB) // Sky Blue
        2 -> Color(0xFFFFA07A) // Light Salmon
        3 -> Color(0xFFD8BFD8) // Thistle
        else -> Color(0xFFF1F5F9)
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("admin_stat_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = OffWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = SlateGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ResultsDistributionChart(homeWins: Int, draws: Int, awayWins: Int) {
    val total = (homeWins + draws + awayWins).toFloat()
    // Avoid division by zero, use default values for placeholders if none
    val homeAngle = if (total > 0) (homeWins / total) * 360f else 120f
    val drawAngle = if (total > 0) (draws / total) * 360f else 120f
    val awayAngle = if (total > 0) (awayWins / total) * 360f else 120f

    val homePercent = if (total > 0) (homeWins * 100 / total).toInt() else 33
    val drawPercent = if (total > 0) (draws * 100 / total).toInt() else 33
    val awayPercent = if (total > 0) (awayWins * 100 / total).toInt() else 34

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_results_distribution_chart"),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Distribución de Resultados ⚽",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )
            Text(
                "Victorias del equipo local, empates y visitantes",
                fontSize = 9.sp,
                color = SlateGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Donut Chart Drawing on Canvas
                Canvas(modifier = Modifier.size(70.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val chartSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
                    val offset = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Draw Home Wins Arc (EmeraldGreen)
                    drawArc(
                        color = Color(0xFF10B981), // EmeraldGreen
                        startAngle = -90f,
                        sweepAngle = homeAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        size = chartSize,
                        topLeft = offset
                    )

                    // Draw Draws Arc (AmberOrange)
                    drawArc(
                        color = Color(0xFFF59E0B), // AmberOrange
                        startAngle = -90f + homeAngle,
                        sweepAngle = drawAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        size = chartSize,
                        topLeft = offset
                    )

                    // Draw Away Wins Arc (SlateGray / Cyan)
                    drawArc(
                        color = Color(0xFF3B82F6), // Blue/Cyan Accent
                        startAngle = -90f + homeAngle + drawAngle,
                        sweepAngle = awayAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        size = chartSize,
                        topLeft = offset
                    )
                }

                // Legend Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    LegendItem(color = Color(0xFF10B981), label = "Victorias Local", value = "$homeWins partidos ($homePercent%)")
                    LegendItem(color = Color(0xFFF59E0B), label = "Empates", value = "$draws partidos ($drawPercent%)")
                    LegendItem(color = Color(0xFF3B82F6), label = "Victorias Visitante", value = "$awayWins partidos ($awayPercent%)")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )
            Text(
                text = value,
                fontSize = 9.sp,
                color = SlateGray
            )
        }
    }
}

@Composable
fun ParticipationTrendChart(matches: List<MatchEntity>, predictions: List<PredictionEntity>) {
    val sortedMatches = matches.sortedBy { it.id }.take(8)
    val maxParticipation = sortedMatches.maxOfOrNull { match ->
        predictions.count { it.matchId == match.id }
    }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_participation_trend_chart"),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Participación de Usuarios por Partido 📈",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )
            Text(
                "Cantidad de pronósticos registrados en cada juego de la jornada",
                fontSize = 9.sp,
                color = SlateGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                sortedMatches.forEach { match ->
                    val count = predictions.count { it.matchId == match.id }
                    val ratio = count.toFloat() / maxParticipation
                    val barHeightDp = (ratio * 55).coerceAtLeast(4f).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (count > 0) EmeraldGreen else SlateGray
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(barHeightDp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            EmeraldGreen,
                                            EmeraldGreen.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${match.flagA}${match.flagB}",
                            fontSize = 9.sp
                        )
                        Text(
                            text = "M${match.id}",
                            fontSize = 7.sp,
                            color = SlateGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaggeredFadeInItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L) // Beautiful staggering delay
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}

@Composable
fun ErrorBoundaryScreen(
    errorMessage: String,
    onReload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBlue)
            .padding(24.dp)
            .testTag("global_error_boundary_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🟥", fontSize = 28.sp)
                }

                Text(
                    text = "¡Tarjeta Roja en el Sistema!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Ha ocurrido un fallo inesperado en las llamadas de datos de la liga o en la navegación.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = OffWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateBlue.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "DETALLES DEL INFORME:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = OffWhite,
                            lineHeight = 14.sp
                        )
                    }
                }

                Text(
                    text = "No te preocupes, tus predicciones guardadas están seguras en el dispositivo. Presiona el botón de abajo para restablecer la vista.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SlateGray,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onReload,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = DeepSlateBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("error_reload_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar"
                        )
                        Text(
                            text = "RECARGAR LA VISTA",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: SportsViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo Hero
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AmberOrange.copy(alpha = 0.15f))
                    .border(2.dp, AmberOrange, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "90'",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = AmberOrange
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MINUTO90",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = OffWhite,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Tu quiniela del Mundial 2026. Registra tus pronósticos y asegura la total privacidad de tus datos.",
                fontSize = 12.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            // Auth Mode Toggle Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateCard)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isRegisterMode) DeepSlateBlue else Color.Transparent)
                        .clickable { isRegisterMode = false }
                        .testTag("tab_login_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        color = if (!isRegisterMode) OffWhite else SlateGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRegisterMode) DeepSlateBlue else Color.Transparent)
                        .clickable { isRegisterMode = true }
                        .testTag("tab_register_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registrarse",
                        color = if (isRegisterMode) OffWhite else SlateGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Error Display Card
            authError?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22EF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(
                            text = err,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Form Fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isRegisterMode) {
                        Column {
                            Text(
                                "Nombre Completo",
                                fontSize = 11.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_field"),
                                placeholder = { Text("Ej: Diego Primera", fontSize = 13.sp, color = SlateGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberOrange,
                                    unfocusedBorderColor = OffWhite.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }

                    Column {
                        Text(
                            "Correo Electrónico",
                            fontSize = 11.sp,
                            color = SlateGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_field"),
                            placeholder = { Text("usuario@email.com", fontSize = 13.sp, color = SlateGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberOrange,
                                unfocusedBorderColor = OffWhite.copy(alpha = 0.12f)
                            )
                        )
                    }

                    Column {
                        Text(
                            "Contraseña",
                            fontSize = 11.sp,
                            color = SlateGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_field"),
                            placeholder = { Text("Mínimo 6 caracteres", fontSize = 13.sp, color = SlateGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                            ),
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "OCULTAR" else "VER",
                                        color = AmberOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberOrange,
                                unfocusedBorderColor = OffWhite.copy(alpha = 0.12f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                viewModel.signUpWithEmailAndPassword(
                                    email = email,
                                    pss = password,
                                    fullName = name,
                                    onSuccess = {}
                                )
                            } else {
                                viewModel.signInWithEmailAndPassword(
                                    email = email,
                                    pss = password,
                                    onSuccess = {}
                                )
                            }
                        },
                        enabled = !isAuthLoading && email.isNotBlank() && password.isNotBlank() && (!isRegisterMode || name.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberOrange,
                            contentColor = Color.Black,
                            disabledContainerColor = AmberOrange.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isAuthLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "CREAR CUENTA" else "INGRESAR A MINUTO90",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Divider or OR label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(OffWhite.copy(alpha = 0.1f)))
                Text("O", fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(OffWhite.copy(alpha = 0.1f)))
            }

            // Guest Fallback Button card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎮 Pruebas de Desarrollo Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                    Text(
                        text = "Si estás probando el emulador en AI Studio, puedes ingresar instantáneamente sin configurar cuentas presionando abajo.",
                        fontSize = 10.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Button(
                        onClick = { viewModel.continueAsGuest() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("auth_guest_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = OffWhite
                        ),
                        border = BorderStroke(1.dp, OffWhite.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "ENTRAR MODO DEMO (LOCAL)",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


