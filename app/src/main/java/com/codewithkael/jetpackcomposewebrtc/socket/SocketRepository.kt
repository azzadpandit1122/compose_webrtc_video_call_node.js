package com.codewithkael.jetpackcomposewebrtc.socket

import android.util.Log
import com.codewithkael.jetpackcomposewebrtc.models.MessageModel
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketRepository @Inject constructor() {
    private var webSocket: WebSocketClient? = null
    private var userName: String? = null
    private val TAG = "SocketRepository"
    private val gson = Gson()

    fun initSocket(username: String, messageInterface: NewMessageInterface) {
        userName = username
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address, use cmd and then write ipconfig
        // and get your ethernet ipv4 , mine is : "ws://192.168.1.3:3000"
        //but if your websocket is deployed you add your websocket address here

//        webSocket = object : WebSocketClient(URI("ws://10.0.2.2:3000")) { //Emulator or Emulator
//        webSocket = object : WebSocketClient(URI("ws://192.168.1.7:3000")) {
        webSocket = object : WebSocketClient(URI("ws://192.168.189.15:3000")) {  //Emulator and Phone
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(
                    MessageModel(
                        "store_user", username, null, null
                    )
                )
            }

            override fun onMessage(message: String?) {
                try {
                    Log.e(TAG, "onMessage: "+message )
                    messageInterface.onNewMessage(gson.fromJson(message, MessageModel::class.java))

                } catch (e: Exception) {
                    Log.e(TAG, "onMessage: "+e.printStackTrace() )
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "onClose: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG, "onError: $ex")
            }

        }
        webSocket?.connect()

    }

    fun sendMessageToSocket(message: MessageModel) {
        try {
            webSocket?.send(Gson().toJson(message))
            Log.e(TAG, "sendMessageToSocket: "+message )
        } catch (e: Exception) {
            Log.d(TAG, "sendMessageToSocket: $e")
        }
    }
}