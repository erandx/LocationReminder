package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepositoryTest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: ReminderDataSource
    private lateinit var appContext: Application

//    TODO: add testing for the error messages.

    /**
     * Koin is used as a ServiceLocator in our case, and we'll use it to test our code.
     */
    //Reminder to be used in our Testing
    private val reminder = ReminderDTO("Title", "Description", "California",
            36.7, 119.4)

    @Before
    fun initRepository() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }

            single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }

            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }
        //New Koin Module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }

        //Get our repository
        remindersRepository = get()

        runBlocking { remindersRepository.deleteAllReminders() }
    }

    @After
    fun cleanUpData() = runBlocking {
        stopKoin()
    }

    @Test
    fun activeReminder_DisplayedInUi() {
        runBlocking {
            // GIVEN - Reminder displays in Fragment Activity
            val activityScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

            //THAN -
            activityScenario.onFragment { }

            onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
            onView(withText(appContext.getString(R.string.no_data))).check(matches(isDisplayed()))
            delay(2000)
        }
    }

    @Test
    fun FABNavigatesToSaveReminderFragment() {
        //GIVEN on the Home Screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        //WHEN clicked on the + Button
        onView(withId(R.id.addReminderFAB)).perform(click())

        //THAN navigate to the Add Reminder Screen
        verify(navController).navigate(
                ReminderListFragmentDirections.toSaveReminder())

    }

    @Test
    fun recyclerViewShowsData() {
        runBlocking {

            remindersRepository.saveReminder(reminder)
            //When Reminder List is Displayed
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

            //THAN - check
            onView(withText(reminder.title)).check(matches(isDisplayed()))
            onView(withText(reminder.description)).check(matches(isDisplayed()))
            onView(withText(reminder.location)).check(matches(isDisplayed()))

        }
    }
}