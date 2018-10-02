package net.twisterrob.mvx

import android.content.ComponentName
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.intending
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.matcher.IntentMatchers.anyIntent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.RootMatchers.withDecorView
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.runner.AndroidJUnit4
import net.twisterrob.mvx.mvc.LoginActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

	@Rule @JvmField val activity =
		IntentsTestRule(LoginActivity::class.java, false, false)

	@Test
	fun logsIn() {
		activity.launchActivity(null)
		onView(withId(R.id.email_edit)).perform(typeText("my@email.com"))
		onView(withId(R.id.password_edit)).perform(typeText("p433w0rd"))
		intending(anyIntent())

		onView(withId(R.id.login)).perform(click())

		intended(IntentMatchers.hasComponent(ComponentName(getTargetContext(), MainActivity::class.java)))
		onView(withText(containsString("1234")))
			.inRoot(toast())
			.check(matches(isDisplayed()))
	}

	@Test
	fun prefillsOnStartup() {
		LoggerInner(getTargetContext()).lastLoggedInUser = "stored@email.com"

		activity.launchActivity(null)

		onView(withId(R.id.email_edit)).check(matches(withText("stored@email.com")))
	}

	/**
	 * Cheap [android.widget.Toast] [Matcher].
	 */
	private fun toast() = withDecorView(not(equalTo(activity.activity.window.decorView)))
}
