package com.rogger.bp.ui.home.view.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.rogger.bp.R
import com.rogger.bp.domain.model.Product
import com.rogger.bp.util.TimeFormatter
import kotlin.text.ifEmpty

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 26/08/2026
 * Hora: 19:59
 */
@Composable
fun ProductItem(
    product: Product,
    isItemVisible: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val formattedDate = remember(product.timestamp) {
        TimeFormatter.formatTimestamp(product.timestamp)
    }

    val context = LocalContext.current
    val imageRequest = remember(product.imageUri) {
        ImageRequest.Builder(context)
            .data(product.imageUri.ifEmpty { R.drawable.ic_shopping })
            .size(200, 200) // Tamanho otimizado para thumbnail
            .precision(Precision.INEXACT) // Maior performance no cache
            .crossfade(false) // Sem animação para scroll ultra-suave
            .build()
    }

    // Só aplica AnimatedVisibility se o item estiver em processo de remoção
    // Isso reduz a profundidade da árvore de UI durante o scroll normal
    if (isItemVisible) {
        ProductItemContent(product, formattedDate, imageRequest, showDivider, onClick, onImageClick)
    } else {
        AnimatedVisibility(
            visible = false,
            exit = shrinkVertically(
                animationSpec = tween(500),
                shrinkTowards = Alignment.Top
            ) + fadeOut()
        ) {
            ProductItemContent(product, formattedDate, imageRequest, showDivider, onClick, onImageClick)
        }
    }
}
