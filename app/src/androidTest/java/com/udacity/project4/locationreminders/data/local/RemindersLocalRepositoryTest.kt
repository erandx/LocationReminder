package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainAndroidCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //    Completed: Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase
    private lateinit var reminderDto: RemindersDao

    //Executes each task synchronously using Architecture Components into a repeatable order.
//We should include this Rule every time we Test LiveData
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainAndroidCoroutineRule()

    @Before
    fun setup() {
        //Using an in-memory database for testing, because it doesn't survive the process.
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        )
                .allowMainThreadQueries()
                .build()
        reminderDto = database.reminderDao()

        remindersLocalRepository = RemindersLocalRepository(
                reminderDto,
                Dispatchers.Main
        )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    // runBlocking is used here because of https://github.com/Kotlin/kotlinx.coroutines/issues/1204
    // Replace with runBlockingTest once issue is resolved
    @Test
    fun saveReminders_getReminders() = runBlocking {
        //GIVEN a new Reminder saved in the database
        val newReminder = ReminderDTO("Title", "Description", "California",
                36.7, 119.4)
        remindersLocalRepository.saveReminder(newReminder)

        //WHEN Reminder retrieved by ID
        val result = remindersLocalRepository.getReminder(newReminder.id) as Result.Success<ReminderDTO>
        val loaded = result.data

        //THAN some reminders are returned.
        assertThat(loaded, `is`(notNullValue()))
        assertThat(result.data.title, `is`("Title"))
        assertThat(result.data.description, `is`("Description"))
        assertThat(result.data.location, `is`("California"))
        assertThat(result.data.latitude, `is`(36.7))
        assertThat(result.data.longitude, `is`(119.4))
    }

    @Test
    fun deleteReminder_returnEmpty() = runBlocking{
        val reminder = ReminderDTO("New", "Item", "USA",
                37.0, 95.7)
        remindersLocalRepository.saveReminder(reminder)

        remindersLocalRepository.deleteAllReminders()

        val result = remindersLocalRepository.getReminders()
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun saveReminder_returnList() = runBlocking {
        //GIVEN a new Reminder saved in database
        val reminder = ReminderDTO("Hello World", "First App", "USA",
                37.0, 95.7)
        remindersLocalRepository.saveReminder(reminder)

        //WHEN Reminder retrieved by ID
        val result = remindersLocalRepository.getReminders()
        //THAN the Reminder list is returned
        result as Result.Success
        assertThat(result.data.size, equalTo(1))
        assertThat(result.data, hasItem(reminder))
        assertThat(result.data, notNullValue())
    }

}