package com.appdev.callsync.DataModel

data class DataModel(
    val type: String,
    val name: String? = null,
    val target: String? = null,
    val data: Any? = null
)
