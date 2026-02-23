package com.example.quantpricetracker.presentation

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Currency { USD, CAD }

data class UiState(
    val coins: List<CoinPrice> = Coin.values().map { CoinPrice(it, null, null, null) },
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastUpdated: String = "",
    val currency: Currency = Currency.USD,
    val priceFlash: Boolean = false
)

class CryptoViewModel(application: Application) : AndroidViewModel(application) {
    private val api = CoinGeckoApi.create()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val previousPrices = mutableMapOf<String, Double>()

    init {
        fetchPrices()
        startAutoRefresh()
    }

    fun fetchPrices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ids = Coin.values().joinToString(",") { it.id }
                val response = api.getPrices(ids = ids)

                val updatedCoins = Coin.values().map { coin ->
                    val data = response[coin.id]
                    CoinPrice(
                        coin = coin,
                        priceUsd = data?.get("usd"),
                        priceCad = data?.get("cad"),
                        change24h = data?.get("usd_24h_change")
                    )
                }

                checkBigMoves(updatedCoins)

                val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())

                _uiState.value = _uiState.value.copy(
                    coins = updatedCoins,
                    isLoading = false,
                    error = null,
                    lastUpdated = currentTime,
                    priceFlash = true
                )

                delay(600)
                _uiState.value = _uiState.value.copy(priceFlash = false)

            } catch (e: Exception) {
                // Keep last known prices on error
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error – showing last known price"
                )
            }
        }
    }

    fun toggleCurrency() {
        val next = if (_uiState.value.currency == Currency.USD) Currency.CAD else Currency.USD
        _uiState.value = _uiState.value.copy(currency = next)
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (!_uiState.value.isLoading) {
                    fetchPrices()
                }
            }
        }
    }

    private fun checkBigMoves(updatedCoins: List<CoinPrice>) {
        var triggered = false
        updatedCoins.forEach { coinPrice ->
            val symbol = coinPrice.coin.symbol
            val currentPrice = coinPrice.priceUsd ?: return@forEach
            val prevPrice = previousPrices[symbol]
            if (prevPrice != null && prevPrice > 0) {
                val changePct = Math.abs((currentPrice - prevPrice) / prevPrice * 100)
                if (changePct >= 5.0) triggered = true
            }
            previousPrices[symbol] = currentPrice
        }
        if (triggered) triggerHaptic()
    }

    private fun triggerHaptic() {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        } catch (e: Exception) {
            // Vibration not available — silently skip
        }
    }
}
