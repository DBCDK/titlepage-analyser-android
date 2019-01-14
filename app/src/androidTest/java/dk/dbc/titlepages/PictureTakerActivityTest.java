package dk.dbc.titlepages;

import android.Manifest;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import com.android.camera.ShutterButton;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class PictureTakerActivityTest {
    // This rule doesn't seem to work since shifting to an androidx runner
    @Rule
    public GrantPermissionRule rule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA);

    @Rule
    public ActivityTestRule<PictureTakerActivity> schedulerActivityTestRule =
        new ActivityTestRule<>(PictureTakerActivity.class, false, true);

    @Test
    public void test_onCreate() {
        final IdlingRegistry idlingRegistry = IdlingRegistry.getInstance();
        idlingRegistry.register(schedulerActivityTestRule.getActivity()
            .countingIdlingResource);
        grantPermission();
        onView(withId(R.id.shutter_button)).check(matches(allOf(
            isDisplayed(), isClickable())));
        onView(withId(R.id.textureview)).check(matches(isDisplayed()));

        final ShutterButton shutterButton = schedulerActivityTestRule
            .getActivity().findViewById(R.id.shutter_button);
        schedulerActivityTestRule.getActivity().runOnUiThread(
            shutterButton::performClick);
        onView(withText("Billede er gemt")).inRoot(withDecorView(not(
            schedulerActivityTestRule.getActivity().getWindow()
            .getDecorView()))).check(matches(isDisplayed()));
    }

    // Try granting permission by clicking the dialog as a workaround for
    // the rule not working.
    private void grantPermission() {
        final UiDevice device = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation());
        final UiObject confirmButton = device.findObject(new UiSelector()
            .text("ALLOW"));
        try {
            confirmButton.click();
        } catch (UiObjectNotFoundException e) {}
    }
}
