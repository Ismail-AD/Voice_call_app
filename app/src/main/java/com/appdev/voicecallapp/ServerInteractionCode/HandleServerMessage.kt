package com.appdev.voicecallapp.ServerInteractionCode

import com.appdev.callsync.DataModel.DataModel

interface HandleServerMessage {
    fun newMessage(dataModel: DataModel)
    fun error(error:String)
}