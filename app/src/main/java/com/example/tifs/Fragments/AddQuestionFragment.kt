package com.example.tifs.Fragments

import android.R
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.tifs.databinding.FragmentAddQuestionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddQuestionFragment : Fragment() {

    private var _binding: FragmentAddQuestionBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddQuestionBinding.inflate(inflater, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val categories = arrayOf("Applying to Universities", "University Confusion", "Admission Counselling","Visa and Immigration", "Financial Aid", "Course Selection", "Internships and Jobs")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter

        // Handle question submission
        binding.submitQuestionBtn.setOnClickListener {
            submitQuestion()
        }

        return binding.root
    }

        private fun submitQuestion() {
            val questionBody = binding.questionInput.text.toString()
            val subject = binding.categorySpinner.selectedItem.toString()
            val user = auth.currentUser

            if (questionBody.isNotEmpty() && user != null) {
                val post = hashMapOf(
                    "questionBody" to questionBody,
                    "subject" to subject,
                    "userName" to user.displayName,
                    "userId" to user.uid,
                    "userProfilePicture" to user.photoUrl.toString(),
                    "likes" to 0,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                firestore.collection("Posts").add(post)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Question submitted!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()  // Navigate back to HomeFragment
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to submit question.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter a question", Toast.LENGTH_SHORT).show()
            }
        }
}