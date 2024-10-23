package com.example.tifs.Fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tifs.MainActivity
import com.example.tifs.R
import com.example.tifs.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private  var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private val RC_SIGN_IN = 9001

    // Constant for delay time
    private val DIM_DELAY = 100L // in milliseconds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        // Check if the user is already logged in

        firebaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Ensure this matches the web client ID in Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Set up button click listeners
        binding.googlebtn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                signInWithGoogle() // Start sign-in after signing out
            }
            applyDimEffect(binding.googlebtn)
        }

        binding.facebookbtn.setOnClickListener {
            applyDimEffect(binding.facebookbtn)
        }

        binding.loginBtn.setOnClickListener {
            applyDimEffect(binding.loginBtn)

            // Get email and password from EditText fields
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                // Prompt the user to fill in both fields
                Toast.makeText(
                    requireContext(),
                    "Please enter email and password.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Try to sign in the user with Firebase Auth
                loginUser(email, password)
            }
        }

        binding.signUpbtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }



        return binding.root
    }

    // Function to apply the dim effect and reset after delay
    private fun applyDimEffect(button: ImageView) {
        button.apply {
            setColorFilter(Color.argb(150, 155, 155, 155)) // Dim effect
            Handler(Looper.getMainLooper()).postDelayed({
                clearColorFilter() // Reset color filter after delay
            }, DIM_DELAY)
        }
    }

    private fun signInWithGoogle() {
        // Sign out first to ensure no cached account is used
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign-In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign-In failed, update UI appropriately
                Log.w("LoginFragment", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {

                    val user = firebaseAuth.currentUser
                    user?.let { saveUserToFirestore(it) }
                    // Sign in success, navigate to HomeFragment
                    navigateToHome()
                } else {
                    // Sign in failed, display a message to the user
                    Toast.makeText(requireContext(), "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Save the user data to Firestore
    private fun saveUserToFirestore(user: FirebaseUser) {
        val firestore = FirebaseFirestore.getInstance()

        // Create a map of user data
        val userData = hashMapOf(
            "fullName" to user.displayName,
            "email" to user.email,
            "profilePicture" to user.photoUrl.toString(),
            "followers" to arrayListOf<String>(),
            "following" to arrayListOf<String>()
        )

        // Store the user data in Firestore
        firestore.collection("Users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // If the user doesn't exist, create a new document
                    firestore.collection("Users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "User data successfully written!")
                            navigateToHome() // Proceed to home after saving user data
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error writing user data", e)
                        }
                } else {
                    // If user already exists, simply navigate to home
                    navigateToHome()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful, navigate to HomeFragment
                    navigateToHome()
                } else {
                    // Login failed, prompt user to register
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed. Please register.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                // Handle any specific failures (e.g., wrong password)
                Toast.makeText(requireContext(), "${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter email to Reset Password")

        val emailInput = EditText(requireContext())
        emailInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(emailInput)

        builder.setPositiveButton("Send") { dialog, _ ->
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter your email address.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


    private fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Password reset email sent.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to send reset email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun navigateToHome() {
        if (isAdded && activity != null) {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish() // Close the current activity
        } else {
            Log.e("LoginFragment", "Fragment is not attached to context, cannot navigate.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}