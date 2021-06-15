package eu.seldon1000.nextpass.api

import android.icu.text.SimpleDateFormat
import com.google.gson.JsonObject
import java.util.*

data class Folder(val folderData: JsonObject, var index: Int = -1) {
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    val id: String = folderData.get("id").asString
    val label: String = folderData.get("label").asString
    val parent: String = folderData.get("parent").asString

    val created: String? = formatter.format(Date(folderData.get("created").asLong * 1000))
    val edited: String? = formatter.format(Date(folderData.get("edited").asLong * 1000))

    val favorite: Boolean = folderData.get("favorite").asBoolean
}