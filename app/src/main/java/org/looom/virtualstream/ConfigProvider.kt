package org.looom.virtualstream

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.google.gson.Gson
import androidx.core.net.toUri

/**
 * ContentProvider 用于提供配置数据
 *
 * @TODO
 * 在xposed实现接收
 * fun readConfigFromOtherApp(context: Context): ConfigManager.Config? {
 *     val uri = Uri.parse("content://org.looom.virtualstream.configprovider/config")
 *     val cursor = context.contentResolver.query(uri, null, null, null, null)
 *
 *     cursor?.use {
 *         if (it.moveToFirst()) {
 *             val json = it.getString(it.getColumnIndexOrThrow("json"))
 *             return try {
 *                 Gson().fromJson(json, ConfigManager.Config::class.java)
 *             } catch (e: Exception) {
 *                 null
 *             }
 *         }
 *     }
 *     return null
 * }
 */
class ConfigProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "org.looom.virtualstream.configprovider"
        val URI_CONFIG: Uri = "content://$AUTHORITY/config".toUri()
    }

    override fun onCreate(): Boolean {
        ConfigManager.init(context!!) // 初始化 ConfigManager
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        if (uri != URI_CONFIG) return null

        // 获取 config 配置，转成 JSON 字符串
        val config = ConfigManager.getCurrentConfig()
        val json = Gson().toJson(config)

        // 用 MatrixCursor 返回数据
        val cursor = MatrixCursor(arrayOf("json"))
        cursor.addRow(arrayOf(json))
        return cursor
    }

    override fun getType(uri: Uri): String? = "application/json"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
