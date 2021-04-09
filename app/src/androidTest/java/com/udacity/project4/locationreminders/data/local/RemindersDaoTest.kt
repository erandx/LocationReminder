package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    Completed: Add testing implementation to the RemindersDao.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb(){
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminder_getById() = runBlockingTest{
        //GIVEN insert a Reminder
        val reminder = ReminderDTO("Title", "Desc", "FL",
                0.0, 0.0)
        database.reminderDao().saveReminder(reminder)

        //WHEN get a Reminder ID
        val loadedReminder = database.reminderDao().getReminderById(reminder.id)

        //THAN the loaded data contains the expected values
        assertThat<ReminderDTO>(loadedReminder as ReminderDTO, notNullValue())
        assertThat(loadedReminder.title, `is`(reminder.title))
        assertThat(loadedReminder.description, `is`(reminder.description))
        assertThat(loadedReminder.location, `is`(reminder.location))
        assertThat(loadedReminder.latitude, `is`(reminder.latitude))
        assertThat(loadedReminder.longitude, `is`(reminder.longitude))
    }

    @Test
    fun insertReminder_deleteAll() = runBlockingTest{
        //GIVEN a saved reminder
        val reminder = ReminderDTO("Interview", "First Interview", "USA", 37.0, 95.7)
        database.reminderDao().saveReminder(reminder)

        //WHEN we delete Reminders and check our database
        database.reminderDao().deleteAllReminders()
        val reminders = database.reminderDao().getReminders()

        //THAN assertThat the reminders list is empty
        assertThat(reminders, `is`(emptyList()))


    }

}