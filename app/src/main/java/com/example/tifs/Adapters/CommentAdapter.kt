package com.example.tifs.Adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tifs.DataClass.Comment
import com.example.tifs.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentAdapter(private val commentList: MutableList<Comment>, private val postId: String) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    inner class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.binding.commentText.text = comment.commentText
        holder.binding.usernameText.text = comment.userName
        Glide.with(holder.itemView.context)
            .load(comment.userProfilePicture)
            .into(holder.binding.userProfileImage)

        //Delete fucntionality
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        // Only show the delete icon if the user is the author of the comment
        if (comment.userId == currentUserId) {
            holder.binding.deleteIcon.visibility = View.VISIBLE
        } else {
            holder.binding.deleteIcon.visibility = View.GONE
        }

        // Set up delete functionality
        holder.binding.deleteIcon.setOnClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, comment, position)
        }
    }

    override fun getItemCount(): Int = commentList.size

    private fun showDeleteConfirmationDialog(context: Context, comment: Comment, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteCommentFromFirestore(comment, position)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteCommentFromFirestore(comment: Comment, position: Int) {
        val commentRef = firestore.collection("Posts")
            .document(postId)  // Assuming `postId` is passed to the adapter
            .collection("Comments")
            .document(comment.id)  // Assuming `Comment` has an `id` field for Firestore document ID

        // Delete the comment from Firestore
        commentRef.delete()
            .addOnSuccessListener {
                // Remove the comment from the list and notify the adapter
                commentList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, commentList.size)
            }
            .addOnFailureListener { e ->
                Log.e("DeleteError", "Failed to delete comment: ${e.localizedMessage}")
            }
    }
}
