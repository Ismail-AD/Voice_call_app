package com.appdev.voicecallapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.callsync.DataModel.DataModel
import com.appdev.callsync.DataModel.IceCandidateModel
import com.appdev.voicecallapp.DataModel.UserInfo
import com.appdev.voicecallapp.ServerInteractionCode.HandleServerMessage
import com.appdev.voicecallapp.ServerInteractionCode.WebSocketClient
import com.appdev.voicecallapp.Utils.UserAdapter
import com.appdev.voicecallapp.WebRTC.CustomPeerConnectionObserver
import com.appdev.voicecallapp.WebRTC.WebRTC
import com.appdev.voicecallapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class MainActivity : AppCompatActivity(), HandleServerMessage, LifecycleObserver {

    lateinit var binding: ActivityMainBinding
    private var webRTC: WebRTC? = null
    private val gson = Gson()
    lateinit var userAdapter: UserAdapter
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance()
    var targetUser: String? = null


    var usersList: MutableList<UserInfo> = mutableListOf()
    var webSocketClient: WebSocketClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webRTC?.getState() == PeerConnection.PeerConnectionState.CONNECTING || webRTC?.getState() == PeerConnection.PeerConnectionState.CONNECTED) {
                    showEndCallConfirmationDialog()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        binding.pg.visibility = View.VISIBLE
        userAdapter = UserAdapter(usersList, ::onCallClicked)
        binding.mainRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.mainRecyclerView.adapter = userAdapter

        getUsersList()
        init()

    }

    private fun showEndCallConfirmationDialog() {
        val builder = AlertDialog.Builder(this).apply {
            setTitle("End Call")
            setMessage("Are you sure you want to end the call?")
            setPositiveButton("Yes") { dialog, _ ->
                webSocketClient?.sendMessage(
                    DataModel(
                        "end_call",
                        firebaseAuth.currentUser?.email,
                        targetUser,
                        null
                    )
                )
                handleEndCall()
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }


    fun getUsersList() {
        dbRef.reference.child("userProfiles")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (firebaseAuth.uid != null) {
                        usersList.clear()
                        snapshot.children.forEach { profileSnapshot ->
                            if (profileSnapshot.key != firebaseAuth.uid
                            ) {
                                profileSnapshot.getValue(UserInfo::class.java)?.let {
                                    usersList.add(it)
                                }
                            }
                        }
                        binding.pg.visibility = View.INVISIBLE
                        if (binding.incomingCallLayout.visibility == View.VISIBLE) {
                            binding.mainRecyclerView.visibility = View.VISIBLE
                        }
                        userAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    fun onCallClicked(userInfo: UserInfo) {
        binding.progressLayout.root.visibility = View.VISIBLE
        hideContactsInfo()
        targetUser = userInfo.email
        webSocketClient?.sendMessage(
            DataModel(
                "start_call", firebaseAuth.currentUser?.email, targetUser, null
            )
        )
    }

    private fun init() {
        webSocketClient = WebSocketClient(this)
        firebaseAuth.currentUser?.email?.let { webSocketClient?.initialization(it) }

        initializeWebRTC()

        binding.endCallButton.setOnClickListener {
            webSocketClient?.sendMessage(
                DataModel(
                    "end_call",
                    firebaseAuth.currentUser?.email,
                    targetUser,
                    null
                )
            )
            handleEndCall()
        }
        binding.backBtn.setOnClickListener {
            webSocketClient?.closeSocketConnection()
            webRTC?.closeConnection()
            firebaseAuth.signOut()
            val intent = Intent(this, Login_activity::class.java)
            startActivity(intent)
        }
    }

    private fun handleEndCall() {
        hideCallLayout()
        showContactsInfo()
        webRTC?.closeConnection()
        initializeWebRTC()
    }

    private fun initializeWebRTC() {
        if (firebaseAuth.currentUser?.email != null && webSocketClient != null) {
            webRTC = WebRTC(
                this,
                firebaseAuth.currentUser!!.email!!,
                webSocketClient!!,
                object : CustomPeerConnectionObserver() {
                    override fun onIceCandidate(p0: IceCandidate?) {
                        super.onIceCandidate(p0)
                        if (p0 != null) {
                            webRTC?.addIceCandidateToPeer(p0)
                            val candidate = hashMapOf(
                                "sdpMid" to p0.sdpMid,
                                "sdpMLineIndex" to p0.sdpMLineIndex,
                                "sdpCandidate" to p0.sdp
                            )
                            webSocketClient?.sendMessage(
                                DataModel(
                                    "ice_candidate",
                                    firebaseAuth.currentUser!!.email!!,
                                    targetUser,
                                    candidate
                                )
                            )
                        }
                    }

                })
        }
    }

    override fun newMessage(dataModel: DataModel) {
        when (dataModel.type) {
            "call_response" -> {
                if (dataModel.data == "user is not online") {
                    runOnUiThread {
                        binding.progressLayout.root.visibility = View.GONE
                        showContactsInfo()
                        Toast.makeText(this, "User is not reachable", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        binding.progressLayout.root.visibility = View.GONE
                        hideContactsInfo()
                        showCallLayout()
                        binding.callTitleTv.text = "Calling ${targetUser}...."
                    }
                    webRTC?.startLocalAudio()
                    webRTC?.call(targetUser!!)

                }
            }

            "answer_received" -> {
                if (dataModel.data == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Call was rejected by ${dataModel.name}",
                            Toast.LENGTH_LONG
                        ).show()
                        hideCallLayout()
                        showContactsInfo()
                    }
                } else {
                    val session = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        dataModel.data.toString()
                    )
                    webRTC?.onRemoteSessionReceived(session)
                    runOnUiThread {
                        binding.callTitleTv.text = "Call Connected"
                    }
                }
            }

            "offer_received" -> {
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${dataModel.name.toString()} is calling you"
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        hideContactsInfo()
                        showCallLayout()

                        webRTC?.startLocalAudio()

                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            dataModel.data.toString()
                        )
                        webRTC?.onRemoteSessionReceived(session)
                        webRTC?.answer(dataModel.name!!)
                        targetUser = dataModel.name!!
                        binding.callTitleTv.text = "Call Connected"
                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()

                        webSocketClient?.sendMessage(
                            DataModel(
                                "create_answer",
                                firebaseAuth.currentUser?.email,
                                dataModel.name,
                                null
                            )
                        )
                    }
                }
            }

            "ice_candidate" -> {
                try {
                    val receivingCandidate = gson.fromJson(
                        gson.toJson(dataModel.data),
                        IceCandidateModel::class.java
                    )
                    webRTC?.addIceCandidateToPeer(
                        IceCandidate(
                            receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                            receivingCandidate.sdpCandidate
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            "call_ended" -> {
                runOnUiThread {
                    handleEndCall()
                }
            }
        }
    }

    override fun error(error: String) {
        runOnUiThread {
            if (targetUser != null) {
                binding.progressLayout.root.visibility = View.GONE
                showContactsInfo()
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun hideContactsInfo() {
        binding.mainRecyclerView.visibility = View.INVISIBLE
        binding.backBtn.visibility = View.INVISIBLE
        binding.incomingCallLayout.visibility = View.INVISIBLE
    }


    private fun showContactsInfo() {
        binding.incomingCallLayout.visibility = View.GONE
        binding.mainRecyclerView.visibility = View.VISIBLE
        binding.backBtn.visibility = View.VISIBLE
    }

    private fun showCallLayout() {
        binding.callTitleTv.visibility = View.VISIBLE
        binding.incomingCallLayout.visibility = View.INVISIBLE
        binding.callBottom.visibility = View.VISIBLE
        binding.endCallButton.visibility = View.VISIBLE
    }

    private fun hideCallLayout() {
        binding.callTitleTv.visibility = View.INVISIBLE
        binding.callBottom.visibility = View.INVISIBLE
        binding.endCallButton.visibility = View.INVISIBLE
    }


    override fun onDestroy() {
        super.onDestroy()
        webRTC?.closeConnection()
        webSocketClient?.closeSocketConnection()
    }


}


