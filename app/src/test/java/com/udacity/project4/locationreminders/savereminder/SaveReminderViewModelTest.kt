package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(maxSdk = Build.VERSION_CODES.P)
class SaveReminderViewModelTest {

    //Executes each task synchronously using Architecture Components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //Set the main Coroutine Dispatcher for Unit Test
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Subject under Test
    private lateinit var viewModel: SaveReminderViewModel

    //Fake Repository to be injected into the ViewModel
    private lateinit var remindersRepository: FakeDataSource

//Completed: provide testing to the SaveReminderView and its live data objects
    @Before
    fun setupViewModel(){
    //Initialize the Repository
    remindersRepository = FakeDataSource()
     viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun cleanUpData() = runBlocking{
        remindersRepository.deleteAllReminders()
        stopKoin()
    }

    @Test
    fun returnError() = mainCoroutineRule.runBlockingTest{
        val reminder = ReminderDataItem("","Description", "Florida",
                0.0, 0.0)
        val dataItem = viewModel.validateEnteredData(reminder)
        assertThat(dataItem, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun onClear_nullValue(){
        viewModel.onClear()
        assertThat(viewModel.reminderTitle.getOrAwaitValue(), `is` (nullValue()))
        assertThat(viewModel.reminderDescription.getOrAwaitValue(), `is` (nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest{
        //Given a new Reminder and a fresh ViewModel
        val reminder = ReminderDataItem("Title","Description", "Florida",
                27.3, -92.8)
        remindersRepository = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
        mainCoroutineRule.pauseDispatcher()
        //When validating a saving the Reminder
        viewModel.validateAndSaveReminder(reminder)
        //Than is loading
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
    }

}