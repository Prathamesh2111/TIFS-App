package com.example.tifs.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tifs.MainActivity
import com.example.tifs.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private  var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        binding.registerBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            // Validate inputs
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Create new user
                signUpUser(username, email, password)
            }
        }
        return binding.root
    }

    private fun signUpUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign up success, navigate to the HomeFragment
                    val user = auth.currentUser
                    user?.let { saveUserToFirestore(it, username)}
                    Toast.makeText(
                        requireContext(),
                        "Account created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate to HomeFragment or desired fragment
                    navigateToHome()
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(
                        requireContext(),
                        "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Save user data to Firestore
    private fun saveUserToFirestore(user: FirebaseUser, username: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Create a map for user data
        val userData = hashMapOf(
            "fullName" to username,
            "email" to user.email,
            "profilePicture" to user.photoUrl.toString(), // You can leave it empty or add logic for a default profile picture
            "followers" to arrayListOf<String>(),
            "following" to arrayListOf<String>()
        )

        // Add the new user to Firestore in the "Users" collection
        firestore.collection("Users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "User data successfully written!")
                Toast.makeText(requireContext(), "Account created successfully", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing user data", e)
                Toast.makeText(requireContext(), "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToHome() {
        // Navigate to MainActivity
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // Finish the current activity to remove it from the back stack
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}