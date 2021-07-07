package com.test.recordlife.models

import com.google.firebase.database.IgnoreExtraProperties

// [START LandmarkRecord_class]
@IgnoreExtraProperties
data class LandmarkRecord(
    var userName: String? = "",
    var userId: String? = "",
    var text: String? = "",
    var time: Long? = 0,
    var location: String? = ""
)
// [END LandmarkRecord_class]