package com.example.tifs.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tifs.Adapters.CommentAdapter
import com.example.tifs.DataClass.Comment
import com.example.tifs.DataClass.Post
import com.example.tifs.databinding.FragmentCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommentFragment : Fragment() {

    private var _binding: FragmentCommentBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postId: String
    private lateinit var postAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCommentBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()

        // Get the post ID from the bundle arguments
        postId = arguments?.getString("postId") ?: ""

        // Load post details and comments
        loadPostDetails()
        loadComments()

        // Set up RecyclerView for comments
        postAdapter = CommentAdapter(commentList, postId)
        binding.commentsRecyclerView.adapter = postAdapter
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Handle comment submission
        binding.postCommentButton.setOnClickListener {
            postComment()
        }

        return binding.root
    }

    private fun loadPostDetails() {
        // Fetch post details using the postId
        firestore.collection("Posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Set the post details
                    val post = document.toObject(Post::class.java)
                    binding.postUserName.text = post?.userName
                    binding.postSubject.text = post?.subject
                    binding.postQuestion.text = post?.questionBody
                    Glide.with(requireContext()).load(post?.userProfilePicture).into(binding.postUserProfileImage)
                }
            }
    }

    private fun loadComments() {
        // Fetch comments for the post
        firestore.collection("Posts").document(postId).collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                commentList.clear()
                for (document in documents) {
                    try {
                        val comment = document.toObject(Comment::class.java).apply {
                            id = document.id
                        }
                        commentList.add(comment)
                    } catch (e: Exception) {
                        Log.e("CommentError", "Error deserializing comment: ${e.localizedMessage}")
                        // You can handle or log specific document IDs here if needed
                    }
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading comments: ${exception.localizedMessage}")
            }
    }


    private fun postComment() {
        val commentText = binding.commentInput.text.toString()
        val user = FirebaseAuth.getInstance().currentUser

        if (commentText.isNotEmpty() && user != null) {
            val comment = hashMapOf(
                "commentText" to commentText,
                "userId" to user.uid,
                "userName" to user.displayName,
                "userProfilePicture" to user.photoUrl.toString(),
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("Posts").document(postId).collection("Comments")
                .add(comment)
                .addOnSuccessListener {
                    binding.commentInput.text.clear()
                    loadComments() // Refresh the comments after posting
                }
        }
    }

}