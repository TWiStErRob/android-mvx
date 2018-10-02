package net.twisterrob.mvx

import android.content.Context
import android.content.SharedPreferences

typealias UserID = Long

class LoggerInner(context: Context) {

	private val prefs: SharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)

	var lastLoggedInUser: String?
		get() = prefs.getString("lastUserName", null)
		set(value) = prefs.edit().putString("lastUserName", value).apply()

	@Suppress("UNUSED_PARAMETER")
	fun login(email: String, password: String): UserID = 1234
}
