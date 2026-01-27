package dduw.com.mobile.fishapp
import com.google.gson.annotations.SerializedName

// DTO
// 서버 응답과 일치하는 데이터 클래스
data class FishItem(
    @SerializedName("name") val name: String,
    @SerializedName("korName") val korName: String?,
    @SerializedName("description") val description: String,
    @SerializedName("imageData") val imageData: String
)

// 서버가 반환하는 전체 응답 객체
data class AnalysisResponse(
    val predictions: List<FishItem>
)