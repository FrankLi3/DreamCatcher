package com.example.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Repository(private val userDao: UserDao, private val dreamDao: DreamDao) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val allUsers: LiveData<List<User>> = userDao.getAllUsers()
    val searchUserResults = MutableLiveData<User?>()

    val allDreams: LiveData<List<Dream>> = dreamDao.getAllDreams()
    val searchDreamResults = MutableLiveData<List<Dream>>()

    fun insertUser(user: User) {
        coroutineScope.launch(Dispatchers.IO) {
            userDao.insertUser(user)
        }
    }

    fun deleteUser(user: User) {
        coroutineScope.launch(Dispatchers.IO) {
            userDao.deleteUser(user)
        }
    }

    suspend fun getUserByIdSync(userId: String): User? {
        return userDao.getUserById(userId)
    }
    fun findUserByEmail(email: String) {
        coroutineScope.launch(Dispatchers.Main) {
            searchUserResults.value = asyncFindUser(email).await()
        }
    }

    private fun asyncFindUser(email: String) = coroutineScope.async(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    fun insertDream(dream: Dream) {
        coroutineScope.launch(Dispatchers.IO) {
            dreamDao.insertDream(dream)
        }
    }

    fun deleteDream(dream: Dream) {
        coroutineScope.launch(Dispatchers.IO) {
            dreamDao.deleteDream(dream)
        }
    }

    fun getDreamsByUserAndDate(userId: Int, date: String): LiveData<List<Dream>> {
        return dreamDao.getDreamsByUserAndDate(userId, date)
    }

    fun findDreamsByDate(date: String) {
        coroutineScope.launch(Dispatchers.Main) {
            searchDreamResults.value = asyncFindDreamsByDate(date).await() ?: emptyList()
        }
    }

    private fun asyncFindDreamsByDate(date: String) = coroutineScope.async(Dispatchers.IO) {
        dreamDao.getDreamsByDate(date)
    }

    suspend fun getUserByEmailSync(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    fun getDreamsByUserId(userId: Int): LiveData<List<Dream>> {
        return dreamDao.getDreamsByUserId(userId)
    }

    fun getUserByEmail(email: String): User? {
        return runBlocking {
            userDao.getUserByEmail(email)
        }
    }


}
