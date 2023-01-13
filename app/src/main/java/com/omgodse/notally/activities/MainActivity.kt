package com.omgodse.notally.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.omgodse.notally.ImportBackupEvent
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMainBinding
import com.omgodse.notally.viewmodels.BaseNoteModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var configuration: AppBarConfiguration

    private val model: BaseNoteModel by viewModels()

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.Toolbar)
        setupMenu()
        setupNavigation()
        setupSearch()
    }


    private fun setupMenu() {
        val menu = binding.NavigationView.menu
        menu.add(0, R.id.Notes, 0, R.string.notes).setCheckable(true).setIcon(R.drawable.notes)
        menu.add(1, R.id.Labels, 0, R.string.labels).setCheckable(true).setIcon(R.drawable.label)
        menu.add(2, R.id.Deleted, 0, R.string.deleted).setCheckable(true).setIcon(R.drawable.delete)
        menu.add(2, R.id.Archived, 0, R.string.archived).setCheckable(true).setIcon(R.drawable.archive)
        menu.add(3, R.id.Settings, 0, R.string.settings).setCheckable(true).setIcon(R.drawable.settings)
    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.NavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        configuration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, configuration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

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
                        popUpTo(navController.graph.startDestination) { inclusive = false }
                    }
                    navController.navigate(requireNotNull(fragmentIdToLoad), null, options)
                }
            }
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
        binding.EnterSearchKeyword.doAfterTextChanged { text ->
            model.keyword = requireNotNull(text).trim().toString()
        }
    }


    @Subscribe
    fun onImportBackup(event: ImportBackupEvent) {
        if (event.success) {
            Snackbar.make(binding.CoordinatorLayout, R.string.imported_backup, Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(binding.CoordinatorLayout, R.string.invalid_backup, Snackbar.LENGTH_LONG)
                .setAction(R.string.report) { reportCrash(event.files) }
                .show()
        }
    }

    private fun reportCrash(files: Array<File>) {
        val uris = ArrayList<Uri>()
        files.mapTo(uris) { file ->
            FileProvider.getUriForFile(this, "${packageName}.provider", file)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("omgodseapps@gmail.com"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Backup Report (Autogenerated)")
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

        startActivity(intent)
    }
}