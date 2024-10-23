package com.example.tifs.Adapters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tifs.DataClass.Post
import com.example.tifs.R
import com.example.tifs.databinding.ItemPostBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AuthorPostAdapter(private var postList: MutableList<Post>) : RecyclerView.Adapter<AuthorPostAdapter.AuthorPostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class AuthorPostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorPostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AuthorPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuthorPostViewHolder, position: Int) {
        val post = postList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Bind post data to views
        holder.binding.questionText.text = post.questionBody
        holder.binding.categoryText.text = post.subject
        holder.binding.usernameText.text = post.userName

        // Load profile picture using Glide
        Glide.with(holder.itemView.context)
            .load(post.userProfilePicture)
            .placeholder(R.drawable.default_profile)
            .into(holder.binding.userProfileImage)

        // Set like count
        holder.binding.likeCountText.text = post.likesCount.toString()
        val isLiked = post.likedBy.contains(currentUserId)
        holder.binding.likeLogo.setImageResource(if (isLiked) R.drawable.liked else R.drawable.like)

        // Handle like button click
        holder.binding.likeLogo.setOnClickListener {
            toggleLike(post, holder)
        }

        // Handle comment count
        fetchCommentCount(post.id, holder.binding.commentCountText)

        // Handle comment layout click
        holder.binding.commentLayout.setOnClickListener {
            val bundle = Bundle().apply {
                putString("postId", post.id)
            }
            holder.itemView.findNavController()
                .navigate(R.id.action_authorProfileFragment_to_commentFragment, bundle)
        }

        // Format and display timestamp
        post.timestamp?.let { timestamp ->
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(timestamp.toDate())
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(timestamp.toDate())

            holder.binding.timestampText.text = "$formattedTime\n$formattedDate"
        } ?: run {
            holder.binding.timestampText.text = "Unknown time\nUnknown date"
        }
    }

    override fun getItemCount(): Int = postList.size

    private fun toggleLike(post: Post, holder: AuthorPostViewHolder) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postRef = firestore.collection("Posts").document(post.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikedBy = snapshot.get("likedBy") as? List<String> ?: listOf()
            val likesCount = snapshot.getLong("likesCount") ?: 0

            if (currentLikedBy.contains(currentUserId)) {
                val updatedLikedBy = currentLikedBy.toMutableList().apply { remove(currentUserId) }
                transaction.update(postRef, "likedBy", updatedLikedBy)
                transaction.update(postRef, "likesCount", likesCount - 1)
                post.likedBy = updatedLikedBy
                post.likesCount = (likesCount - 1).toInt()
            } else {
                val updatedLikedBy = currentLikedBy.toMutableList().apply { add(currentUserId) }
                transaction.update(postRef, "likedBy", updatedLikedBy)
                transaction.update(postRef, "likesCount", likesCount + 1)
                post.likedBy = updatedLikedBy
                post.likesCount = (likesCount + 1).toInt()
            }
        }.addOnSuccessListener {
            notifyItemChanged(holder.adapterPosition)
        }.addOnFailureListener { e ->
            Log.e("LikeError", "Failed to update likes: ${e.localizedMessage}")
        }
    }

    private fun fetchCommentCount(postId: String, commentCountText: TextView) {
        firestore.collection("Posts").document(postId).collection("Comments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                commentCountText.text = querySnapshot.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to fetch comment count: ${e.localizedMessage}")
                commentCountText.text = "0"
            }
    }

}
