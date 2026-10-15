package com.rogger.bp.ui.naviation

import androidx.navigation.NavHostController

fun NavHostController.safePopBackStack(): Boolean =
    if (previousBackStackEntry != null) popBackStack() else false
