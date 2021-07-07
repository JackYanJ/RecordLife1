package com.test.recordlife.models

import com.google.firebase.database.IgnoreExtraProperties

// [START UserRecord_class]
@IgnoreExtraProperties
data class UserRecord(
    var text: String? = "",
    var userName: String? = "",
    var time: Long? = 0,
    var latitude: Double? = 0.0,
    var longtitude: Double? = 0.0,
    var location: String? = "",
)
// [END UserRecord_class]