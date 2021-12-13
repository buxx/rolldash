package fr.bux.rollingdashboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.database.Observable
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.WorkManager
import fr.bux.rollingdashboard.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var workManager: WorkManager? = null

    // TMP CODE FOR TEST SCHEDULE
    lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.refreshNow.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), importance).apply {
                description = getString(R.string.notification_channel_description)
            }
            val notificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Background worker
        val database = AppDatabase.getDatabase(this@MainActivity)
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val accountConfiguration = database.accountConfigurationDao().get()
                // Start background task only if account configuration is set
                if (accountConfiguration != null) {
                    println("Account configuration exist, enqueue work request")
                    workManager = WorkManager.getInstance(this@MainActivity)
                    workManager?.enqueue(RollingDashboardApplication.instance.buildPeriodicGrabCharacterWorkRequest())
                } else {
                    println("Account configuration do not exist")
                }
            }
        }

//        val accountConfigurationViewModel =  ViewModelProvider(this)[AccountConfigurationViewModel::class.java]
//        lifecycleScope.launch {
//            withContext(Dispatchers.Default) {
//                val accountConfiguration = accountConfigurationViewModel.get()
//                println("{$accountConfiguration}")
//            }
//        }


        // BLA BLA periodique pour refresh ui ?
        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(doSomethingEveryOneSecond)
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