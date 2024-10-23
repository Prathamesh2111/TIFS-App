package com.example.tifs.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tifs.Adapters.PostAdapter
import com.example.tifs.DataClass.Post
import com.example.tifs.LoginActivity
import com.example.tifs.R
import com.example.tifs.databinding.FragmentHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: PostAdapter
    private var postList: MutableList<Post> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()

        binding.addQuestion.bringToFront()

        binding.addQuestion.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addQuestionFragment)
        }

        //Set up RecyclerView
        adapter = PostAdapter(postList)
        binding.questionsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.questionsRv.adapter = adapter

        loadPosts()

        return binding.root
    }

    // Use coroutines to load posts from Firestore
    private fun loadPosts() {

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("Posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    postList.clear() // Clear the list before adding new data
                    for (doc in documents) {
                        val post = doc.toObject(Post::class.java).copy(id = doc.id)
                        postList.add(post)
                    }
                    adapter.notifyDataSetChanged()
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                // Log the exception details for further inspection
                Log.e("Firestore", "Error loading posts: ${exception.localizedMessage}", exception)
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()

                binding.progressBar.visibility = View.GONE
            }
    }


    // Clean up binding when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}