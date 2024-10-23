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

class FollowingAdapter(
    private val followingList: List<String>,
    private val onUnfollowClick: (String) -> Unit
) : RecyclerView.Adapter<FollowingAdapter.FollowingViewHolder>() {

    private val users = followingList.toMutableList()

    inner class FollowingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.usernameText)
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val unfollowButton: Button = itemView.findViewById(R.id.unfollowButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_following, parent, false)
        return FollowingViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
        val userId = users[position]

        // Fetch user details like username and profile picture (you can cache this in Firestore)
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

        holder.unfollowButton.setOnClickListener {
            onUnfollowClick(userId)
        }
    }

    override fun getItemCount(): Int = users.size

    // Remove the user from the list after unfollowing
    fun removeUser(userId: String) {
        val position = users.indexOf(userId)
        if (position >= 0) {
            users.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
