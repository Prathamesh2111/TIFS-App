package com.example.tifs.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tifs.Adapters.FollowingAdapter
import com.example.tifs.DataClass.User
import com.example.tifs.R
import com.example.tifs.databinding.FragmentFollowingBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FollowingFragment : Fragment() {

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: FollowingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Load the list of users the current user is following
        loadFollowingList(currentUserId)
    }

    private fun loadFollowingList(currentUserId: String) {
        val userRef = firestore.collection("Users").document(currentUserId)

        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val followingList = documentSnapshot.get("following") as? List<String> ?: emptyList()

                // Set up the RecyclerView with the list of followed users
                setupRecyclerView(followingList)
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error fetching following list: ${e.localizedMessage}")
        }
    }

    private fun setupRecyclerView(followingList: List<String>) {
        adapter = FollowingAdapter(followingList) { userId ->
            onUnfollowButtonClick(userId)
        }
        binding.followingRv.layoutManager = LinearLayoutManager(requireContext())
        binding.followingRv.adapter = adapter
    }

    // Handle unfollow button click
    private fun onUnfollowButtonClick(followedUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserRef = firestore.collection("Users").document(currentUserId)
        val followedUserRef = firestore.collection("Users").document(followedUserId)

        // Update Firestore using a batch to remove following/follower relationship
        firestore.runBatch { batch ->
            batch.update(currentUserRef, "following", FieldValue.arrayRemove(followedUserId))
            batch.update(followedUserRef, "followers", FieldValue.arrayRemove(currentUserId))
        }.addOnSuccessListener {
            Log.d("Unfollow", "Successfully unfollowed user: $followedUserId")
            adapter.removeUser(followedUserId)
        }.addOnFailureListener { e ->
            Log.e("UnfollowError", "Error unfollowing user: ${e.localizedMessage}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



