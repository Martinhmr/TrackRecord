package biz.itonline.trackrecord.viewModels

import android.location.Location
import androidx.lifecycle.ViewModel
import biz.itonline.trackrecord.data.BaseLocation
import biz.itonline.trackrecord.data.OwnCategory
import biz.itonline.trackrecord.support.DataStoreService
import kotlinx.coroutines.flow.StateFlow

class HZViewModel : ViewModel() {


    private var dataService : DataStoreService = DataStoreService

    val loggedIn = dataService.loginFlow

    val showProgress: StateFlow<Boolean> = dataService.showProgressFlow

    val categoryList: StateFlow<List<OwnCategory>> = dataService.allLocationsList

    val curLocation: StateFlow<Location?> = dataService.curLocationFlow

    val trackRecording: StateFlow<Boolean> = dataService.trackRecording

    val trackData: StateFlow<List<BaseLocation>> = dataService.trackData

    fun login(userName: String, password: String) {
        dataService.login(userName, password)
    }

    fun logout() {
        dataService.logout()
    }

    fun checkNetAccess() {
        dataService.checkNetAccess()
    }

    fun processRecording(recording: Boolean = true) {
        if (recording) {
            var locationReceived = false
            while (!locationReceived) {
                if (curLocation.value != null) {
                    dataService.startLocationRecording(curLocation.value!!)
                    locationReceived = true
                }
            }
        } else {
            dataService.stopLocationRecording()
        }
    }

    fun storeRecording() {
        //TODO store track to the server
    }
}