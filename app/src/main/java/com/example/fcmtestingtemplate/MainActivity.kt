package com.example.fcmtestingtemplate

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fcmtestingtemplate.data.NotificationTopic
import com.example.fcmtestingtemplate.databinding.ActivityMainBinding
import com.example.fcmtestingtemplate.ui.NotificationTopicAdapter
import com.example.fcmtestingtemplate.ui.onItemClick
import com.example.fcmtestingtemplate.utils.PreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.Manifest
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

private val Context.dataStore by preferencesDataStore(name = "subscribed_topic")
private const val TAG = "MainActivity"


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val notificationTopicAdapter by lazy { NotificationTopicAdapter() }

    private var subscribedTopic: Flow<MutableList<NotificationTopic>> = emptyFlow()

    private val fcm: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    private var newTopic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchRegistrationToken()

        subscribedTopic = dataStore.data.map { preferences ->
            preferences[PreferencesKey.subscribedTopic]?.map {topic ->
                NotificationTopic(topic)
            }?.toMutableList() ?: mutableListOf()
        }

        binding.recyclerView.apply {
            adapter = notificationTopicAdapter
        }

        // listener for delete button
        notificationTopicAdapter.apply {
            onItemClick = { notificationTopic ->
                lifecycleScope.launch {
                    deleteTopic(notificationTopic.topicName)
                }
            }
        }

        // listener for input topic
        binding.etTopic.addTextChangedListener {
            newTopic = it.toString().uppercase()
        }

        // listener for button subscribe
        binding.btnSubscribe.apply {
            setOnClickListener {
                if (newTopic != "") {
                    lifecycleScope.launch {
                        addNewTopic(newTopic)
                        binding.etTopic.setText("")
                    }
                }
            }
        }

        subscribeObserver()
    }

    private fun fetchRegistrationToken() {
        fcm.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            Log.d(TAG, "token: ${task.result}")
        }
    }

    private fun subscribeObserver() {
        collectLatestLifecycleFlow(subscribedTopic) {
            Log.d(TAG, "items: $it")
            notificationTopicAdapter.submitList(it)
        }
    }


    private suspend fun deleteTopic(topic: String) {
        dataStore.edit { preferences ->
            val subscribeTopic = preferences[PreferencesKey.subscribedTopic]?.toMutableSet() ?: mutableSetOf()
            subscribeTopic.remove(topic)

            preferences[PreferencesKey.subscribedTopic] = subscribeTopic
        }

        fcm.unsubscribeFromTopic(topic)
    }

    private suspend fun addNewTopic(topic: String) {
        dataStore.edit { preferences ->
            val subscribedTopic = preferences[PreferencesKey.subscribedTopic]?.toMutableSet() ?: mutableSetOf()
            subscribedTopic.add(topic)
            preferences[PreferencesKey.subscribedTopic] = subscribedTopic
        }

        fcm.subscribeToTopic(topic)
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun <T> collectLatestLifecycleFlow(
        flow: Flow<T>,
        collect: suspend (T) -> Unit,
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collect)
            }
        }
    }
}