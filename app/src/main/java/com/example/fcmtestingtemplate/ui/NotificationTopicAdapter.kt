package com.example.fcmtestingtemplate.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fcmtestingtemplate.data.NotificationTopic
import com.example.fcmtestingtemplate.databinding.ItemSubscribedTopicBinding
import com.example.fcmtestingtemplate.utils.clickWithDebounce

var onItemClick: ((NotificationTopic) -> Unit)? = null

private const val TAG = "NotificationTopicAdapter"

class NotificationTopicAdapter: ListAdapter<NotificationTopic, NotificationTopicAdapter.ViewHolder>(
    ITEM_COMPARATOR
) {
    inner class ViewHolder(val binding: ItemSubscribedTopicBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubscribedTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    @SuppressLint("LongLogTag")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.apply {
            getItem(position)?.let { notificationTopic ->
                Log.d(TAG, "onBindViewHolder: $notificationTopic")

                topicName.text = notificationTopic.topicName

                btnDelete.clickWithDebounce {
                    onItemClick?.invoke(notificationTopic)
                }
            }
        }
    }

    companion object {
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<NotificationTopic>() {
            override fun areItemsTheSame(
                oldItem: NotificationTopic,
                newItem: NotificationTopic,
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: NotificationTopic,
                newItem: NotificationTopic,
            ): Boolean = oldItem.topicName == newItem.topicName
        }
    }
}

