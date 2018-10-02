package net.twisterrob.mvx.mvc

import android.content.Intent
import android.os.Bundle
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.login_activity.view.*
import net.twisterrob.mvx.LoggerInner
import net.twisterrob.mvx.MainActivity
import net.twisterrob.mvx.R
import net.twisterrob.mvx.UserID
import net.twisterrob.mvx.trainline.LoginActivityContract
import rx.Completable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers

@UiThread
class LoginActivity : AppCompatActivity(), LoginActivityContract.View {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        // Dependency Injection
        val model = LoginModel()
        val interactor = LoginOrchestrator(LoggerInner(this))
        val view = LoginView(window.decorView, model)
        val controller = LoginController(view, model, LoginInteractions(this), LoginModelMapper(), interactor)
        controller.init()
        controller.startLoading()
    }

    override fun loggedIn(user: String) {
        Toast.makeText(this, "Logged in as $user", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@UiThread
class LoginInteractions(
        private val view: LoginActivityContract.View
) : LoginContract.Interactions {

    override fun loggedIn(userID: UserID) {
        view.loggedIn(userID.toString())
    }
}

interface LoginContract {

    interface View {
        fun modelChanged()
        fun setController(controller: Controller)
    }

    interface Controller {
        fun loginClicked(email: String, password: String)
    }

    interface ReadOnlyModel {
        val email: String
        val password: String
    }

    interface Interactions {
        fun loggedIn(userID: UserID)

    }
}

@UiThread
class LoginModel @AnyThread constructor(
        override var email: String = "",
        override var password: String = ""
) : LoginContract.ReadOnlyModel {

    fun setFrom(model: LoginContract.ReadOnlyModel) {
        email = model.email
        password = model.password
    }
}

@UiThread
class LoginView(
        private val view: View,
        private val model: LoginContract.ReadOnlyModel
) : LoginContract.View {

    private lateinit var controller: LoginContract.Controller

    init {
        view.login.setOnClickListener {
            val email = view.email_edit.text.toString()
            val password = view.password_edit.text.toString()
            controller.loginClicked(email, password)
        }
    }

    override fun setController(controller: LoginContract.Controller) {
        this.controller = controller
    }

    override fun modelChanged() {
        view.email_edit.setText(model.email)
        view.password_edit.setText(model.password)
    }
}

@UiThread
class LoginController(
        private val view: LoginContract.View,
        private val model: LoginModel,
        private val interactions: LoginContract.Interactions,
        private val mapper: LoginModelMapper,
        private val loginOrchestrator: LoginOrchestrator
) : LoginContract.Controller {

    fun init() {
        view.setController(this)
    }

    fun startLoading() {
        loginOrchestrator
                .getLastLoggedInUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(mapper)
                .subscribe(
                        { model.setFrom(it); view.modelChanged() },
                        { Log.e("Login", "Failed", it) }
                )
    }

    override fun loginClicked(email: String, password: String) {
        model.email = email
        model.password = password
        loginOrchestrator
                .login(model.email, model.password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { loginOrchestrator.setLastLoggedInUser(model.email).await() }
                .subscribe(
                        { interactions.loggedIn(it) },
                        { Log.e("Login", "Failed", it) }
                )
    }
}

class LoginModelMapper : Func1<String?, LoginModel> {
    @WorkerThread
    override fun call(storedUserName: String?) =
            LoginModel(
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
