package dduw.com.mobile.fishapp

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import android.content.Context
import android.net.Uri

interface FishApiService {
    @Multipart
    @POST("/analyze")
    suspend fun analyzeImage(@Part image: MultipartBody.Part): Response<AnalysisResponse>
}

object RetrofitInstance {
    // 로컬 서버 주소. 'YOUR_LOCAL_IP_ADDRESS'를 당신의 컴퓨터 IP로 변경하세요.
    private const val BASE_URL = "http://172.30.1.12:8000/"

    val api: FishApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // JSON 응답을 AnalysisResponse 객체로 자동 변환
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FishApiService::class.java)
    }
}

// URI를 파일로 변환하는 헬퍼 함수 (HomeScreen 내부에 추가)
fun uriToFile(uri: Uri, context: Context): File? {
    val contentResolver = context.contentResolver
    val inputStream: InputStream? = contentResolver.openInputStream(uri) ?: return null
    val tempFile = File(context.cacheDir, "temp_image_file.jpg")
    val outputStream = FileOutputStream(tempFile)
    inputStream.use { input -> outputStream.use { output -> input?.copyTo(output) } }
    return tempFile
}