package com.rogger.bp.ui.login.view

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.rogger.bp.R
import com.rogger.bp.ui.componentes.GoogleSignInButton
import com.rogger.bp.ui.login.presentation.LoginViewModel
import com.rogger.bp.ui.theme.BipandoTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    // Forçar o tema escuro apenas nesta tela para garantir o visual imersivo
    BipandoTheme(darkTheme = true) {
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        // Configuração do Google Sign-In
        val gso = remember {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        }
        val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.loginWithGoogle(it) }
            } catch (e: ApiException) { }
        }

        // Imagens do Slideshow
        val images = remember {
            listOf(
                R.drawable.magic_1,
                R.drawable.picture_3,
                R.drawable.magnific_5,
                R.drawable.magnific_3,
                R.drawable.magnific_2,
                R.drawable.magnific_6
            )
        }

        var currentImageIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(3500.milliseconds)
                currentImageIndex = (currentImageIndex + 1) % images.size
            }
        }

        LaunchedEffect(state.loginSuccess) {
            if (state.loginSuccess) {
                onLoginSuccess()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Fundo com Slideshow ocupando a tela toda (Edge-to-Edge)
            Crossfade(
                targetState = images[currentImageIndex],
                animationSpec = tween(durationMillis = 1000),
                label = "BackgroundSlideshow"
            ) { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 2. Overlay escuro e ScrollView (verticalScroll)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Usando Spacer com min height em vez de weight para funcionar bem no scroll
                    Spacer(modifier = Modifier.height(60.dp))

                    // Área do Logo
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bipando_smoll),
                            contentDescription = "Logo Bipando",
                            modifier = Modifier.size(280.dp)
                        )

                        Text(
                            text = stringResource(id = R.string.txt_logo_bottom),
                            color = Color(0xFF5CB82E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .offset(y = (-55).dp)
                                .padding(end = 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Mensagens de Boas-vindas
                    Text(
                        text = "Bem-vindo ao Bipando",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Faça login para continuar",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    // Área de Erro
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Botão Google
                    GoogleSignInButton(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        isLoading = state.isLoading
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
