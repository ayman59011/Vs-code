package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autosave_prefs")

class AutoSaveDataStore(private val context: Context) {
    companion object {
        val KEY_LAST_FILE_ID = intPreferencesKey("last_file_id")
        val KEY_UNSAVED_CONTENT = stringPreferencesKey("unsaved_content")
    }

    val lastFileId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_FILE_ID]
    }

    val unsavedContent: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_UNSAVED_CONTENT]
    }

    suspend fun saveBackup(fileId: Int, content: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_FILE_ID] = fileId
            prefs[KEY_UNSAVED_CONTENT] = content
        }
    }

    suspend fun clearBackup() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_FILE_ID)
            prefs.remove(KEY_UNSAVED_CONTENT)
        }
    }
}
