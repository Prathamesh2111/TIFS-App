package com.example.tifs.Adapters

import android.os.Bundle
import android.provider.Settings.Global.putString
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
import com.example.tifs.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(private var postList: MutableList<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.questionText.text = post.questionBody
        holder.binding.categoryText.text = post.subject
        holder.binding.usernameText.text = post.userName
        // Load profile picture using Glide or any image loading library
        // Load profile picture using Glide
        Glide.with(holder.itemView.context)
            .load(post.userProfilePicture) // URL of the profile picture
            .placeholder(R.drawable.default_profile) // Default image in case of error or no image
            .into(holder.binding.userProfileImage)

        //AuthorProfileNavigation
        holder.binding.usernameText.setOnClickListener {
            val bundle = Bundle().apply {
                putString("authorId", post.userId)  // Pass the author's userId
            }
            holder.itemView.findNavController().navigate(R.id.action_homeFragment_to_authorProfileFragment, bundle)
        }

        // Set likes count
        holder.binding.likeCountText.text = post.likesCount.toString()
        // Check if the current user has liked the post
        val isLiked = post.likedBy.contains(currentUserId)
        // Set like button state
        holder.binding.likeLogo.setImageResource(if (isLiked) R.drawable.liked else R.drawable.like)
        // Set like button click listener
        holder.binding.likeLogo.setOnClickListener {
            toggleLike(post, holder)
        }

        // Set comment count
        fetchCommentCount(post.id, holder.binding.commentCountText)

        // Add click listener to the comment layout
        holder.binding.commentLayout.setOnClickListener {
            val bundle = Bundle().apply {
                putString(
                    "postId",
                    post.id
                )  // Assuming 'post.id' holds the Firestore document ID of the post
            }
            holder.itemView.findNavController()
                .navigate(R.id.action_homeFragment_to_commentFragment, bundle)
        }

        // Convert Firestore Timestamp to a readable format (time and date separately)
        post.timestamp?.let { timestamp ->
            // Format for time (hh:mm a)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(timestamp.toDate())

            // Format for date (dd MM YYYY)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(timestamp.toDate())

            // Set the timestampText to show time on the first line and date on the second line
            holder.binding.timestampText.text = "$formattedTime\n$formattedDate"
        } ?: run {
            // In case there's no timestamp, show a default value
            holder.binding.timestampText.text = "Unknown time\nUnknown date"
        }

    }

    override fun getItemCount(): Int = postList.size

    private fun toggleLike(post: Post, holder: PostViewHolder) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postId = post.id
        val postRef = firestore.collection("Posts").document(postId) // Get post ID

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikedBy = snapshot.get("likedBy") as? List<String> ?: listOf()
            val likesCount = snapshot.getLong("likesCount") ?: 0

            // Modify local post object
            if (currentLikedBy.contains(currentUserId)) {
                // Unlike the post
                val updatedLikedBy = currentLikedBy.toMutableList().apply { remove(currentUserId) }
                transaction.update(postRef, "likedBy", updatedLikedBy)
                transaction.update(postRef, "likesCount", likesCount - 1)

                // Update local post object
                post.likedBy = updatedLikedBy
                post.likesCount = (likesCount - 1).toInt()
            } else {
                // Like the post
                val updatedLikedBy = currentLikedBy.toMutableList().apply { add(currentUserId) }
                transaction.update(postRef, "likedBy", updatedLikedBy)
                transaction.update(postRef, "likesCount", likesCount + 1)

                // Update local post object
                post.likedBy = updatedLikedBy
                post.likesCount = (likesCount + 1).toInt()
            }
        }.addOnSuccessListener {
            // Notify adapter of changes
            notifyItemChanged(holder.adapterPosition) // Update this specific post's view
        }.addOnFailureListener { e ->
            Log.e("LikeError", "Failed to update likes: ${e.localizedMessage}")
        }
    }

    // Method to fetch the comment count for a specific post
    private fun fetchCommentCount(postId: String, commentCountText: TextView) {
        val commentsCollection =
            firestore.collection("Posts").document(postId).collection("Comments")
        commentsCollection.get().addOnSuccessListener { querySnapshot ->
            val commentCount = querySnapshot.size()
            commentCountText.text = commentCount.toString()
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Failed to fetch comment count: ${e.localizedMessage}")
            commentCountText.text = "0"  // Default to 0 if fetching fails
        }
    }
}
