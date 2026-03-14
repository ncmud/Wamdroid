package org.ncmud.mudwammer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream

object AppStateKeys {
    val STATUS_BAR_HEIGHT = intPreferencesKey("status_bar_height")
    val TITLE_BAR_HEIGHT = intPreferencesKey("title_bar_height")
    val TEST_VERSION = intPreferencesKey("test_version")
    val LUA_LIBS_VERSION = intPreferencesKey("lua_libs_version")
}

private object PreferencesJavaIoSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences = PreferencesSerializer.defaultValue

    override suspend fun readFrom(input: InputStream): Preferences =
        PreferencesSerializer.readFrom(input.source().buffer())

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val sink = output.sink().buffer()
        PreferencesSerializer.writeTo(t, sink)
        sink.flush()
    }
}

object AppStateStore {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getInstance(context: Context): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: MultiProcessDataStoreFactory.create<Preferences>(
                serializer = PreferencesJavaIoSerializer,
                produceFile = {
                    context.applicationContext.filesDir.resolve("datastore/app_state.preferences_pb")
                }
            ).also { instance = it }
        }
    }

    @JvmStatic
    fun getInt(context: Context, key: Preferences.Key<Int>, defaultValue: Int): Int = runBlocking {
        getInstance(context).data.first()[key] ?: defaultValue
    }

    @JvmStatic
    fun putInt(context: Context, key: Preferences.Key<Int>, value: Int): Unit = runBlocking {
        getInstance(context).edit { it[key] = value }
    }

    @JvmStatic
    fun getAll(context: Context): Preferences = runBlocking {
        getInstance(context).data.first()
    }
}
