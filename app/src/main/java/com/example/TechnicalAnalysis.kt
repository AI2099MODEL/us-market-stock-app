package com.example

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalAnalysis {
    
    fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        val ema = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)
        var sum = 0.0
        for (i in 0 until period) {
            sum += prices[i]
        }
        var prevEma = sum / period
        for (i in prices.indices) {
            if (i < period - 1) {
                ema.add(0.0)
            } else if (i == period - 1) {
                ema.add(prevEma)
            } else {
                val currentEma = (prices[i] - prevEma) * multiplier + prevEma
                ema.add(currentEma)
                prevEma = currentEma
            }
        }
        return ema
    }

    fun rsiSignal(prices: List<Double>): Triple<Boolean, String?, Int> {
        if (prices.size < 15) return Triple(false, null, 0)
        
        val period = 14
        var avgGain = 0.0
        var avgLoss = 0.0
        
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period
        
        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        var rsi = 100.0 - (100.0 / (1 + rs))
        
        for (i in period + 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            
            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            rsi = 100.0 - (100.0 / (1 + rs))
        }
        
        val v = rsi
        if (v > 75) return Triple(true, "RSI Overbought (%.1f)".format(v), -10)
        if (v > 60) return Triple(true, "RSI Bullish Momentum (%.1f)".format(v), 20)
        if (v in 50.0..60.0) return Triple(true, "RSI Rising (%.1f)".format(v), 10)
        if (v < 30) return Triple(true, "RSI Oversold Reversal (%.1f)".format(v), 25)
        return Triple(false, null, 0)
    }
    
    fun macdSignal(prices: List<Double>): Triple<Boolean, String?, Int> {
        if (prices.size < 35) return Triple(false, null, 0)
        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        
        val macdLine = ema12.zip(ema26) { e12, e26 -> e12 - e26 }
        val macdForSignal = macdLine.drop(25)
        val signalLine = calculateEMA(macdForSignal, 9)
        
        if (macdForSignal.isEmpty() || signalLine.isEmpty()) return Triple(false, null, 0)
        
        val m = macdForSignal.last()
        val s = signalLine.last()
        val m_prev = macdForSignal[macdForSignal.size - 2]
        val s_prev = signalLine[signalLine.size - 2]
        
        if (m > s && m_prev <= s_prev) {
            return Triple(true, "MACD Bullish Crossover", 35)
        } else if (m > s) {
            return Triple(true, "MACD Bullish", 15)
        }
        return Triple(false, null, 0)
    }
    
    fun emaCrossover(prices: List<Double>): Triple<Boolean, String?, Int> {
        if (prices.size < 50) return Triple(false, null, 0)
        val ema9 = calculateEMA(prices, 9)
        val ema21 = calculateEMA(prices, 21)
        
        val e9_last = ema9.last()
        val e21_last = ema21.last()
        val e9_prev = ema9[ema9.size - 2]
        val e21_prev = ema21[ema21.size - 2]
        
        if (e9_last > e21_last) {
            if (e9_prev <= e21_prev) {
                return Triple(true, "EMA 9/21 Golden Cross", 28)
            }
            return Triple(true, "Bullish Trend (9 > 21 EMA)", 10)
        }
        return Triple(false, null, 0)
    }
    
    fun bollingerSqueeze(prices: List<Double>): Triple<Boolean, String?, Int> {
        if (prices.size < 20) return Triple(false, null, 0)
        
        val window = 20
        val recent = prices.takeLast(window)
        val mean = recent.average()
        val variance = recent.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)
        
        val lower = mean - 2 * std
        val upper = mean + 2 * std
        val price = prices.last()
        
        val bandwidth = if (price != 0.0) ((upper - lower) / price) * 100 else 0.0
        
        if (price > upper * 0.98 && bandwidth < 12) {
            return Triple(true, "BB Squeeze Breakout Setup", 25)
        }
        if (price > upper) {
            return Triple(true, "Upper BB Breakout", 20)
        }
        return Triple(false, null, 0)
    }
    
    
    fun calculateSMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        val sma = mutableListOf<Double>()
        var sum = prices.take(period).sum()
        sma.add(sum / period)
        for (i in period until prices.size) {
            sum += prices[i] - prices[i - period]
            sma.add(sum / period)
        }
        val paddedSma = MutableList(period - 1) { 0.0 }
        paddedSma.addAll(sma)
        return paddedSma
    }

    fun volumeBreakout(volumes: List<Long>, closes: List<Double>): Triple<Boolean, String?, Int> {
        if (volumes.size < 20 || closes.size < 20) return Triple(false, null, 0)
        
        val recentVolumes = volumes.takeLast(21).dropLast(1)
        val avgVol = recentVolumes.average()
        val currentVol = volumes.last()
        
        val currentClose = closes.last()
        val prevClose = closes[closes.size - 2]
        
        if (avgVol > 0 && currentVol > avgVol * 1.5 && currentClose > prevClose) {
            return Triple(true, "Volume Breakout (%.1fx avg)".format(currentVol / avgVol), 25)
        }
        return Triple(false, null, 0)
    }
    
    fun smaCrossover(prices: List<Double>): Triple<Boolean, String?, Int> {
        if (prices.size < 200) return Triple(false, null, 0)
        val sma50 = calculateSMA(prices, 50)
        val sma200 = calculateSMA(prices, 200)
        
        val s50_last = sma50.last()
        val s200_last = sma200.last()
        val s50_prev = sma50[sma50.size - 2]
        val s200_prev = sma200[sma200.size - 2]
        
        if (s50_last > s200_last) {
            if (s50_prev <= s200_prev) {
                return Triple(true, "SMA 50/200 Golden Cross", 35)
            }
            return Triple(true, "Long-term Bullish (50 > 200 SMA)", 15)
        }
        return Triple(false, null, 0)
    }

    fun stochasticOscillator(highs: List<Double>, lows: List<Double>, closes: List<Double>): Triple<Boolean, String?, Int> {
        val period = 14
        if (closes.size < period + 3) return Triple(false, null, 0)
        
        val kValues = mutableListOf<Double>()
        for (i in period - 1 until closes.size) {
            val windowHighs = highs.subList(i - period + 1, i + 1)
            val windowLows = lows.subList(i - period + 1, i + 1)
            val highestHigh = windowHighs.maxOrNull() ?: closes[i]
            val lowestLow = windowLows.minOrNull() ?: closes[i]
            
            val k = if (highestHigh == lowestLow) 50.0 else ((closes[i] - lowestLow) / (highestHigh - lowestLow)) * 100
            kValues.add(k)
        }
        
        val dValues = calculateSMA(kValues, 3).takeLast(2)
        val recentK = kValues.takeLast(2)
        
        if (dValues.size < 2 || recentK.size < 2) return Triple(false, null, 0)
        
        val k_last = recentK.last()
        val d_last = dValues.last()
        val k_prev = recentK.first()
        val d_prev = dValues.first()
        
        if (k_last > d_last && k_prev <= d_prev) {
            if (k_last < 20) return Triple(true, "Stoch Oversold Cross", 20)
            if (k_last < 50) return Triple(true, "Stoch Bullish Cross", 10)
        }
        return Triple(false, null, 0)
    }

    
    fun supertrendSignal(highs: List<Double>, lows: List<Double>, closes: List<Double>): Triple<Boolean, String?, Int> {
        val period = 10
        val multiplier = 3.0
        if (closes.size < period + 1) return Triple(false, null, 0)
        
        val atr = mutableListOf<Double>()
        var currentTrSum = 0.0
        
        for (i in 1 until closes.size) {
            val hl = highs[i] - lows[i]
            val hc = abs(highs[i] - closes[i - 1])
            val lc = abs(lows[i] - closes[i - 1])
            val tr = max(hl, max(hc, lc))
            
            if (i <= period) {
                currentTrSum += tr
                if (i == period) atr.add(currentTrSum / period)
            } else {
                val lastAtr = atr.last()
                atr.add((lastAtr * (period - 1) + tr) / period)
            }
        }
        
        val basicUb = mutableListOf<Double>()
        val basicLb = mutableListOf<Double>()
        for (i in period until closes.size) {
            val hl2 = (highs[i] + lows[i]) / 2.0
            val currentAtr = atr[i - period]
            basicUb.add(hl2 + multiplier * currentAtr)
            basicLb.add(hl2 - multiplier * currentAtr)
        }
        
        val finalUb = mutableListOf<Double>()
        val finalLb = mutableListOf<Double>()
        val supertrend = mutableListOf<Double>()
        val trend = mutableListOf<Int>() // 1 for up, -1 for down
        
        finalUb.add(basicUb[0])
        finalLb.add(basicLb[0])
        trend.add(1)
        supertrend.add(finalLb[0])
        
        for (i in 1 until basicUb.size) {
            val closeIdx = i + period
            val prevClose = closes[closeIdx - 1]
            val currClose = closes[closeIdx]
            
            val ub = if (basicUb[i] < finalUb[i - 1] || prevClose > finalUb[i - 1]) basicUb[i] else finalUb[i - 1]
            val lb = if (basicLb[i] > finalLb[i - 1] || prevClose < finalLb[i - 1]) basicLb[i] else finalLb[i - 1]
            
            finalUb.add(ub)
            finalLb.add(lb)
            
            var currentTrend = trend[i - 1]
            if (supertrend[i - 1] == finalUb[i - 1] && currClose > finalUb[i]) {
                currentTrend = 1
            } else if (supertrend[i - 1] == finalUb[i - 1] && currClose <= finalUb[i]) {
                currentTrend = -1
            } else if (supertrend[i - 1] == finalLb[i - 1] && currClose >= finalLb[i]) {
                currentTrend = 1
            } else if (supertrend[i - 1] == finalLb[i - 1] && currClose < finalLb[i]) {
                currentTrend = -1
            }
            
            trend.add(currentTrend)
            supertrend.add(if (currentTrend == 1) lb else ub)
        }
        
        if (trend.size >= 2) {
            val currentTrend = trend.last()
            val prevTrend = trend[trend.size - 2]
            
            if (currentTrend == 1 && prevTrend == -1) {
                return Triple(true, "SuperTrend Buy Signal", 30)
            } else if (currentTrend == 1) {
                return Triple(true, "SuperTrend Bullish", 10)
            }
        }
        
        return Triple(false, null, 0)
    }

    fun calculateTargets(highs: List<Double>, lows: List<Double>, closes: List<Double>, price: Double): Map<String, Double> {
        if (closes.size < 14) return emptyMap()
        
        val trList = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            val hl = highs[i] - lows[i]
            val hc = abs(highs[i] - closes[i - 1])
            val lc = abs(lows[i] - closes[i - 1])
            trList.add(max(hl, max(hc, lc)))
        }
        
        val period = 14
        if (trList.size < period) return emptyMap()
        
        var atr = trList.take(period).average()
        for (i in period until trList.size) {
            atr = (atr * (period - 1) + trList[i]) / period
        }
        
        // Practical AI / Technical Stop Loss calculation based on ATR volatility & swing low support
        val recentLow = if (lows.size >= 5) lows.takeLast(5).minOrNull() ?: (price * 0.98) else (price * 0.98)
        val atrStop = price - (1.8 * atr)
        // Combine ATR volatility buffer with swing low support level
        val rawStopLoss = minOf(atrStop, recentLow * 0.995)
        // Practical SL bounded between 1.5% and 5.0% below current market price depending on asset volatility
        val stopLoss = rawStopLoss.coerceIn(price * 0.95, price * 0.985)

        val riskAmount = max(price - stopLoss, price * 0.015)
        val target1 = price + (1.8 * riskAmount)
        val target2 = price + (3.0 * riskAmount)

        return mapOf(
            "stop_loss" to stopLoss,
            "target_1" to target1,
            "target_2" to target2
        )
    }

    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += Math.abs(change)
        }
        avgGain /= period
        avgLoss /= period
        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        var rsi = 100.0 - (100.0 / (1 + rs))
        for (i in period + 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) Math.abs(change) else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            rsi = 100.0 - (100.0 / (1 + rs))
        }
        return rsi
    }
    
    fun calculateVWAP(highs: List<Double>, lows: List<Double>, closes: List<Double>, volumes: List<Long>, window: Int = 20): Double {
        if (closes.size < window) return closes.lastOrNull() ?: 0.0
        var tpVolSum = 0.0
        var volSum = 0.0
        for (i in closes.size - window until closes.size) {
            val tp = (highs[i] + lows[i] + closes[i]) / 3.0
            tpVolSum += tp * volumes[i]
            volSum += volumes[i]
        }
        return if (volSum > 0.0) tpVolSum / volSum else closes.last()
    }

    fun priceBreakout(highs: List<Double>, closes: List<Double>): Triple<Boolean, String?, Int> {
        if (highs.size < 21 || closes.size < 21) return Triple(false, null, 0)
        val currentClose = closes.last()
        val prev20Highs = highs.takeLast(21).dropLast(1)
        val max20High = prev20Highs.maxOrNull() ?: 0.0

        if (max20High > 0.0 && currentClose > max20High) {
            val pct = ((currentClose - max20High) / max20High) * 100
            return Triple(true, "20-Day Resistance Breakout (+%.1f%%)".format(pct), 40)
        }

        if (highs.size >= 51 && closes.size >= 51) {
            val prev50Highs = highs.takeLast(51).dropLast(1)
            val max50High = prev50Highs.maxOrNull() ?: 0.0
            if (max50High > 0.0 && currentClose > max50High) {
                val pct = ((currentClose - max50High) / max50High) * 100
                return Triple(true, "50-Day Resistance Breakout (+%.1f%%)".format(pct), 50)
            }
        }
        return Triple(false, null, 0)
    }

}
