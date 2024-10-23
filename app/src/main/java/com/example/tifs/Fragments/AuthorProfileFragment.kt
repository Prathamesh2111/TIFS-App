package com.example.tifs.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tifs.Adapters.AuthorPostAdapter
import com.example.tifs.Adapters.PostAdapter
import com.example.tifs.DataClass.Post
import com.example.tifs.R
import com.example.tifs.databinding.FragmentAuthorProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AuthorProfileFragment : Fragment() {

    private var _binding: FragmentAuthorProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var authorPostAdapter: AuthorPostAdapter
    private var postList: MutableList<Post> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAuthorProfileBinding.inflate(inflater, container, false)

        // Setup RecyclerView
        authorPostAdapter = AuthorPostAdapter(postList)
        binding.authorPostsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.authorPostsRecyclerView.adapter = authorPostAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the authorId from arguments
        val authorId = arguments?.getString("authorId") ?: return

        binding.progressBar.visibility = View.VISIBLE

        // Setup the author profile
        loadAuthorProfile(authorId)

        // Load author's posts
        loadAuthorPosts(authorId)

        // Listen to real-time updates for the author's followers list
        listenToAuthorUpdates(authorId)

        // Handle follow/unfollow button click
        binding.followButton.setOnClickListener {
            val isFollowing = binding.followButton.text == "Unfollow"
            onFollowButtonClick(isFollowing, authorId)
        }
    }

    private fun loadAuthorPosts(authorId: String) {
        firestore.collection("Posts")
            .whereEqualTo("userId", authorId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    postList.clear()
                    for (doc in documents) {
                        val post = doc.toObject(Post::class.java).copy(id = doc.id)
                        postList.add(post)
                    }
                    authorPostAdapter.notifyDataSetChanged()
                }
                binding.progressBar.visibility = View.GONE

            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading author's posts: ${exception.localizedMessage}", exception)
                Toast.makeText(requireContext(), "Failed to load author's posts", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    // Load the author's profile (e.g., name, profile picture, etc.)
    private fun loadAuthorProfile(authorId: String) {
        val authorRef = firestore.collection("Users").document(authorId)

        authorRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val authorName = documentSnapshot.getString("fullName") ?: "Unknown"
                val profilePictureUrl = documentSnapshot.getString("profilePicture") ?: ""

                // Set author name and load profile picture
                binding.authorUsername.text = authorName
                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(binding.authorProfileImage)
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error fetching author profile: ${e.localizedMessage}")
        }
    }

    // Optimistically update UI on follow/unfollow click
    private fun onFollowButtonClick(isFollowing: Boolean, authorId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = firestore.collection("Users").document(currentUserId)
        val authorRef = firestore.collection("Users").document(authorId)

        // Optimistically update the UI
        if (isFollowing) {
            binding.followButton.text = "Follow"
            binding.followersCount.text = (binding.followersCount.text.toString().toInt() - 1).toString()
        } else {
            binding.followButton.text = "Unfollow"
            binding.followersCount.text = (binding.followersCount.text.toString().toInt() + 1).toString()
        }

        // Update Firestore using a batch
        firestore.runBatch { batch ->
            if (isFollowing) {
                batch.update(userRef, "following", FieldValue.arrayRemove(authorId))
                batch.update(authorRef, "followers", FieldValue.arrayRemove(currentUserId))
            } else {
                batch.update(userRef, "following", FieldValue.arrayUnion(authorId))
                batch.update(authorRef, "followers", FieldValue.arrayUnion(currentUserId))
            }
        }.addOnSuccessListener {
            Log.d("FollowUpdate", "Follow status updated successfully.")
        }.addOnFailureListener { e ->
            Log.e("FollowError", "Failed to update follow status: ${e.localizedMessage}")
            // Revert optimistic update on failure
            if (isFollowing) {
                binding.followButton.text = "Unfollow"
                binding.followersCount.text = (binding.followersCount.text.toString().toInt() + 1).toString()
            } else {
                binding.followButton.text = "Follow"
                binding.followersCount.text = (binding.followersCount.text.toString().toInt() - 1).toString()
            }
        }
    }

    private fun listenToAuthorUpdates(authorId: String) {
        val authorRef = firestore.collection("Users").document(authorId)
        // Add snapshot listener for real-time updates
        authorRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ListenFailed", "Listen failed: ${e.localizedMessage}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Make sure the binding is not null and the view is attached
                if (isAdded && _binding != null) {
                    // Get followers list
                    val followers = snapshot.get("followers") as? List<String> ?: listOf()
                    binding.followersCount.text = followers.size.toString()

                    val following = snapshot.get("following") as? List<String> ?: listOf()
                    binding.followingCount.text = following.size.toString()

                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    // Check if the current user is following the author
                    val isFollowing = followers.contains(currentUserId)

                    // Update the follow button text
                    binding.followButton.text = if (isFollowing) "Unfollow" else "Follow"
                }
            } else {
                Log.d("SnapshotNull", "Current data: null")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
