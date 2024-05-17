package com.appdev.voicecallapp.ServerInteractionCode

import com.appdev.callsync.DataModel.DataModel
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class WebSocketClient(private var handleServerMessage: HandleServerMessage) {

    private var webSocket: WebSocketClient? = null
    private var userName: String? = null
    private var gson = Gson()

    fun initialization(username: String) {
        webSocket = object : WebSocketClient(URI("ws://192.168.1.248:3000")) { // the server URI to connect to

            // Called after an opening handshake has been performed and now ready to send data to the server.
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessage(DataModel("store_user", username, null, null))
            }

            // Callback for string messages received from the server
            override fun onMessage(message: String?) {
                try {
                    handleServerMessage.newMessage(gson.fromJson(message, DataModel::class.java))
                } catch (e: Exception) {
                    e.let {
                        handleServerMessage.error(it.localizedMessage)
                    }
                }
            }

            // Called after the websocket connection has been closed.
            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            // Called when errors occurs.
            override fun onError(ex: Exception?) {
                ex?.let {
                    handleServerMessage.error(it.localizedMessage)
                }
            }
        }
        webSocket?.connect()
    }

    fun closeSocketConnection(){
        webSocket?.close()
    }

    fun sendMessage(dataModel: DataModel) {
        try {
            webSocket?.send(Gson().toJson(dataModel)) // Sends text to the connected websocket server.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}