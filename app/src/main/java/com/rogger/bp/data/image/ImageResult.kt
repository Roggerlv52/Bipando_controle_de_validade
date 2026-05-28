package com.rogger.bp.data.image

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 28/05/2026
 * Hora: 20:11
 */

/**
 * Resultado tipado do fluxo de resolução de imagem.
 *
 * Usado em [ImageResolutionRepository] para encapsular o estado
 * sem lançar exceções — o Presenter decide o que fazer com cada caso.
 */
sealed class ImageResult {

    /**
     * Imagem personalizada do usuário encontrada.
     * @param url URL pública no Storage: /produtos/{uid}/{barcode}.jpg
     * @param isCustom sempre true — útil para UI mostrar indicador "imagem personalizada"
     */
    data class CustomImage(val url: String, val isCustom: Boolean = true) : ImageResult()

    /**
     * Imagem global encontrada (criada por outro usuário ou pelo próprio na 1ª vez).
     * @param url URL pública no Storage: /imagens_produtos/{barcode}.jpg
     * @param name nome do produto associado à imagem global
     * @param isCustom sempre false
     */
    data class GlobalImage(val url: String, val name: String = "", val isCustom: Boolean = false) : ImageResult()

    /**
     * Nenhuma imagem existe (nem global nem personalizada).
     * A UI deve oferecer a opção de fazer upload — que criará a imagem GLOBAL.
     */
    object NoImage : ImageResult()

    /**
     * Erro durante a busca (rede, autenticação, permissão).
     * @param message mensagem legível para exibir ao utilizador
     */
    data class Error(val message: String) : ImageResult()
}

/**
 * Resultado tipado do fluxo de upload de imagem.
 */
sealed class UploadResult {

    /** Upload concluído com sucesso. */
    data class Success(val url: String) : UploadResult()

    /** Erro durante o upload. */
    data class Error(val message: String) : UploadResult()

    /** Progresso do upload (0–100). Opcional — usado para ProgressBar. */
    data class Progress(val percent: Int) : UploadResult()
}
