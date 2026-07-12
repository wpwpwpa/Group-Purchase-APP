package com.example.dealoptimizer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前优惠券模式：true = 叠加券，false = 单用券。
 * 跨页持久化（SharedPreferences），默认叠加券。
 */
@Singleton
class CouponModeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isStackableMode = MutableStateFlow(prefs.getBoolean(KEY, DEFAULT_STACKABLE))
    val isStackableMode: StateFlow<Boolean> = _isStackableMode.asStateFlow()

    fun setStackableMode(value: Boolean) {
        prefs.edit { putBoolean(KEY, value) }
        _isStackableMode.value = value
    }

    companion object {
        private const val PREFS_NAME = "deal_optimizer_prefs"
        private const val KEY = "coupon_mode_stackable"
        private const val DEFAULT_STACKABLE = true
    }
}
