package com.example.quantpricetracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.example.quantpricetracker.presentation.theme.QuantPriceTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuantPriceTrackerTheme {
                CryptoTrackerApp()
            }
        }
    }
}

@Composable
fun CryptoTrackerApp(viewModel: CryptoViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.isLoading && uiState.coins.all { it.priceUsd == null }) {
            // First load — show spinner
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading...", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header row with currency toggle and refresh
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Currency toggle button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleCurrency() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (uiState.currency == Currency.USD) "USD" else "CAD",
                                fontSize = 12.sp,
                                color = Color(0xFF00D9FF),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Last updated
                        if (uiState.lastUpdated.isNotEmpty()) {
                            Text(
                                text = "↻ ${uiState.lastUpdated}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        // Refresh button
                        if (!uiState.isLoading) {
                            Text(
                                text = "⟳",
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.clickable { viewModel.fetchPrices() }
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Error banner (non-destructive — prices still show)
                if (uiState.error != null) {
                    item {
                        Text(
                            text = uiState.error!!,
                            fontSize = 10.sp,
                            color = Color(0xFFFF6B6B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Coin cards
                items(uiState.coins) { coinPrice ->
                    CoinCard(
                        coinPrice = coinPrice,
                        currency = uiState.currency,
                        flash = uiState.priceFlash
                    )
                }
            }
        }
    }
}

@Composable
fun CoinCard(coinPrice: CoinPrice, currency: Currency, flash: Boolean) {
    val price = if (currency == Currency.USD) coinPrice.priceUsd else coinPrice.priceCad
    val change = coinPrice.change24h

    // Animated background flash on price update
    val bgColor by animateColorAsState(
        targetValue = if (flash && price != null) Color(0xFF1A2A1A) else Color(0xFF111111),
        animationSpec = tween(durationMillis = 500),
        label = "flashAnim"
    )

    val changeColor = when {
        change == null -> Color.Gray
        change >= 0 -> Color(0xFF00E676)   // green
        else -> Color(0xFFFF5252)           // red
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: symbol + name
            Column {
                Text(
                    text = coinPrice.coin.symbol,
                    fontSize = 14.sp,
                    color = Color(0xFF00D9FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = coinPrice.coin.displayName,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            // Right: price + 24h change
            Column(horizontalAlignment = Alignment.End) {
                if (price != null) {
                    Text(
                        text = formatPrice(price, currency),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (change != null) {
                        Text(
                            text = "${if (change >= 0) "▲" else "▼"} ${"%.2f".format(Math.abs(change))}%",
                            fontSize = 10.sp,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text("—", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

fun formatPrice(price: Double, currency: Currency): String {
    val symbol = if (currency == Currency.USD) "$" else "CA$"
    return when {
        price >= 1000 -> "$symbol${"%.0f".format(price)}"
        price >= 1 -> "$symbol${"%.2f".format(price)}"
        else -> "$symbol${"%.4f".format(price)}"
    }
}
