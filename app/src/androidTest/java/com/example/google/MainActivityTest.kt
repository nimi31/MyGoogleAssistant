package com.example.google


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Test
    fun mainActivityTest() {
        val appCompatImageView = onView(
            allOf(
                withId(R.id.action_google_lens),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        4
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageView.perform(click())

        val actionMenuItemView = onView(
            allOf(
                withId(R.id.addImage), withContentDescription("Add Image"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.action_bar),
                        2
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView.perform(click())

        val materialTextView = onData(anything())
            .inAdapterView(
                allOf(
                    withId(com.theartofdev.edmodo.cropper.R.id.select_dialog_listview),
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.contentPanel),
                        0
                    )
                )
            )
            .atPosition(0)
        materialTextView.perform(click())

        val actionMenuItemView2 = onView(
            allOf(
                withId(com.theartofdev.edmodo.cropper.R.id.crop_image_menu_flip),
                withContentDescription("Flip"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.action_bar),
                        1
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        actionMenuItemView2.perform(click())

        val actionMenuItemView3 = onView(
            allOf(
                withId(com.theartofdev.edmodo.cropper.R.id.crop_image_menu_flip),
                withContentDescription("Flip"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.action_bar),
                        1
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        actionMenuItemView3.perform(click())

        val appCompatTextView = onView(
            allOf(
                withId(androidx.transition.R.id.title), withText("Flip vertically"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.content),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatTextView.perform(click())

        val actionMenuItemView4 = onView(
            allOf(
                withId(com.theartofdev.edmodo.cropper.R.id.crop_image_menu_crop), withText("Crop"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.action_bar),
                        1
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        actionMenuItemView4.perform(click())

        val actionMenuItemView5 = onView(
            allOf(
                withId(com.theartofdev.edmodo.cropper.R.id.crop_image_menu_rotate_right),
                withContentDescription("Rotate"),
                childAtPosition(
                    childAtPosition(
                        withId(com.theartofdev.edmodo.cropper.R.id.action_bar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView5.perform(click())

        val materialButton = onView(
            allOf(
                withId(R.id.button), withText("Search"),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton.perform(click())

        pressBack()

        pressBack()

        pressBack()

        val appCompatImageView2 = onView(
            allOf(
                withId(R.id.action_explore),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageView2.perform(click())

        pressBack()

        val appCompatImageView3 = onView(
            allOf(
                withId(R.id.action_button),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        2
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageView3.perform(click())

        val appCompatImageView4 = onView(
            allOf(
                withId(R.id.assistant_Action),
                childAtPosition(
                    allOf(
                        withId(R.id.assistantConstraintLayout),
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView4.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
