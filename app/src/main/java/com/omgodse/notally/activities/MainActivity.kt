package com.omgodse.notally.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMainBinding
import com.omgodse.notally.miscellaneous.Constants

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
        val topLevelDestinations = getTopLevelDestinations()
        navController = findNavController(R.id.NavigationHost)
        appBarConfiguration = AppBarConfiguration(topLevelDestinations, binding.DrawerLayout)
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
                    val builder = NavOptions.Builder()
                    builder.setLaunchSingleTop(true)
                    builder.setEnterAnim(R.anim.nav_default_enter_anim)
                    builder.setExitAnim(R.anim.nav_default_exit_anim)
                    builder.setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                    builder.setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                    builder.setPopUpTo(navController.graph.startDestination, false)
                    val options = builder.build()
                    navController.navigate(fragmentIdToLoad!!, null, options)
                }
            }
        })

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            fragmentIdToLoad = destination.id
            binding.NavigationView.setCheckedItem(destination.id)
            handleDestinationChange(destination, arguments)
        }
    }

    private fun getTopLevelDestinations(): Set<Int> {
        return setOf(R.id.NotesFragment, R.id.LabelsFragment, R.id.DeletedFragment, R.id.ArchivedFragment, R.id.SettingsFragment)
    }

    private fun handleDestinationChange(destination: NavDestination, arguments: Bundle?) {
        when (destination.id) {
            R.id.NotesFragment -> {
                binding.TakeNoteFAB.show()
                binding.EnterSearchKeyword.visibility = View.GONE
            }
            R.id.SearchFragment -> {
                binding.TakeNoteFAB.hide()
                binding.EnterSearchKeyword.text = null
                binding.EnterSearchKeyword.visibility = View.VISIBLE
            }
            R.id.DisplayLabelFragment -> {
                binding.TakeNoteFAB.hide()
                binding.EnterSearchKeyword.visibility = View.GONE
                supportActionBar?.setTitle(arguments?.getString(Constants.argLabelKey))
            }
            else -> {
                binding.TakeNoteFAB.hide()
                binding.EnterSearchKeyword.visibility = View.GONE
            }
        }
    }
}