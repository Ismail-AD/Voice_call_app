package com.appdev.voicecallapp.DataModel

data class UserInfo(
    var userName: String = "",
    var password: String = "",
    var email: String = ""
){
    constructor() : this("", "", "")
}