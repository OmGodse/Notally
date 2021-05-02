package com.omgodse.notally.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.*
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMainBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.setVisible

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.Toolbar)
        setupNavigation()
    }

    private fun setupNavigation() {
        navController = findNavController(R.id.NavigationHost)
        appBarConfiguration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START, true)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {
                if (fragmentIdToLoad != null && navController.currentDestination?.id != fragmentIdToLoad) {
                    val options = navOptions {
                        launchSingleTop = true
                        anim {
                            exit = R.anim.nav_default_exit_anim
                            enter = R.anim.nav_default_enter_anim
                            popExit = R.anim.nav_default_pop_exit_anim
                            popEnter = R.anim.nav_default_pop_enter_anim
                        }
                        popUpTo = navController.graph.startDestination
                    }
                    navController.navigate(requireNotNull(fragmentIdToLoad), null, options)
                }
            }
        })

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            fragmentIdToLoad = destination.id
            binding.NavigationView.setCheckedItem(destination.id)
            handleDestinationChange(destination, arguments)
        }
    }

    private fun handleDestinationChange(destination: NavDestination, arguments: Bundle?) {
        when (destination.id) {
            R.id.NotesFragment -> binding.TakeNoteFAB.show()
            R.id.SearchFragment -> {
                binding.TakeNoteFAB.hide()
                binding.EnterSearchKeyword.requestFocus()
            }
            R.id.DisplayLabelFragment -> {
                binding.TakeNoteFAB.hide()
                supportActionBar?.setTitle(arguments?.getString(Constants.SelectedLabel))
            }
            else -> binding.TakeNoteFAB.hide()
        }
        binding.EnterSearchKeyword.setVisible(destination.id == R.id.SearchFragment)
    }
}