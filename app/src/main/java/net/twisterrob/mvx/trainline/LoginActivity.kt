package net.twisterrob.mvx.trainline

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.login_activity.view.*
import net.twisterrob.mvx.LoggerInner
import net.twisterrob.mvx.R
import net.twisterrob.mvx.UserID
import rx.Completable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers

class LoginActivity : AppCompatActivity(), LoginActivityContract.View {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.login_activity)
		// Dependency Injection
		val interactor = LoginOrchestrator(LoggerInner(this))
		val loginPresenter = LoginPresenter(LoginView(window.decorView), LoginInteractions(this), interactor)
		val activityPresenter = LoginActivityPresenter(interactor, LoginModelMapper(), loginPresenter)
		activityPresenter.init()
		activityPresenter.start()
	}

	override fun loggedIn(user: String) {
		Toast.makeText(this, "Logged in as $user", Toast.LENGTH_LONG).show()
	}
}

interface LoginActivityContract {

	interface View {
		fun loggedIn(user: String)
	}

	interface Presenter {
		fun init()
		fun start()
	}
}

class LoginActivityPresenter(
	private val loginOrchestrator: LoginOrchestrator,
	private val loginMapper: LoginModelMapper,
	private val loginPresenter: LoginPresenter
) : LoginActivityContract.Presenter {

	override fun init() {
		loginPresenter.init()
	}

	override fun start() {
		loginOrchestrator
			.getLastLoggedInUser()
			.map(loginMapper)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
				{ loginPresenter.bind(it) },
				{ Log.e("Login", "Failed", it) }
			)
	}
}

class LoginInteractions(
	private val view: LoginActivityContract.View
) : LoginContract.Interactions {

	override fun loggedIn(userID: UserID) {
		view.loggedIn(userID.toString())
	}
}

interface LoginContract {

	interface View {
		var email: String
		var password: String
		fun setPresenter(presenter: Presenter)
	}

	interface Presenter {
		fun init()
		fun bind(model: LoginPrefilledModel)
		fun loginClicked()
	}

	interface Interactions {
		fun loggedIn(userID: UserID)
	}
}

class LoginPrefilledModel(
	val username: String,
	val password: String
)

class LoginView(
	private val view: View
) : LoginContract.View {

	private lateinit var presenter: LoginContract.Presenter

	init {
		view.login.setOnClickListener {
			presenter.loginClicked()
		}
	}

	override fun setPresenter(presenter: LoginContract.Presenter) {
		this.presenter = presenter
	}

	override var email: String
		get() = view.email_edit.text.toString()
		set(value) = view.email_edit.setText(value)

	override var password: String
		get() = view.password_edit.text.toString()
		set(value) = view.password_edit.setText(value)
}

class LoginPresenter(
	private val view: LoginContract.View,
	private val interactions: LoginContract.Interactions,
	private val loginOrchestrator: LoginOrchestrator
) : LoginContract.Presenter {

	override fun init() {
		view.setPresenter(this)
	}

	override fun bind(model: LoginPrefilledModel) {
		view.email = model.username
		view.password = model.password
	}

	override fun loginClicked() {
		val email = view.email
		loginOrchestrator
			.login(email, view.password)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.doOnSuccess { loginOrchestrator.setLastLoggedInUser(email).await() }
			.subscribe(
				{ interactions.loggedIn(it) },
				{ Log.e("Login", "Failed", it) }
			)
	}
}

class LoginModelMapper : Func1<String?, LoginPrefilledModel> {
	override fun call(storedUserName: String?) =
		LoginPrefilledModel(
			storedUserName ?: "",
			""
		)
}

class LoginOrchestrator(
	private val login: LoggerInner
) {

	fun getLastLoggedInUser(): Single<String?> =
		Single
			.fromCallable {
				login.lastLoggedInUser
			}

	fun login(email: String, password: String): Single<UserID> =
		Single
			.fromCallable {
				login.login(email, password)
			}

	fun setLastLoggedInUser(email: String): Completable =
		Completable
			.fromAction {
				login.lastLoggedInUser = email
			}
}
