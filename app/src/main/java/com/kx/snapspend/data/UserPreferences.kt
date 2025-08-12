package com.kx.snapspend.data

import android.content.Context

object UserPreferences {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_CONSENT_GIVEN = "consent_given"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveConsent(context: Context, hasConsented: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONSENT_GIVEN, hasConsented).apply()
    }

    fun hasConsented(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CONSENT_GIVEN, false)
    }
}