package dk.dbc.titlepages;

import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> schedulerActivityTestRule =
        new ActivityTestRule<>(MainActivity.class, false, true);

    @Test
    public void test_onCreate() {
        onView(withId(R.id.book_id))
            .perform(clearText(), typeText("123"));
        final EditText bookIdEditText = schedulerActivityTestRule
            .getActivity().findViewById(R.id.book_id);
        assertThat(bookIdEditText.getText().toString(), is("123"));

        onView(withId(R.id.titlepage_picture_btn)).check(matches(allOf(
            isDisplayed(), isClickable())));
        onView(withId(R.id.colophon_picture_btn)).check(matches(allOf(
            isDisplayed(), isClickable())));
        onView(withId(R.id.send)).check(matches(allOf(
            isDisplayed(), isClickable())));
    }

    @Test
    public void test_bookIdField_onlyNumbers() {
        onView(withId(R.id.book_id))
            .perform(clearText(), typeText("Barnacles"));
        final EditText bookIdEditText = schedulerActivityTestRule
            .getActivity().findViewById(R.id.book_id);
        // assert that none of the non-number characters were put in
        assertThat(bookIdEditText.getText().toString(), is(""));
    }
}
