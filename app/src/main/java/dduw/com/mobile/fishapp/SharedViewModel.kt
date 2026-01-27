package dduw.com.mobile.fishapp

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class SharedViewModel(application: Application) : AndroidViewModel(application) { // ✅ AndroidViewModel 대신 ViewModel 상속

    // SharedPreferences를 사용하여 deviceId를 관리합니다.
    private val preferences = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val DEVICE_ID_KEY = "device_id"
    var isAnalyzing by mutableStateOf(false)

    // 앱의 고유 Device ID (최초 실행 시 생성, 이후 재사용)
    val deviceId: String = getOrCreateDeviceId() // ✅ deviceId 초기화

    private fun getOrCreateDeviceId(): String {
        var id = preferences.getString(DEVICE_ID_KEY, null)
        if (id == null) {
            id = UUID.randomUUID().toString() // 없을 때만 새로 생성
            preferences.edit().putString(DEVICE_ID_KEY, id).apply() // 저장
            println("새로운 Device ID 생성 및 저장: $id")
        } else {
            println("기존 Device ID 로드: $id")
        }
        return id
    }


    var selectedImageUri: Uri? by mutableStateOf(null)
    var analysisResults: List<FishItem> by mutableStateOf(emptyList())

    private val _dictionaryFish = MutableStateFlow<List<FishItem>>(emptyList())
    val dictionaryFish: StateFlow<List<FishItem>> = _dictionaryFish.asStateFlow()

    fun addAnalysisResultsToDictionary(results: List<FishItem>) {
        if (results.isEmpty()) return

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.addToDictionary(deviceId, results)
                if (response.isSuccessful) {
                    println("서버 사전에 물고기 추가 성공: ${response.body()}")
                } else {
                    println("서버 사전에 물고기 추가 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                println("네트워크 오류: 서버 사전에 물고기 추가 실패: ${e.message}")
            } catch (e: HttpException) {
                println("HTTP 오류: 서버 사전에 물고기 추가 실패: ${e.code()}")
            } catch (e: Exception) {
                println("알 수 없는 오류: 서버 사전에 물고기 추가 실패: ${e.message}")
            }
        }
    }

    fun loadDictionaryFromApi() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getDictionary(deviceId)
                if (response.isSuccessful) {
                    _dictionaryFish.value = response.body() ?: emptyList()
                    println("서버에서 사전 로드 성공: ${_dictionaryFish.value.size}개 항목")
                } else {
                    println("서버 사전 로드 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    _dictionaryFish.value = emptyList()
                }
            } catch (e: IOException) {
                println("네트워크 오류: 서버 사전 로드 실패: ${e.message}")
                _dictionaryFish.value = emptyList()
            } catch (e: HttpException) {
                println("HTTP 오류: 서버 사전 로드 실패: ${e.code()}")
                _dictionaryFish.value = emptyList()
            } catch (e: Exception) {
                println("알 수 없는 오류: 서버 사전 로드 실패: ${e.message}")
            }
        }
    }
}