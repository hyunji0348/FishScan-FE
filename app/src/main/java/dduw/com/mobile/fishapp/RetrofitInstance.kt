package dduw.com.mobile.fishapp

import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// 보낼 http 객체 포장해주는 곳
object RetrofitInstance {
    // Lambda Url 주소
    private const val BASE_URL = "https://sgihphz27ceyauoxl3oulxnuia0yorex.lambda-url.ap-northeast-2.on.aws/"

    // ✅ Interceptor 추가: 모든 요청에 X-Device-ID 헤더를 자동으로 추가
    // 이 인터셉터는 나중에 SharedViewModel에서 deviceId를 받아서 초기화해야 함
    private var deviceIdInterceptor: Interceptor? = null

    // 외부에서 deviceId를 설정할 수 있는 함수
    fun setDeviceId(deviceId: String) {
        deviceIdInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithDeviceId = originalRequest.newBuilder()
                .header("X-Device-ID", deviceId) // ✅ 헤더 추가
                .build()
            chain.proceed(requestWithDeviceId)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터
        // ✅ deviceIdInterceptor가 설정되어 있을 경우에만 추가
        .apply {
            deviceIdInterceptor?.let { addInterceptor(it) } // null이 아니면 추가
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy { // ⭐ 여기에서 'ApiService' 인터페이스를 참조합니다.
        retrofit.create(ApiService::class.java)
    }
}