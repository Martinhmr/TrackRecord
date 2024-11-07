package biz.itonline.trackrecord.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.itonline.trackrecord.ui.theme.LisBlue
import biz.itonline.trackrecord.ui.theme.LisRose
import biz.itonline.trackrecord.viewModels.HZViewModel
import java.text.DecimalFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: HZViewModel
) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val showProgress by viewModel.showProgress.collectAsStateWithLifecycle()
    val categoryList by viewModel.categoryList.collectAsStateWithLifecycle()
    val curLocation by viewModel.curLocation.collectAsStateWithLifecycle()
    val trackData by viewModel.trackData.collectAsStateWithLifecycle()
    val trackRecording by viewModel.trackRecording.collectAsStateWithLifecycle()

    val df = DecimalFormat("#.#####")
    val dfAlt = DecimalFormat("#.#")

    val gpsLocation = curLocation?.let {
        df.format(it.latitude) + ", " + df.format(it.longitude) + ", " + dfAlt.format(it.altitude) + ", " + dfAlt.format(
            it.accuracy
        )
    } ?: "Čekám na GPS souřadnice"
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "Tracking tool") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LisBlue,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (loggedIn) {
                        Text(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable(
                                    onClick = {
                                        viewModel.logout()
                                    }),
                            text = "Logout"
                        )
                    }
                }
            )
        },
        content = { padding ->
            Box(
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "GPS souřadnice: $gpsLocation",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (loggedIn) {
                        TrackingScreen(
                            categoryList,
                            trackRecording = trackRecording,
                            trackData = trackData,
                            processRecording = { viewModel.processRecording(it) },
                            storeTrack = { viewModel.storeRecording() },
                        )
                    } else {
                        LoginScreen(processLogin = { userName, password ->
                            viewModel.login(userName, password)
                        })
                    }
                }

                if (showProgress) CircularProgressIndicator(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(top = 100.dp),
                    color = LisRose,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        },

        )
}