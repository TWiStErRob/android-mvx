package net.twisterrob.mvx.god

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView.BufferType
import android.widget.Toast
import net.twisterrob.mvx.LoggerInner
import net.twisterrob.mvx.R

class LoginActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.login_activity)

		val login = LoggerInner(this)
		findViewById<EditText>(R.id.email_edit).setText(login.lastLoggedInUser, BufferType.NORMAL)
		findViewById<Button>(R.id.login).setOnClickListener {
			val email = findViewById<EditText>(R.id.email_edit).text.toString()
			val password = findViewById<EditText>(R.id.password_edit).text.toString()
			val userId = login.login(email, password)
			login.lastLoggedInUser = email
			Toast.makeText(this, "Logged in as $userId", Toast.LENGTH_LONG).show()
		}
	}
}
