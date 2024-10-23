package com.example.tifs.Adapters

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tifs.DataClass.Post
import com.example.tifs.R
import com.example.tifs.databinding.ItemPostProfileBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserPostAdapter(private val context: Context, private var postList: MutableList<Post>) : RecyclerView.Adapter<UserPostAdapter.UserPostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class UserPostViewHolder(val binding: ItemPostProfileBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
        val binding = ItemPostProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
        val post = postList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.questionText.text = post.questionBody
        holder.binding.categoryText.text = post.subject
        holder.binding.usernameText.text = post.userName

        Glide.with(holder.itemView.context)
            .load(post.userProfilePicture)
            .placeholder(R.drawable.default_profile)
            .into(holder.binding.userProfileImage)

        holder.binding.likeCountText.text = post.likesCount.toString()
        val isLiked = post.likedBy.contains(currentUserId)
        holder.binding.likeLogo.setImageResource(if (isLiked) R.drawable.liked else R.drawable.like)

        holder.binding.likeLogo.setOnClickListener {
            toggleLike(post, holder)
        }

        // Handle comment layout click for navigation
        holder.binding.commentLayout.setOnClickListener {
            val bundle = Bundle().apply {
                putString("postId", post.id)  // Pass the post ID to the comment fragment
            }
            holder.itemView.findNavController()
                .navigate(R.id.action_profileFragment_to_commentFragment, bundle) // Update the action ID as per your navigation graph
        }

        if (post.userId == currentUserId) {
            holder.binding.deleteButton.visibility = View.VISIBLE
            holder.binding.deleteButton.setOnClickListener {
                showDeleteConfirmation(post.id)
            }
        } else {
            holder.binding.deleteButton.visibility = View.GONE
        }

        fetchCommentCount(post.id, holder.binding.commentCountText)
    }

    override fun getItemCount(): Int = postList.size

    private fun toggleLike(post: Post, holder: UserPostViewHolder) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postRef = firestore.collection("Posts").document(post.id)

        // Run a transaction to ensure data consistency
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            // Retrieve the likedBy list and the current likes count
            val currentLikedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
            val likesCount = snapshot.getLong("likesCount")?.toInt() ?: 0

            // Check if the current user has already liked the post
            if (currentLikedBy.contains(currentUserId)) {
                // If the user has already liked, remove the like
                currentLikedBy.remove(currentUserId)
                transaction.update(
                    postRef, mapOf(
                        "likedBy" to currentLikedBy,
                        "likesCount" to likesCount - 1
                    )
                )

                // Update the local post object
                post.likedBy = currentLikedBy
                post.likesCount = likesCount - 1
            } else {
                // If the user hasn't liked the post, add the like
                currentLikedBy.add(currentUserId)
                transaction.update(
                    postRef, mapOf(
                        "likedBy" to currentLikedBy,
                        "likesCount" to likesCount + 1
                    )
                )

                // Update the local post object
                post.likedBy = currentLikedBy
                post.likesCount = likesCount + 1
            }
        }.addOnSuccessListener {
            // Notify the adapter about the changes
            notifyItemChanged(holder.adapterPosition)
        }.addOnFailureListener { e ->
            Log.e("LikeError", "Failed to update likes: ${e.localizedMessage}")
        }
    }

    private fun fetchCommentCount(postId: String, commentTextView: TextView) {
        firestore.collection("Posts").document(postId)
            .collection("Comments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val commentCount = querySnapshot.size()
                commentTextView.text = commentCount.toString()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching comments count: ${e.localizedMessage}")
            }
    }

    private fun showDeleteConfirmation(postId: String) {
        // Show a confirmation dialog for deletion
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { dialog, _ ->
                deletePost(postId)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deletePost(postId: String) {
        val postRef = firestore.collection("Posts").document(postId)
        postRef.delete()
            .addOnSuccessListener {
                Log.d("DeletePost", "Post deleted successfully.")
                // Optionally, remove the post from the list and notify the adapter
                postList.removeIf { post -> post.id == postId }
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("DeleteError", "Failed to delete post: ${e.localizedMessage}")
            }
    }
}