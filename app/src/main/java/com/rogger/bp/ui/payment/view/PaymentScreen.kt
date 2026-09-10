package com.rogger.bp.ui.payment.view

import android.app.Activity
import androidx.compose.animation.*
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
import com.rogger.bp.ui.payment.presentation.PaymentViewModel
import com.rogger.bp.ui.theme.BipandoTheme

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.initBilling(activity)
    }

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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.sub_title_promotion),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF5CB82E),
                        fontWeight = FontWeight.SemiBold
                    )

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

                    PlanCard(
                        title = stringResource(id = R.string.plan_name_mensal),
                        price = state.mensalPrice,
                        isSelected = state.selectedPlanId == "bipando_premium_mensal",
                        isActive = state.activePlanId == "bipando_premium_mensal",
                        onClick = { viewModel.onPlanSelect("bipando_premium_mensal") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlanCard(
                        title = stringResource(id = R.string.plan_name_semestral),
                        price = state.semestralPrice,
                        isSelected = state.selectedPlanId == "bipando_premium_semestral",
                        isActive = state.activePlanId == "bipando_premium_semestral",
                        onClick = { viewModel.onPlanSelect("bipando_premium_semestral") }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Banner de Trial / Info
                    val bannerText = when {
                        state.activePlanId != null -> {
                            val name = if (state.activePlanId == "bipando_premium_mensal") 
                                stringResource(R.string.plan_name_mensal) else stringResource(R.string.plan_name_semestral)
                            stringResource(R.string.plan_active_info_text, name)
                        }
                        state.selectedPlanId == "bipando_premium_mensal" -> state.mensalTrialText
                        state.selectedPlanId == "bipando_premium_semestral" -> state.semestralTrialText
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
                            val name = if (state.selectedPlanId == "bipando_premium_mensal") 
                                stringResource(R.string.plan_name_mensal) else stringResource(R.string.plan_name_semestral)
                            stringResource(R.string.change_plan_text, name)
                        }
                        else -> stringResource(R.string.btn_subscribe_now)
                    }

                    Button(
                        onClick = { viewModel.purchaseSelectedPlan(activity) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = state.selectedPlanId != null && state.selectedPlanId != state.activePlanId,
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
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
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
                        text = "ATIVO",
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
