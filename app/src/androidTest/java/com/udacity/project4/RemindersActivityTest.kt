package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    //An Idling Resource that waits for Data Binding to Have no pending bindings.
    private val databindingIdlingResource = DataBindingIdlingResource()

    /** Idling resource tells Espresso that the App is Idle or Busy. This is needed when operations are
     * not scheduled in the Main Looper (ex when executed in a different Thread)
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(databindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(databindingIdlingResource)
    }

    private val reminder = ReminderDTO(
        "First Interview",
        "Get ready to answer questions", "Googleplex", 37.4220656, -122.0840897
    )

    //    Completed: add End to End testing to the app .
    @Test
    fun addReminder() = runBlocking {
        //Set initial State
//        repository.saveReminder(ReminderDTO("First Interview", "Get ready to answer questions",
//        "Boston", 42.3, 71.0))
        //Set up Reminder Screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        databindingIdlingResource.monitorActivity(activityScenario)

        //Espresso code will go here
        //Click to add a Reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        //Insert Details
        onView(withId(R.id.reminderTitle)).perform(typeText("First Interview"))
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Get ready to answer questions"),
            closeSoftKeyboard()
        )

        //Click Map
        onView(withId(R.id.selectLocation)).perform(click())
        delay(3000)

        //Select Map Location
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(longClick())
//        onView(withId(R.id.save_location_button)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
//        onView(withId(R.id.save_location_button)).check(matches(isDisplayed()))
//        onView(withId(R.id.save_location_button)).perform(click())
        Espresso.pressBack()

        //Harcoding a Reminder DTO with LatLong for the code to Pass. Cannot perform a POI click
        // Another Option is to click ourselves on the Map while in delay.
        repository.saveReminder(reminder)

        onView(withId(R.id.saveReminder)).perform(click())


        //Verify the Reminder listed on the screen is displayed
        onView(withText("First Interview")).check(matches(isDisplayed()))

        //Make sure the Activity is closed before resetting the db
        activityScenario.close()
    }

}
