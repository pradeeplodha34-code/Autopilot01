package com.autopilot.app.automation

import java.util.Locale

/**
 * Country and timezone mappings ported from original APK classes (ox0.java and em2.java).
 */
object CountryRegionCatalog {

    val countryNames: Map<String, String> = mapOf(
        "US" to "United States",
        "UK" to "United Kingdom",
        "GB" to "United Kingdom",
        "CA" to "Canada",
        "DE" to "Germany",
        "FR" to "France",
        "IN" to "India",
        "AU" to "Australia",
        "JP" to "Japan",
        "SG" to "Singapore",
        "BR" to "Brazil",
        "NL" to "Netherlands",
        "RU" to "Russia",
        "CN" to "China",
        "KR" to "South Korea",
        "ID" to "Indonesia",
        "MY" to "Malaysia",
        "PH" to "Philippines",
        "VN" to "Vietnam",
        "TH" to "Thailand",
        "TR" to "Turkey",
        "UA" to "Ukraine",
        "PL" to "Poland",
        "ES" to "Spain",
        "IT" to "Italy",
        "MX" to "Mexico",
        "AR" to "Argentina",
        "BD" to "Bangladesh",
        "PK" to "Pakistan",
        "EG" to "Egypt",
        "ZA" to "South Africa",
        "NG" to "Nigeria",
        "AE" to "UAE",
        "SA" to "Saudi Arabia",
        "HK" to "Hong Kong",
        "TW" to "Taiwan",
        "IR" to "Iran",
        "IQ" to "Iraq",
        "CO" to "Colombia",
        "EC" to "Ecuador",
        "PE" to "Peru",
        "CL" to "Chile",
        "CZ" to "Czechia",
        "RO" to "Romania",
        "HU" to "Hungary",
        "BG" to "Bulgaria",
        "SK" to "Slovakia",
        "AT" to "Austria",
        "CH" to "Switzerland",
        "SE" to "Sweden",
        "NO" to "Norway",
        "DK" to "Denmark",
        "FI" to "Finland",
        "IE" to "Ireland",
        "PT" to "Portugal",
        "BE" to "Belgium",
        "GR" to "Greece",
        "IL" to "Israel",
        "KZ" to "Kazakhstan",
        "UZ" to "Uzbekistan",
        "NP" to "Nepal",
        "LK" to "Sri Lanka"
    )

    val countryTimezones: Map<String, String> = mapOf(
        "US" to "America/New_York",
        "GB" to "Europe/London",
        "UK" to "Europe/London",
        "CA" to "America/Toronto",
        "DE" to "Europe/Berlin",
        "FR" to "Europe/Paris",
        "IN" to "Asia/Kolkata",
        "AU" to "Australia/Sydney",
        "JP" to "Asia/Tokyo",
        "SG" to "Asia/Singapore",
        "BR" to "America/Sao_Paulo",
        "NL" to "Europe/Amsterdam",
        "RU" to "Europe/Moscow",
        "CN" to "Asia/Shanghai",
        "KR" to "Asia/Seoul",
        "MY" to "Asia/Kuala_Lumpur",
        "ID" to "Asia/Jakarta",
        "PH" to "Asia/Manila",
        "VN" to "Asia/Ho_Chi_Minh",
        "TH" to "Asia/Bangkok"
    )

    val quickSelectCountries: List<Pair<String, String>> = listOf(
        "Any" to "Any Region",
        "US" to "United States",
        "GB" to "United Kingdom",
        "CA" to "Canada",
        "DE" to "Germany",
        "FR" to "France",
        "IN" to "India",
        "AU" to "Australia",
        "JP" to "Japan",
        "SG" to "Singapore",
        "NL" to "Netherlands",
        "BR" to "Brazil"
    )

    fun resolveCountryCode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val upper = trimmed.uppercase(Locale.ROOT)
        if (upper == "UK") return "GB"
        if (countryNames.containsKey(upper)) return upper
        val byName = countryNames.entries.firstOrNull {
            it.value.equals(trimmed, ignoreCase = true)
        }?.key
        return when (byName) {
            "UK" -> "GB"
            null -> if (upper.length == 2) upper else ""
            else -> byName
        }
    }

    fun resolveCountryName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Unknown"
        val upper = trimmed.uppercase(Locale.ROOT)
        return countryNames[upper] ?: trimmed
    }

    fun timezoneForCountry(raw: String): String? {
        val code = resolveCountryCode(raw)
        if (code.isEmpty()) return null
        return countryTimezones[code]
    }
}
