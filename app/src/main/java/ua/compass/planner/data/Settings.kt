package ua.compass.planner.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.util.Locale

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, SYSTEM) ?: SYSTEM)
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _firstDay = MutableStateFlow(
        if (prefs.getString(KEY_FIRST_DAY, "mon") == "sun") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
    )
    val firstDay: StateFlow<DayOfWeek> = _firstDay.asStateFlow()

    fun setFirstDay(day: DayOfWeek) {
        prefs.edit().putString(KEY_FIRST_DAY, if (day == DayOfWeek.SUNDAY) "sun" else "mon").apply()
        _firstDay.value = day
    }

    val language: String get() = prefs.getString(KEY_LANG, SYSTEM) ?: SYSTEM

    fun setTheme(value: String) {
        prefs.edit().putString(KEY_THEME, value).apply()
        _theme.value = value
    }

    fun setLanguage(value: String) {
        prefs.edit().putString(KEY_LANG, value).commit()
    }

    companion object {
        const val SYSTEM = "system"
        private const val PREFS = "settings"
        private const val KEY_THEME = "theme"
        private const val KEY_LANG = "lang"
        private const val KEY_FIRST_DAY = "first_day"

        /** Applies the language chosen in the app settings to [base]. */
        fun wrapLocale(base: Context): Context {
            val lang = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, SYSTEM)
            if (lang.isNullOrEmpty() || lang == SYSTEM) return base
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocales(LocaleList(locale))
            return base.createConfigurationContext(config)
        }
    }
}
