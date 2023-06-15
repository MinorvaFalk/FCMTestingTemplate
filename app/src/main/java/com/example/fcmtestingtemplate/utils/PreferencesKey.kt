package com.example.fcmtestingtemplate.utils

import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKey {
    val subscribedTopic = stringSetPreferencesKey("subscribed_topic")
}