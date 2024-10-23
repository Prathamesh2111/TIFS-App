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
import com.example.tifs.Adapters.UserPostAdapter
import com.example.tifs.DataClass.Post
import com.example.tifs.R
import com.example.tifs.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var userPostAdapter: UserPostAdapter
    private var postList: MutableList<Post> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Setup RecyclerView
        userPostAdapter = UserPostAdapter(requireContext(),postList)
        binding.userPostsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userPostsRecyclerView.adapter = userPostAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileProgressBar.visibility = View.VISIBLE

        // Get the current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Load current user's profile
        loadUserProfile(currentUserId)

        // Load current user's posts
        loadUserPosts(currentUserId)

        // Listen to real-time updates for the user's followers and following lists
        listenToUserUpdates(currentUserId)
    }

    private fun loadUserPosts(userId: String) {
        firestore.collection("Posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    postList.clear()
                    for (doc in documents) {
                        val post = doc.toObject(Post::class.java).copy(id = doc.id)
                        postList.add(post)
                    }
                    userPostAdapter.notifyDataSetChanged()
                }
                binding.profileProgressBar.visibility = View.GONE

            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading user's posts: ${exception.localizedMessage}", exception)
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                binding.profileProgressBar.visibility = View.GONE
            }
    }

    private fun loadUserProfile(userId: String) {
        val userRef = firestore.collection("Users").document(userId)

        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userName = documentSnapshot.getString("fullName") ?: "Unknown"
                val profilePictureUrl = documentSnapshot.getString("profilePicture") ?: ""

                // Set username and load profile picture
                binding.profileUsername.text = userName
                Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(binding.userProfileImage)
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error fetching user profile: ${e.localizedMessage}")
        }
    }

    private fun listenToUserUpdates(userId: String) {
        val userRef = firestore.collection("Users").document(userId)

        // Add snapshot listener for real-time updates
        userRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ListenFailed", "Listen failed: ${e.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                // Get followers list
                val followers = snapshot.get("followers") as? List<String> ?: listOf()
                binding.followersCount.text = followers.size.toString()

                val following = snapshot.get("following") as? List<String> ?: listOf()
                binding.followingCount.text = following.size.toString()
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