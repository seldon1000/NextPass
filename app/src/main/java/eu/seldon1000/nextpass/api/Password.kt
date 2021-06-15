package eu.seldon1000.nextpass.api

import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.painter.Painter
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonObject
import java.util.*

data class Password(val passwordData: JsonObject, var index: Int = -1) {
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    val id: String = passwordData.get("id").asString
    val label: String = passwordData.get("label").asString
    val url: String = passwordData.get("url").asString
    val username: String = passwordData.get("username").asString
    val password: String = passwordData.get("password").asString
    val notes: String = passwordData.get("notes").asString
    val hash: String = passwordData.get("hash").asString
    val folder: String = passwordData.get("folder").asString
    var customFields: SnapshotStateList<SnapshotStateMap<String, String>> =
        ObjectMapper().readValue(
            passwordData.get("customFields").asString,
            object : TypeReference<SnapshotStateList<SnapshotStateMap<String, String>>>() {}
        )

    val created: String = formatter.format(Date(passwordData.get("created").asLong * 1000))
    val edited: String = formatter.format(Date(passwordData.get("edited").asLong * 1000))

    val favorite: Boolean = passwordData.get("favorite").asBoolean
    val shared: Boolean = passwordData.get("shared").asBoolean

    val status: Int = passwordData.get("status").asInt

    var favicon: Painter? = null

    fun restoreCustomFields() {
        customFields = ObjectMapper().readValue(
            passwordData.get("customFields").asString,
            object : TypeReference<SnapshotStateList<SnapshotStateMap<String, String>>>() {}
        )
    }
}