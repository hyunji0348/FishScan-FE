package dduw.com.mobile.fishapp

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body // ✅ @Body 어노테이션 임포트 확인
import retrofit2.http.Header

interface ApiService {
    @Multipart
    @POST("analyze")
    suspend fun analyzeImage(@Part file: MultipartBody.Part): Response<AnalysisResponse>

    @POST("dictionary")
    // ✅ @Header("X-Device-ID")로 deviceId를 HTTP 헤더에 포함
    suspend fun addToDictionary(@Header("X-Device-ID") deviceId: String, @Body fishItems: List<FishItem>): Response<Map<String, String>>

    @GET("dictionary")
    // ✅ @Header("X-Device-ID")로 deviceId를 HTTP 헤더에 포함
    suspend fun getDictionary(@Header("X-Device-ID") deviceId: String): Response<List<FishItem>>
}