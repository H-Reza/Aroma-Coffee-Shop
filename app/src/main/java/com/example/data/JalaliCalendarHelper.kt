package com.example.data

import java.util.Calendar

object JalaliCalendarHelper {

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = if (gm > 2) gy else gy - 1
        var gDays = 365 * gy + gy2 / 4 - gy2 / 100 + gy2 / 400
        for (i in 1 until gm) {
            gDays += gDaysInMonth[i]
        }
        // Account for leap year of current year if gm > 2
        var isLeap = false
        if (gy % 4 == 0) {
            if (gy % 100 == 0) {
                if (gy % 400 == 0) {
                    isLeap = true
                }
            } else {
                isLeap = true
            }
        }
        if (isLeap && gm > 2) {
            gDays += 1
        }
        gDays += gd

        val jDays = gDays - 737274
        val jNp = jDays / 12053
        var jDaysLeft = jDays % 12053

        var jy = 979 + 33 * jNp + 4 * (jDaysLeft / 1461)
        jDaysLeft %= 1461

        if (jDaysLeft >= 366) {
            jy += (jDaysLeft - 1) / 365
            jDaysLeft = (jDaysLeft - 1) % 365
        }

        var jm = 1
        for (i in 1..12) {
            val daysInThisMonth = if (i == 12 && isJalaliLeap(jy)) 30 else jDaysInMonth[i]
            if (jDaysLeft >= daysInThisMonth) {
                jDaysLeft -= daysInThisMonth
                jm++
            } else {
                break
            }
        }
        val jd = jDaysLeft + 1
        return JalaliDate(jy, jm, jd)
    }

    private fun isJalaliLeap(jy: Int): Boolean {
        val r = (jy - 979) % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    fun getTodayJalaliDate(): JalaliDate {
        val today = Calendar.getInstance()
        val gy = today.get(Calendar.YEAR)
        val gm = today.get(Calendar.MONTH) + 1
        val gd = today.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    // Returns dynamic next delivery dates from current time, given a delay in days
    fun getFutureJalaliDate(daysAhead: Int): JalaliDate {
        val targetDate = Calendar.getInstance()
        targetDate.add(Calendar.DAY_OF_YEAR, daysAhead)
        val gy = targetDate.get(Calendar.YEAR)
        val gm = targetDate.get(Calendar.MONTH) + 1
        val gd = targetDate.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    fun getPersianWeekdayName(daysAhead: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return getPersianWeekdayNameForCal(cal)
    }

    fun getPersianWeekdayNameForCal(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنجشنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.MONTH, Calendar.MARCH)
        cal.set(Calendar.DAY_OF_MONTH, 21)
        
        var days = 0
        val yearDiff = jy - 1405
        if (yearDiff > 0) {
            for (y in 1405 until jy) {
                days += if (isJalaliLeap(y)) 366 else 365
            }
        } else if (yearDiff < 0) {
            for (y in jy until 1405) {
                days -= if (isJalaliLeap(y)) 366 else 365
            }
        }
        
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (m in 1 until jm) {
            days += if (m == 12 && isJalaliLeap(jy)) 30 else jDaysInMonth[m]
        }
        
        days += jd - 1
        
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal
    }

    fun getFutureDeliveries(jalaliDateStr: String, frequencyFa: String, count: Int = 3): List<String> {
        return try {
            val parts = jalaliDateStr.split("/")
            if (parts.size == 3) {
                val jy = parts[0].toInt()
                val jm = parts[1].toInt()
                val jd = parts[2].toInt()
                
                val startCal = jalaliToGregorian(jy, jm, jd)
                val intervalDays = when (frequencyFa) {
                    "هفتگی" -> 7
                    "دو هفته یکبار" -> 14
                    else -> 30
                }
                
                val resultList = mutableListOf<String>()
                for (i in 1..count) {
                    val clonedCal = startCal.clone() as Calendar
                    clonedCal.add(Calendar.DAY_OF_YEAR, i * intervalDays)
                    val gy = clonedCal.get(Calendar.YEAR)
                    val gm = clonedCal.get(Calendar.MONTH) + 1
                    val gd = clonedCal.get(Calendar.DAY_OF_MONTH)
                    val nextJalali = gregorianToJalali(gy, gm, gd)
                    val weekday = getPersianWeekdayNameForCal(clonedCal)
                    resultList.add("$weekday، ${nextJalali.day} ${nextJalali.getMonthNameFa()} ${nextJalali.year}")
                }
                resultList
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        fun format(separator: String = "/"): String {
            val yStr = year.toString()
            val mStr = month.toString().padStart(2, '0')
            val dStr = day.toString().padStart(2, '0')
            return "$yStr$separator$mStr$separator$dStr"
        }

        fun getMonthNameFa(): String {
            return when (month) {
                1 -> "فروردین"
                2 -> "اردیبهشت"
                3 -> "خرداد"
                4 -> "تیر"
                5 -> "مرداد"
                6 -> "شهریور"
                7 -> "مهر"
                8 -> "آبان"
                9 -> "آذر"
                10 -> "دی"
                11 -> "بهمن"
                12 -> "اسفند"
                else -> ""
            }
        }

        fun getFullFormattedText(weekday: String): String {
            return "$weekday، $day ${getMonthNameFa()} $year"
        }
    }
}
