package com.bloodconnect.notifications

import android.content.Context
import com.bloodconnect.data.model.BloodGroup
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("topic_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVED_GROUP = "saved_blood_group"
        private val ALL_TOPICS = listOf(
            "bg_A_POS", "bg_A_NEG",
            "bg_B_POS", "bg_B_NEG",
            "bg_AB_POS", "bg_AB_NEG",
            "bg_O_POS", "bg_O_NEG"
        )
    }

    fun topicFor(bloodGroup: String): String? {
        val bg = BloodGroup.fromString(bloodGroup) ?: return null
        val prefix = when (bg) {
            BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE -> "bg_A"
            BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE -> "bg_B"
            BloodGroup.AB_POSITIVE, BloodGroup.AB_NEGATIVE -> "bg_AB"
            BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE -> "bg_O"
        }
        val suffix = if (bg.displayName.endsWith("+")) "_POS" else "_NEG"
        return "${prefix}${suffix}"
    }

    suspend fun subscribe(bloodGroup: String) {
        try {
            val topic = topicFor(bloodGroup) ?: return
            val savedGroup = prefs.getString(KEY_SAVED_GROUP, null)

            if (savedGroup != bloodGroup) {
                if (savedGroup != null) {
                    val oldTopic = topicFor(savedGroup)
                    if (oldTopic != null) {
                        try {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic).await()
                        } catch (_: Exception) {}
                    }
                }
                FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
                prefs.edit().putString(KEY_SAVED_GROUP, bloodGroup).apply()
            }
        } catch (_: Exception) {
            // Swallow errors as per requirement
        }
    }

    suspend fun unsubscribeAll() {
        try {
            for (topic in ALL_TOPICS) {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
                } catch (_: Exception) {}
            }
            prefs.edit().remove(KEY_SAVED_GROUP).apply()
        } catch (_: Exception) {
            // Swallow errors as per requirement
        }
    }
}
