package biz.itonline.trackrecord

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import biz.itonline.trackrecord.composables.MainScreen
import biz.itonline.trackrecord.location.LocationServiceBase
import biz.itonline.trackrecord.ui.theme.TrackRecordTheme
import biz.itonline.trackrecord.viewModels.HZViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
// TODO permission done process accordingly
                } else {
//TODO permission not granted process accordingly
                }
            }
        }

    lateinit var viewModel: HZViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[HZViewModel::class.java]
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NFC,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    while (true) {
                        viewModel.checkNetAccess()
                        delay(1.minutes)
                    }
                }
            }

        }

        if (hasPermissions(permissionsToRequest.toTypedArray())) {
            setContent {
                TrackRecordTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }


    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        Intent(applicationContext, LocationServiceBase::class.java).apply {
            action = LocationServiceBase.ACTION_START
            startService(this)
        }
    }

    private fun stopLocationUpdates() {
        Intent(applicationContext, LocationServiceBase::class.java).apply {
            action = LocationServiceBase.ACTION_STOP
            startService(this)
        }

    }
}

