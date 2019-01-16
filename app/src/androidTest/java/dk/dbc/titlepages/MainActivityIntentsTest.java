package dk.dbc.titlepages;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MainActivityIntentsTest {
    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule =
        new IntentsTestRule<>(MainActivity.class);

    @Test
    public void test_onActivityResult_titlepage() {
        final Intent testIntent = new Intent();
        testIntent.putExtra(Constants.RESULT_FILENAME_KEY,
            "/data/SpongeBob-JPEGPants.gif");
        final Instrumentation.ActivityResult activityResult =
            new Instrumentation.ActivityResult(Activity.RESULT_OK, testIntent);
        intending(isInternal()).respondWith(activityResult);
        onView(withId(R.id.titlepage_picture_btn)).perform(click());

        assertThat("contains filename", intentsTestRule.getActivity()
            .resultImageFilenames.containsKey(
            Constants.TITLEPAGE_FILENAME_KEY), is(true));
        assertThat("filename", intentsTestRule.getActivity()
            .resultImageFilenames.get(Constants.TITLEPAGE_FILENAME_KEY),
            is("/data/SpongeBob-JPEGPants.gif"));
        assertThat("does not contain colophon filename", intentsTestRule
            .getActivity().resultImageFilenames.containsKey(
            Constants.COLOPHON_FILENAME_KEY), is(false));
    }

    @Test
    public void test_onActivityResult_colophon() {
        final Intent testIntent = new Intent();
        testIntent.putExtra(Constants.RESULT_FILENAME_KEY,
            "/data/SpongeBob-JPEGPants.gif");
        final Instrumentation.ActivityResult activityResult =
            new Instrumentation.ActivityResult(Activity.RESULT_OK, testIntent);
        intending(isInternal()).respondWith(activityResult);
        onView(withId(R.id.colophon_picture_btn)).perform(click());

        assertThat("contains filename", intentsTestRule.getActivity()
            .resultImageFilenames.containsKey(
                Constants.COLOPHON_FILENAME_KEY), is(true));
        assertThat("filename", intentsTestRule.getActivity()
                .resultImageFilenames.get(Constants.COLOPHON_FILENAME_KEY),
            is("/data/SpongeBob-JPEGPants.gif"));
        assertThat("does not contain titlepage filename", intentsTestRule
            .getActivity().resultImageFilenames.containsKey(
                Constants.TITLEPAGE_FILENAME_KEY), is(false));
    }

    @Test
    public void test_onActivityResult_cancelled() {
        final Intent testIntent = new Intent();
        final Instrumentation.ActivityResult activityResult =
            new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, testIntent);
        intending(isInternal()).respondWith(activityResult);
        onView(withId(R.id.colophon_picture_btn)).perform(click());

        assertThat("does not contains colophon filename", intentsTestRule
            .getActivity().resultImageFilenames.containsKey(
                Constants.COLOPHON_FILENAME_KEY), is(false));
        assertThat("does not contain titlepage filename", intentsTestRule
            .getActivity().resultImageFilenames.containsKey(
                Constants.TITLEPAGE_FILENAME_KEY), is(false));
    }

    @Test
    public void test_titlepageExtras() {
        onView(withId(R.id.book_id)).perform(clearText(), typeText("135531"));
        onView(withId(R.id.titlepage_picture_btn)).perform(click());
        intended(hasExtra(Constants.BOOK_ID_KEY, "135531"));
        intended(hasExtra(Constants.IMAGE_TYPE_KEY,
            Constants.IMAGE_TYPE_TITLEPAGE));
    }

    @Test
    public void test_colophonExtras() {
        onView(withId(R.id.book_id)).perform(clearText(), typeText("543123"));
        onView(withId(R.id.colophon_picture_btn)).perform(click());
        intended(hasExtra(Constants.BOOK_ID_KEY, "543123"));
        intended(hasExtra(Constants.IMAGE_TYPE_KEY,
            Constants.IMAGE_TYPE_COLOPHON));
    }
}
