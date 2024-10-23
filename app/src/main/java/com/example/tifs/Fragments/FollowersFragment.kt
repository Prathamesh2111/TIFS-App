package com.example.tifs.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tifs.Adapters.FollowersAdapter
import com.example.tifs.databinding.FragmentFollowersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FollowersFragment : Fragment() {

    private var _binding: FragmentFollowersBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: FollowersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Load the list of followers for the current user
        loadFollowersList(currentUserId)
    }

    private fun loadFollowersList(currentUserId: String) {
        val userRef = firestore.collection("Users").document(currentUserId)

        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val followersList = documentSnapshot.get("followers") as? List<String> ?: emptyList()

                // Set up the RecyclerView with the list of followers
                setupRecyclerView(followersList)
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error fetching followers list: ${e.localizedMessage}")
        }
    }

    private fun setupRecyclerView(followersList: List<String>) {
        adapter = FollowersAdapter(followersList) { userId ->
            onRemoveButtonClick(userId)
        }
        binding.followersRv.layoutManager = LinearLayoutManager(requireContext())
        binding.followersRv.adapter = adapter
    }

    // Handle remove button click
    private fun onRemoveButtonClick(followerUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserRef = firestore.collection("Users").document(currentUserId)
        val followerUserRef = firestore.collection("Users").document(followerUserId)

        // Update Firestore using a batch to remove follower/following relationship
        firestore.runBatch { batch ->
            batch.update(currentUserRef, "followers", FieldValue.arrayRemove(followerUserId))
            batch.update(followerUserRef, "following", FieldValue.arrayRemove(currentUserId))
        }.addOnSuccessListener {
            Log.d("RemoveFollower", "Successfully removed follower: $followerUserId")
            adapter.removeUser(followerUserId)
        }.addOnFailureListener { e ->
            Log.e("RemoveFollowerError", "Error removing follower: ${e.localizedMessage}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
