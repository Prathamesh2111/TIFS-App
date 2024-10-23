package com.example.tifs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.example.tifs.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Initialize binding
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize DrawerLayout
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        // Set up NavController

        // Initialize NavController once
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // Set up AppBarConfiguration with the top-level destinations (those that can open the drawer)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_settings, R.id.nav_profile, R.id.nav_followers, R.id.nav_following), drawerLayout)


        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // Link NavController with toolbar
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Link NavController with NavigationView
        NavigationUI.setupWithNavController(navView, navController)

        // Add a listener to keep the drawer icon as the default in all destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (appBarConfiguration.topLevelDestinations.contains(destination.id)) {
                // Show drawer icon
                toggle.syncState()
            } else {
                // Always show the drawer icon even in non-top-level fragments
                toggle.isDrawerIndicatorEnabled = true
                toggle.syncState()
            }
        }

        binding.navView.setNavigationItemSelectedListener(this)

        // Get header view
        val headerView = binding.navView.getHeaderView(0)
        val fullnameTextView: TextView = headerView.findViewById(R.id.fullname)
        val emailTextView: TextView = headerView.findViewById(R.id.emailAddress)
        val profileImageView: ImageView = headerView.findViewById(R.id.profile)

        // Get the current Firebase user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set user's display name and email to the TextViews
            fullnameTextView.text = currentUser.displayName ?: "User"
            emailTextView.text = currentUser.email ?: "email@example.com"

            // Load the profile image using Glide (or Picasso)
            currentUser.photoUrl?.let { profileUri ->
                // Use Glide to load the profile picture
                Glide.with(this)
                    .load(profileUri)
                    .placeholder(R.drawable.boy_avatar_01)  // Placeholder image
                    .error(R.drawable.boy_avatar_01)        // Error image if it fails
                    .into(profileImageView)
            } ?: run {
                // If the user doesn't have a profile picture, set a default image
                profileImageView.setImageResource(R.drawable.boy_avatar_01)
            }
        }

    }

    // Handle Up button navigation
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> navController.navigate(R.id.homeFragment)
            R.id.nav_profile -> navController.navigate(R.id.profileFragment)
            R.id.nav_followers -> navController.navigate(R.id.followersFragment)
            R.id.nav_following -> navController.navigate(R.id.followingFragment)
            R.id.nav_settings -> navController.navigate(R.id.settingFragment)
            R.id.nav_logout -> {
                logoutUser()
                return true
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    private fun logoutUser() {
        auth.signOut()  // Sign out from Firebase
        // Redirect the user to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()  // Close the MainActivity
    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

        // Check if we are already at the HomeFragment
        if (navController?.currentDestination?.id == R.id.homeFragment) {
            // If already on HomeFragment, close the app
            finish()
        } else {
            // Otherwise, navigate to HomeFragment
            navController?.navigate(R.id.homeFragment)
        }
    }
}