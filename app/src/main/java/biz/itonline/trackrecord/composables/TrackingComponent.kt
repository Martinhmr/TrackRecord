package biz.itonline.trackrecord.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import biz.itonline.trackrecord.data.BaseLocation
import biz.itonline.trackrecord.data.OwnCategory
import biz.itonline.trackrecord.data.parseJsonToBaseLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun TrackingScreen(
    categoryList: List<OwnCategory>,
    trackRecording: Boolean = false,
    trackData: List<BaseLocation> = listOf(),
    processRecording: (Boolean) -> Unit = { },
    storeTrack: () -> Unit = { }
) {
    var selectedCategory by remember { mutableStateOf("") }

    var trackLocation by remember {
        mutableStateOf(parseJsonToBaseLocation(categoryList.find { it.categoryId == selectedCategory }?.trackPoly))
    }

    LaunchedEffect(selectedCategory) {
        trackLocation =
            parseJsonToBaseLocation(categoryList.find { it.categoryId == selectedCategory }?.trackPoly)
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState)
        ) {
            Text(
                text = "Přehled kategorií",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontWeight = FontWeight.Bold
            )

            CategorySelector(
                memberCategoryList = categoryList,
                selectedCategory = selectedCategory,
                defaultEmptyText = "Žádné kategorie",
                catUpdate = { catId ->
                    selectedCategory = catId
                }
            )

            if (categoryList.isNotEmpty()) {
                Text(
                    text = categoryList.find { it.categoryId == selectedCategory }?.categoryName
                        ?: ""
                )
            } else {
                Text(text = "Žádná vybraná kategorie")
            }

            Spacer(modifier = Modifier.padding(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        processRecording(!trackRecording)
                    },
                    modifier = Modifier.padding(8.dp),
                    enabled = true
                ) {
                    Text(text = if (trackRecording) "Zastavit záznam" else "Záznam trasy")
                }
                Button(
                    onClick = {
                        storeTrack()
                    },
                    modifier = Modifier.padding(8.dp),
                    enabled = true
                ) {
                    Text(text = "Uložit trasu")
                }

            }
            
            Spacer(modifier = Modifier.padding(10.dp))

            Row {
                if (trackData.isNotEmpty() || trackRecording) {
                    MapsPoly(
                        modifier = Modifier.fillMaxSize(),
                        trackPoly = trackData
                    )
                } else {
                    MapsPoly(
                        modifier = Modifier.fillMaxSize(),
                        trackPoly = trackLocation?.toList() ?: listOf()
                    )

                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    modifier: Modifier = Modifier,
    memberCategoryList: List<OwnCategory> = listOf(),
    selectedCategory: String,
    defaultEmptyText: String = "",
    catUpdate: (String) -> Unit = { }
) {
    var mDisplaySelection by remember { mutableStateOf(false) }

    val catName = memberCategoryList.find { it.categoryId == selectedCategory }?.categoryName
        ?: defaultEmptyText

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(text = catName,
                modifier = Modifier.clickable { mDisplaySelection = !mDisplaySelection }
            )

            IconButton(onClick = { mDisplaySelection = !mDisplaySelection }) {
                Icon(Icons.Default.ArrowDropDown, "")
            }
        }

        Box(
            modifier = modifier
                .requiredWidth(50.dp)
        ) {
            DropdownMenu(
                expanded = mDisplaySelection,
                onDismissRequest = { mDisplaySelection = false }
            ) {
                for (category in memberCategoryList) {
                    DropdownMenuItem(
                        text = { Text(text = category.categoryName) },
                        onClick = {
                            catUpdate(category.categoryId)
                            mDisplaySelection = false
                        }
                    )
                }
            }
        }

    }
}

@Composable
fun MapsPoly(
    modifier: Modifier = Modifier,
    trackPoly: List<BaseLocation>
) {
    val properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = true,
            )
        )
    }

    var onMapStatus by remember {
        mutableStateOf(false)
    }

    var currentPosition = LatLng(50.092039404672136, 14.401923602591369)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPosition, 18f)
    }

    LaunchedEffect(trackPoly) {
        currentPosition = if (trackPoly.isNotEmpty()) trackPoly[0].let {
            LatLng(
                it.latitude,
                it.longitude
            )
        } else LatLng(50.092039404672136, 14.401923602591369)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentPosition, 18f)
    }


    val locationList = trackPoly.map {
        LatLng(
            it.latitude,
            it.longitude
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = modifier
                .fillMaxSize()
                .requiredHeight(600.dp),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapLoaded = {
                onMapStatus = true
            },
        )
        {
            if (onMapStatus) {
                Polyline(
                    points = locationList,
                    clickable = true,
                    jointType = JointType.ROUND,
                    color = Color.Blue,
                    width = 15f
                )

            }

        }
    }
}