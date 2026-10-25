package com.rogger.bp.ui.payment.view

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rogger.bp.R
import com.rogger.bp.ui.payment.PlanKeys
import com.rogger.bp.ui.payment.presentation.PaymentViewModel
import com.rogger.bp.ui.theme.BipandoTheme

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onBackClick: () -> Unit,
    isLimitReached: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // O limite é considerado atingido se o parâmetro externo for true OU se o estado do banco de dados confirmar
    val showLimitWarning = isLimitReached || state.isLimitReached

    LaunchedEffect(Unit) {
        viewModel.initBilling(activity)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    val anyPlanAvailable = state.mensalAvailable || state.semestralAvailable || state.anualAvailable

    // Forçar Tema Escuro e Barras Transparentes para efeito Full Screen
    BipandoTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fundo Imersivo
            Image(
                painter = painterResource(id = R.drawable.magnific_6),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay para legibilidade
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    // Header com botão de fechar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close), tint = Color.White)
                        }
                    }

                    // Título Promoção
                    Text(
                        text = stringResource(id = R.string.title_promotion),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )

                    if (showLimitWarning) {
                        Text(
                            text = stringResource(id = R.string.premium_limit_message, state.freeLimit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val isTrialEnded = state.trialEndsAtMillis != null && !com.rogger.bp.domain.policy.AccessPolicy.isTrialActive(state.trialEndsAtMillis) && !state.isPremium
                    if (isTrialEnded) {
                        Text(
                            text = stringResource(id = R.string.trial_ended_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Benefícios
                    BenefitItem(stringResource(id = R.string.text_unlimited_promotion))
                    BenefitItem(stringResource(id = R.string.text_customized_notification))
                    BenefitItem(stringResource(id = R.string.text_unlimited_categories))

                    Spacer(modifier = Modifier.height(40.dp))

                    // Seleção de Planos
                    Text(
                        text = stringResource(id = R.string.select_plan),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (!anyPlanAvailable) {
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF5CB82E))
                            }
                        } else {
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.plan_unavailable_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        if (state.mensalAvailable) {
                            PlanCard(
                                title = stringResource(id = R.string.plan_name_mensal),
                                price = state.mensalPrice,
                                isSelected = state.selectedPlanId == PlanKeys.MENSAL,
                                isActive = state.activePlanId == PlanKeys.MENSAL,
                                badge = if (state.isEligibleForTrial && state.mensalTrialText != null) stringResource(R.string.plan_mensal_trial_badge) else null,
                                onClick = { viewModel.onPlanSelect(PlanKeys.MENSAL) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (state.semestralAvailable) {
                            PlanCard(
                                title = stringResource(id = R.string.plan_name_semestral),
                                price = state.semestralPrice,
                                isSelected = state.selectedPlanId == PlanKeys.SEMESTRAL,
                                isActive = state.activePlanId == PlanKeys.SEMESTRAL,
                                badge = if (state.isEligibleForTrial && state.semestralTrialText != null) stringResource(R.string.plan_mensal_trial_badge) else null,
                                onClick = { viewModel.onPlanSelect(PlanKeys.SEMESTRAL) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (state.anualAvailable) {
                            PlanCard(
                                title = stringResource(id = R.string.plan_name_anual),
                                price = state.anualPrice,
                                isSelected = state.selectedPlanId == PlanKeys.ANUAL,
                                isActive = state.activePlanId == PlanKeys.ANUAL,
                                badge = if (state.isEligibleForTrial && state.anualTrialText != null) stringResource(R.string.plan_mensal_trial_badge) else null,
                                onClick = { viewModel.onPlanSelect(PlanKeys.ANUAL) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Banner de Trial / Info
                    val bannerText = when {
                        state.activePlanId != null -> {
                            val name = when (state.activePlanId) {
                                PlanKeys.MENSAL -> stringResource(R.string.plan_name_mensal)
                                PlanKeys.SEMESTRAL -> stringResource(R.string.plan_name_semestral)
                                PlanKeys.ANUAL -> stringResource(R.string.plan_name_anual)
                                else -> ""
                            }
                            stringResource(R.string.plan_active_info_text, name)
                        }
                        state.selectedPlanId == PlanKeys.MENSAL -> if (state.isEligibleForTrial) state.mensalTrialText else null
                        state.selectedPlanId == PlanKeys.SEMESTRAL -> if (state.isEligibleForTrial) state.semestralTrialText else null
                        state.selectedPlanId == PlanKeys.ANUAL -> if (state.isEligibleForTrial) state.anualTrialText else null
                        else -> null
                    }

                    if (bannerText != null) {
                        Surface(
                            color = Color(0xFF5CB82E).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bannerText,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5CB82E),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Botão Assinar
                    val buttonText = when {
                        state.selectedPlanId == null -> stringResource(R.string.select_plan)
                        state.selectedPlanId == state.activePlanId -> stringResource(R.string.current_plan)
                        state.activePlanId != null -> {
                            val name = when (state.selectedPlanId) {
                                PlanKeys.MENSAL -> stringResource(R.string.plan_name_mensal)
                                PlanKeys.SEMESTRAL -> stringResource(R.string.plan_name_semestral)
                                PlanKeys.ANUAL -> stringResource(R.string.plan_name_anual)
                                else -> ""
                            }
                            stringResource(R.string.change_plan_text, name)
                        }
                        else -> stringResource(R.string.btn_subscribe_now)
                    }

                    Button(
                        onClick = { viewModel.purchaseSelectedPlan(activity) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = anyPlanAvailable && state.selectedPlanId != null && state.selectedPlanId != state.activePlanId,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5CB82E),
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.information_subscription),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5CB82E), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    isSelected: Boolean,
    isActive: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF5CB82E) else Color.White.copy(alpha = 0.2f)
    val backgroundColor = if (isSelected) Color(0xFF5CB82E).copy(alpha = 0.1f) else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    if (badge != null && !isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF5CB82E),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (price.isNotEmpty()) {
                    Text(text = price, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (isActive) {
                Surface(
                    color = Color(0xFF5CB82E),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.plan_active_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF5CB82E),
                        unselectedColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
