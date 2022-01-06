package com.omgodse.notally.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMainBinding
import com.omgodse.notally.viewmodels.BaseNoteModel

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var configuration: AppBarConfiguration

    private val model: BaseNoteModel by viewModels()

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.Toolbar)
        setupNavigation()
        setupSearch()
    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.NavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        configuration = AppBarConfiguration.Builder(binding.NavigationView.menu)
            .setOpenableLayout(binding.DrawerLayout)
            .build()
        NavigationUI.setupActionBarWithNavController(this, navController, configuration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                if (fragmentIdToLoad != null && navController.currentDestination?.id != fragmentIdToLoad) {
                    val options = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setExitAnim(R.anim.nav_default_exit_anim)
                        .setEnterAnim(R.anim.nav_default_enter_anim)
                        .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                        .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                        .setPopUpTo(navController.graph.startDestination, false)
                        .build()
                    navController.navigate(requireNotNull(fragmentIdToLoad), null, options)
                }
            }

            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
        })

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            fragmentIdToLoad = destination.id
            binding.NavigationView.setCheckedItem(destination.id)
            handleDestinationChange(destination)
        }
    }

    private fun handleDestinationChange(destination: NavDestination) {
        if (destination.id == R.id.Notes) {
            binding.TakeNoteFAB.show()
        } else binding.TakeNoteFAB.hide()

        binding.EnterSearchKeyword.isVisible = (destination.id == R.id.Search)
    }


    private fun setupSearch() {
        binding.EnterSearchKeyword.setText(model.keyword)
        binding.EnterSearchKeyword.addTextChangedListener(onTextChanged = { text, start, count, after ->
            model.keyword = text?.trim()?.toString() ?: String()
        })
    }
}