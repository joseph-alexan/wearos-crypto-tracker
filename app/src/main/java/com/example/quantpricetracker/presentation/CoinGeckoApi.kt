package com.example.quantpricetracker.presentation

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// All tracked coins
enum class Coin(
    val id: String,
    val symbol: String,
    val displayName: String
) {
    QNT("quant-network", "QNT", "Quant"),
    XRP("ripple", "XRP", "XRP"),
    XLM("stellar", "XLM", "Stellar"),
    ADA("cardano", "ADA", "Cardano"),
    ALGO("algorand", "ALGO", "Algorand"),
    HBAR("hedera-hashgraph", "HBAR", "Hedera"),
    IOTA("iota", "IOTA", "IOTA"),
    XDC("xdce-crowd-sale", "XDC", "XDC Network")
}

data class CoinPrice(
    val coin: Coin,
    val priceUsd: Double?,
    val priceCad: Double?,
    val change24h: Double?
)

interface CoinGeckoApi {
    @GET("api/v3/simple/price")
    suspend fun getPrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") currencies: String = "usd,cad",
        @Query("include_24hr_change") include24hrChange: Boolean = true
    ): Map<String, Map<String, Double>>

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/"

        fun create(): CoinGeckoApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CoinGeckoApi::class.java)
        }
    }
}
