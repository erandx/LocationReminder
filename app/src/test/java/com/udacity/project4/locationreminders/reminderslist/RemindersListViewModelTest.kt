package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(maxSdk = Build.VERSION_CODES.P)
class RemindersListViewModelTest {

    //Completed: provide testing to the RemindersListViewModel and its live data objects

    //Executes each task synchronously using Architecture Components into a repeatable order.
    //We should include this Rule every time we Test LiveDataa
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Subject under Test
    private lateinit var viewModel: RemindersListViewModel

    //Fake Repository to be injected into the viewModel
    private lateinit var remindersRepository: FakeDataSource

    @Before
    fun setupViewModel() {
        //Initialize the fake Repository
        remindersRepository = FakeDataSource()
        viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun cleanUpData() = runBlocking {
        remindersRepository.deleteAllReminders()
        stopKoin()
    }

    @Test
    fun loadReminder_loadingReminders() {
        //GIVEN - we are loading reminders
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()

        //WHEN - the Dispatcher is paused, showLoading is true
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        //THAT - the Dispatcher is resumed, showLoading is false
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))

    }

    @Test
    fun loadReminders_deleteAllReminders() {
        viewModel.deleteReminders()
        val remindersRepository = viewModel.remindersList.getOrAwaitValue()

        assertThat(remindersRepository, hasSize(0))
    }
}