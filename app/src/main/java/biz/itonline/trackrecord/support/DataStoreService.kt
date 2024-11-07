package biz.itonline.trackrecord.support

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import biz.itonline.trackrecord.exceptions.HZException
import biz.itonline.trackrecord.MyApplication
import biz.itonline.trackrecord.data.BaseLocation
import biz.itonline.trackrecord.data.OwnCategory
import biz.itonline.trackrecord.exceptions.HZException.ApiAccessNotAllowed
import biz.itonline.trackrecord.exceptions.HZException.InvalidParamException
import biz.itonline.trackrecord.exceptions.HZException.InvalidUserException
import biz.itonline.trackrecord.exceptions.HZException.NoNetworkConnectionException
import biz.itonline.trackrecord.support.connectivity.ConnectivityObserver
import biz.itonline.trackrecord.support.connectivity.NetTask
import biz.itonline.trackrecord.support.connectivity.NetworkConnectivityObserver
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.measureTime

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object DataStoreService {
    private const val TAG = "DataStoreService"
    private val appContext = MyApplication.applicationContext
    private var dispatcherMain: CoroutineDispatcher = Dispatchers.Main
    private var dispatcherIO: CoroutineDispatcher = Dispatchers.IO
    private val scopeIO = CoroutineScope(SupervisorJob() + dispatcherIO)
    private val scopeMain = CoroutineScope(SupervisorJob() + dispatcherMain)
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val nvTask = NetTask()

    private var userData: UserData = UserData()

    private val _showProgressFlow = MutableStateFlow(false)
    val showProgressFlow: StateFlow<Boolean> = _showProgressFlow.asStateFlow()

    private val _loginFlow = MutableStateFlow(false)
    val loginFlow: StateFlow<Boolean> = _loginFlow.stateIn(
        scope = scopeIO,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private val _allLocationsList = MutableStateFlow<List<OwnCategory>>(emptyList())
    val allLocationsList: StateFlow<List<OwnCategory>> = _allLocationsList.asStateFlow()

    private val _curLocationFlow = MutableStateFlow(null as Location?)
    @OptIn(FlowPreview::class)
    val curLocationFlow: StateFlow<Location?> =
        _curLocationFlow.debounce(300).stateIn(scopeIO, SharingStarted.WhileSubscribed(), null)

    fun setLocation(newLocation: Location) { //TODO CHeck what exactly is triggering this task. Looks like is doubled.
        Log.w(TAG, "setLocation: ${newLocation.latitude} ${newLocation.longitude}")
        _curLocationFlow.update { newLocation }
        if (trackRecording.value) {
            addLocationToTrack(newLocation)
        }

    }

    private var curTrackRecording: String = ""
    private var _trackRecording = MutableStateFlow(false)
    val trackRecording = _trackRecording.asStateFlow()

    private var _trackData = MutableStateFlow<List<BaseLocation>>(emptyList())
    val trackData: StateFlow<List<BaseLocation>> = _trackData.asStateFlow()

    fun startLocationRecording(location: Location) {
        _trackRecording.update { true }
        curTrackRecording = location.toTrackString()
        val newItem = BaseLocation(
            location.latitude,
            location.longitude,
            location.altitude,
            location.accuracy
        )
        _trackData.update { listOf(newItem) }

    }

    fun addLocationToTrack(location: Location) {
        curTrackRecording += ",${location.toTrackString()}"
        val newItem = BaseLocation(
            location.latitude,
            location.longitude,
            location.altitude,
            location.accuracy
        )
        val updatedList = _trackData.value.toMutableList().apply { add(newItem) }
        _trackData.update { updatedList }
    }

    fun stopLocationRecording() {
        _trackRecording.update { false }
    }

    fun setDispatcher(
        dispatcherMain: CoroutineDispatcher = Dispatchers.Main,
        dispatcherIO: CoroutineDispatcher = Dispatchers.IO
    ) {
        this.dispatcherMain = dispatcherMain
        this.dispatcherIO = dispatcherIO
    }

    init {
        checkNetAccess()
    }

    fun setLogin(login: Boolean) {
        _loginFlow.update { login }
    }

    fun login(userName: String, password: String) {
        if (isOnline.value) {
            processLogin(userName, password)
        } else {
            Toast.makeText(appContext, "Offline", Toast.LENGTH_SHORT).show()
        }
    }

    fun logout() {
        setLogin(false)
    }

    private fun processLogin(userName: String, password: String) {
        val param = commonApiParams("login")
        param.put("user", userName.urlSafe()!!)
        param.put("pswd", password.urlSafe()!!)

        scopeIO.launch {
            processApiCall(
                param,
                "/login",
                { response ->
                    scopeIO.launch {
                        processLoginResponse(param, response)
                    }
                },
                NetTask.Method.POST,
                true
            )
        }
    }

    private suspend fun processLoginResponse(param: JSONObject?, js: JSONObject?) {
        Log.v(TAG, "processLoginResponse, processing task: ${param?.optString("task")}")
        val data = js?.getJSONObject("data")
        userData.userToken = data?.optString("token")
        userData.userId = data?.optString("userId")
        userData.userNick = data?.optString("nick")
        userData.userEmail = data?.optString("email")

        setLogin(true)
        getUserAllCategoryData()

        withContext(dispatcherMain) {
            Toast.makeText(
                appContext, "Uživatel je úspěšně přihlášen", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private var connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(appContext)

    val isConnected = connectivityObserver.observe()
    private var _isNetworkAvailable = MutableStateFlow(false)

    val isOnline: StateFlow<Boolean> =
        combine(isConnected, _isNetworkAvailable) { observerIsConnected, netIsAccessible ->
            observerIsConnected && netIsAccessible
        }.stateIn(
            scope = scopeIO,
            started = SharingStarted.Eagerly,
            initialValue = false
        )


    fun checkNetAccess() {
        try {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            scopeIO.launch {
                val hasInternetConnection = hasInternetConnection()
                _isNetworkAvailable.update {
                    (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
                            && hasInternetConnection
                }
            }
        } catch (e: Exception) {
            Log.d("Internet", "curOnline: in exception block")
            e.printStackTrace()
        }

    }

    private suspend fun getUserAllCategoryData() {
        val param = commonApiParams("usercategory")

        withContext(dispatcherIO) {
            processApiCall(
                param,
                "/usercategory",
                { response ->
                    scopeIO.launch {
                        val data = response.getJSONObject("data")
                        val catData = data.optJSONArray("ownCategoryList")
                        catData?.let {
                            Log.v("Internet", "curOnline: in getUserAllCategoryData catData: $it")
                            fillUserCategoryDb(it)
                        }
                    }
                },
                NetTask.Method.GET,
                false
            )
        }
    }

    private fun fillUserCategoryDb(userCategory: JSONArray) {

        val allCat: MutableList<OwnCategory> = ArrayList()
        try {
            measureTime {
                for (i in 0 until userCategory.length()) {
                    val rec = userCategory.getJSONObject(i)
                    val recentCategory =
                        json.decodeFromString(OwnCategory.serializer(), rec.toString())
                    allCat.add(recentCategory)
                }
                _allLocationsList.update { allCat }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (exe: Exception) {
            exe.printStackTrace()
        }
    }



    suspend fun hasInternetConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null;
            try {
                val url = URL(HZConstants.serverURL + "/hello")
                connection = url.openConnection() as HttpURLConnection

                val headers = getHeaders()
                for ((key, value) in headers) {
                    connection.setRequestProperty(key, value)
                }
                connection.requestMethod = "POST"

                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.useCaches = false
                connection.connect()
                val responseCode = connection.responseCode
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.v("Internet", "curOnline: in hasInternetConnection responseCode: $responseCode")
                Log.v("Internet", "curOnline: in hasInternetConnection responseBody: $responseBody")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun getHeaders(userToken: String? = null): Map<String, String> {
        val date: DateFormat = SimpleDateFormat("Z", Locale.getDefault())
        val localTime = date.format(Date())
        val localT = localTime.substring(0, 3) + ":" + localTime.substring(3, 5)
        val headers = HashMap<String, String>()

        headers["Content-Type"] = "application/json; charset=utf-8"
        headers["User-agent"] = System.getProperty("http.agent") as String
        headers["Api-Key"] = HZConstants.api_key
        headers["User-Token"] = userToken ?: "N/A"
        headers["Authorization"] = "Bearer ${HZConstants.bearer_token}"
        headers["Content-Time"] = localT
        return headers
    }

    private fun processApiCall(
        param: JSONObject?,
        url: String,
        resultProcess: (JSONObject) -> Unit,
        method: NetTask.Method = NetTask.Method.GET,
        showProgress: Boolean = false
    ) {
        Log.v("Internet", "curOnline: in processApiCall request URL: $url")
        scopeIO.launch {
            if (showProgress) {
                _showProgressFlow.update { true }
            }
            try {
                val response = nvTask.processOkHZApiCall(
                    param, url, userData.userToken, method
                )
                resultProcess(response)
            } catch (e: HZException) {
                processHZException(e)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (showProgress) {
                    _showProgressFlow.update { false }
                }
            }
        }
    }

    private suspend fun processHZException(e: HZException) {
        when (e) {
            is InvalidUserException -> {
                userData.clearUser()
                logout()
                withContext(dispatcherMain) {
                    Toast.makeText(
                        appContext,
                        "CHYBA -> Neplatné jméno nebo heslo.  ${e.task}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            is NoNetworkConnectionException -> withContext(dispatcherMain) {
                Toast.makeText(
                    appContext,
                    "CHYBA -> problém s připojením k internetu.  ${e.task}",
                    Toast.LENGTH_LONG
                ).show()
            }

            is InvalidParamException -> withContext(dispatcherMain) {
                Toast.makeText(
                    appContext,
                    "CHYBA -> Zadány neplatné parametry.  ${e.task}",
                    Toast.LENGTH_LONG
                ).show()
            }

            is ApiAccessNotAllowed -> withContext(dispatcherMain) {
                Toast.makeText(
                    appContext,
                    "CHYBA -> Přístup na API není povolen.  ${e.task}",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> withContext(dispatcherMain) {
                Toast.makeText(
                    appContext,
                    "Vyskytla se neznámá chyba.  ${e.task}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun commonApiParams(task: String): JSONObject {
        val jsonParams: MutableMap<String, String> = HashMap()
        jsonParams["task"] = task
        return JSONObject((jsonParams as Map<*, *>))
    }

}