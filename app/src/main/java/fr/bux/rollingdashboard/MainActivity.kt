package fr.bux.rollingdashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.WorkManager
import fr.bux.rollingdashboard.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var workManager: WorkManager? = null

    // TMP CODE FOR TEST SCHEDULE
    private var networkGrabPeriod: Int = 0  // FIXME BS NOW : ca ce sera dans background (https://developer.android.com/guide/background) (probablement https://developer.android.com/topic/libraries/architecture/workmanager)
    lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        // Background worker
        workManager = WorkManager.getInstance(this@MainActivity)
        workManager?.enqueue(buildPeriodicWorkRemoteWorkRequest())

        // BLA BLA periodique pour refresh ui ?
        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(doSomethingEveryOneSecond)
    }

    private fun buildPeriodicWorkRemoteWorkRequest(): PeriodicWorkRequest {
        val data: Data = Data.Builder()
            .putString("NAME", "Rolling worker")
            .build()

        return PeriodicWorkRequest.Builder(
            GrabCharacterWorker::class.java,
            // FIXME value by configuration (WHEN CONFIG !)
            MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        ).setInputData(data).build()
    }

    private val doSomethingEveryOneSecond = object : Runnable {
        override fun run() {
            //println("coucou")
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.action_DashboardFragment_to_AccountConfigurationFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}