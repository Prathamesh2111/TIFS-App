package com.example.tifs.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tifs.R
import com.google.firebase.firestore.FirebaseFirestore

class FollowersAdapter(
    private val followersList: List<String>,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<FollowersAdapter.FollowersViewHolder>() {

    private val users = followersList.toMutableList()

    inner class FollowersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.usernameText)
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val removeButton: Button = itemView.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_followers, parent, false)
        return FollowersViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowersViewHolder, position: Int) {
        val userId = users[position]

        // Fetch user details like username and profile picture
        FirebaseFirestore.getInstance().collection("Users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val username = documentSnapshot.getString("fullName") ?: "Unknown"
                val profilePictureUrl = documentSnapshot.getString("profilePicture") ?: ""
                holder.username.text = username
                Glide.with(holder.itemView.context)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(holder.profileImage)
            }

        holder.removeButton.setOnClickListener {
            onRemoveClick(userId)
        }
    }

    override fun getItemCount(): Int = users.size

    // Remove the user from the list after removing from followers
    fun removeUser(userId: String) {
        val position = users.indexOf(userId)
        if (position >= 0) {
            users.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
