package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("editor_user_settings", Context.MODE_PRIVATE)

    // Observable flows for settings
    private val _theme = MutableStateFlow(getSavedTheme())
    val theme: StateFlow<String> = _theme

    private val _language = MutableStateFlow(getSavedLanguage())
    val language: StateFlow<String> = _language

    private val _fontSize = MutableStateFlow(getSavedFontSize())
    val fontSize: StateFlow<Float> = _fontSize

    private val _themeColor = MutableStateFlow(getSavedThemeColor())
    val themeColor: StateFlow<String> = _themeColor

    private val _autoSave = MutableStateFlow(getSavedAutoSave())
    val autoSave: StateFlow<Boolean> = _autoSave

    private val _keywordColor = MutableStateFlow(getSavedKeywordColor())
    val keywordColor: StateFlow<String> = _keywordColor

    private val _stringColor = MutableStateFlow(getSavedStringColor())
    val stringColor: StateFlow<String> = _stringColor

    private val _commentColor = MutableStateFlow(getSavedCommentColor())
    val commentColor: StateFlow<String> = _commentColor

    private val _isMiniMapEnabled = MutableStateFlow(getSavedMiniMapEnabled())
    val isMiniMapEnabled: StateFlow<Boolean> = _isMiniMapEnabled

    private fun getSavedTheme(): String = prefs.getString("theme", "dark") ?: "dark"
    private fun getSavedLanguage(): String = prefs.getString("language", "ar") ?: "ar"
    private fun getSavedFontSize(): Float = prefs.getFloat("font_size", 14f)
    private fun getSavedThemeColor(): String = prefs.getString("theme_color", "#007acc") ?: "#007acc"
    private fun getSavedAutoSave(): Boolean = prefs.getBoolean("auto_save", true)
    private fun getSavedKeywordColor(): String = prefs.getString("keyword_color", "#569CD6") ?: "#569CD6"
    private fun getSavedStringColor(): String = prefs.getString("string_color", "#CE9178") ?: "#CE9178"
    private fun getSavedCommentColor(): String = prefs.getString("comment_color", "#6A9955") ?: "#6A9955"
    private fun getSavedMiniMapEnabled(): Boolean = prefs.getBoolean("minimap_enabled", true)

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _theme.value = theme
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat("font_size", size).apply()
        _fontSize.value = size
    }

    fun setThemeColor(colorHex: String) {
        prefs.edit().putString("theme_color", colorHex).apply()
        _themeColor.value = colorHex
    }

    fun setAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save", enabled).apply()
        _autoSave.value = enabled
    }

    fun setKeywordColor(hex: String) {
        prefs.edit().putString("keyword_color", hex).apply()
        _keywordColor.value = hex
    }

    fun setStringColor(hex: String) {
        prefs.edit().putString("string_color", hex).apply()
        _stringColor.value = hex
    }

    fun setCommentColor(hex: String) {
        prefs.edit().putString("comment_color", hex).apply()
        _commentColor.value = hex
    }

    fun setMiniMapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("minimap_enabled", enabled).apply()
        _isMiniMapEnabled.value = enabled
    }
}
