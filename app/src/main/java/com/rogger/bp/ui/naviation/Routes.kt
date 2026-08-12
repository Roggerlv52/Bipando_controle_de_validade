package com.rogger.bp.ui.naviation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CATEGORY = "category"
    const val SCANNER = "scanner/{categoryId}"
    const val ADD_PRODUCT = "add_product/{barcode}/{categoryId}"
    const val PROFILE = "profile"
    const val EDIT_PRODUCT = "edit_product/{uuid}"
    const val TRASH = "trash"
    const val PAYMENT = "payment"
    const val GROUPS = "groups"
    const val IMAGE_PREVIEW = "image_preview?uri={uri}&barcode={barcode}"
}
