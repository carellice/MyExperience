package com.myexperience.app.ui.globe

import android.content.Context

/** Preferenze di visualizzazione del globo, pensate per renderlo più leggibile. */
data class GlobeSettings(
    val alwaysDay: Boolean = false,   // tutto illuminato, senza lato notte
    val sunGlint: Boolean = true,     // riflesso del sole sugli oceani
    val clouds: Boolean = true,
    val atmosphere: Boolean = true,
    val stars: Boolean = true,
    val cityLights: Boolean = true,   // luci delle città sul lato notte
    val autoRotate: Boolean = true,
    val brightness: Float = 1f,       // 0.7 .. 1.5
    val pinScale: Float = 1f,         // 0.7 .. 1.6
    val showUnesco: Boolean = true,   // patrimoni UNESCO come punti dorati
) {
    companion object {
        private const val PREFS = "globe"

        fun load(context: Context): GlobeSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = GlobeSettings()
            return GlobeSettings(
                alwaysDay = p.getBoolean("alwaysDay", d.alwaysDay),
                sunGlint = p.getBoolean("sunGlint", d.sunGlint),
                clouds = p.getBoolean("clouds", d.clouds),
                atmosphere = p.getBoolean("atmosphere", d.atmosphere),
                stars = p.getBoolean("stars", d.stars),
                cityLights = p.getBoolean("cityLights", d.cityLights),
                autoRotate = p.getBoolean("autoRotate", d.autoRotate),
                brightness = p.getFloat("brightness", d.brightness),
                pinScale = p.getFloat("pinScale", d.pinScale),
                showUnesco = p.getBoolean("showUnesco", d.showUnesco),
            )
        }

        fun save(context: Context, s: GlobeSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("alwaysDay", s.alwaysDay)
                .putBoolean("sunGlint", s.sunGlint)
                .putBoolean("clouds", s.clouds)
                .putBoolean("atmosphere", s.atmosphere)
                .putBoolean("stars", s.stars)
                .putBoolean("cityLights", s.cityLights)
                .putBoolean("autoRotate", s.autoRotate)
                .putFloat("brightness", s.brightness)
                .putFloat("pinScale", s.pinScale)
                .putBoolean("showUnesco", s.showUnesco)
                .apply()
        }
    }
}
