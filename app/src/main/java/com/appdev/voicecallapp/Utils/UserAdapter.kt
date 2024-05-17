package com.appdev.voicecallapp.Utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appdev.voicecallapp.DataModel.UserInfo
import com.appdev.voicecallapp.databinding.UserViewLayoutBinding

class UserAdapter(var userList: List<UserInfo>, var callIconClicked: (UserInfo) -> Unit = {}) :
    RecyclerView.Adapter<UserAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: UserViewLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserInfo) {
            binding.usernameTv.text = user.userName
            binding.usermailTV.text = user.email
            binding.audioCallBtn.setOnClickListener {
                callIconClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            UserViewLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(userList[position])
    }
}