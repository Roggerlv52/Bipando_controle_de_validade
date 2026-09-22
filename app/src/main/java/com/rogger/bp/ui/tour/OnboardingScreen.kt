package com.rogger.bp.ui.tour

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rogger.bp.R
import com.rogger.bp.ui.theme.BipandoTheme
import kotlinx.coroutines.launch

data class OnboardingPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val imageRes: Int
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_categories,
            descriptionRes = R.string.onboarding_desc_categories,
            imageRes = R.drawable.category
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_groups,
            descriptionRes = R.string.onboarding_desc_groups,
            imageRes = R.drawable.group
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_alerts,
            descriptionRes = R.string.onboarding_desc_alerts,
            imageRes = R.drawable.alerta
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    BipandoTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { index ->
                    OnboardingPageContent(page = pages[index])
                }

                // Navegação inferior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicadores
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pages.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) Color(0xFF5CB82E)
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    // Botão Próximo / Começar
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5CB82E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pages.size - 1) {
                                stringResource(R.string.onboarding_btn_start)
                            } else {
                                stringResource(R.string.onboarding_btn_next)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(page.imageRes)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(), // Margem para a ilustração não encostar nas bordas
                contentScale = ContentScale.FillWidth
            )
            // Gradiente para suavizar a transição para o texto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 300f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(page.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
