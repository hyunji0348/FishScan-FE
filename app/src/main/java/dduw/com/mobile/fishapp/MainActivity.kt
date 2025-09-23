package dduw.com.mobile.fishapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.util.DebugLogger
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
//import coil.fetch.Base64Fetcher


// Base64 문자열을 Bitmap으로 변환하는 함수
fun base64ToBitmap(base64Str: String): Bitmap? {
    // 'data:image/jpeg;base64,' 접두사 제거
    val cleanedStr = base64Str.replaceFirst("data:image/jpeg;base64,", "")
    return try {
        val decodedBytes = Base64.decode(cleanedStr, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// --- Coil ImageLoader 설정 시작 ---
//        val imageLoader = ImageLoader.Builder(this)
//            .logger(DebugLogger()) // 디버그 로거 활성화
//            .components {
//                // 이 줄을 추가하세요.
////                add(Base64Decoder.Factory())
//            }
//            // Coil 2.x 버전은 Base64 문자열을 자동으로 인식하여 처리합니다.
//            .build()
//        Coil.setImageLoader(imageLoader) // Coil의 기본 ImageLoader 설정
        // --- Coil ImageLoader 설정 끝 ---
        setContent {
            // 기본 테마
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E7CF6),
                    secondary = Color(0xFF5EBEF5),
                    surface = Color(0xFFF8FAFF),
                    onSurface = Color(0xFF0A0F1F)
                )
            ) {
                val nav = rememberNavController()
                val sharedViewModel: SharedViewModel = viewModel() // ViewModel 인스턴스 생성

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            navToAnalyze = { nav.navigate("analyze") }, // 화면 전환 함수
                            sharedViewModel = sharedViewModel, // ViewModel 전달
                            onOpenDictionary = { nav.navigate("dictionary") }
                        )
                    }
                    composable("analyze") {
                        AnalyzeScreen(
                            sharedViewModel = sharedViewModel, // ViewModel 전달
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("dictionary") { DictionaryScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedViewModel: SharedViewModel,
    navToAnalyze: () -> Unit,
    onOpenDictionary: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        sharedViewModel.selectedImageUri = uri // ViewModel에 URI 저장
    }

    // API 호출을 위한 CoroutineScope와 Context
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("앱 이름(미정)", fontWeight = FontWeight.Bold) }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
                    .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    .background(if (sharedViewModel.selectedImageUri == null) Brush.linearGradient(listOf(Color(0xFF2E7CF6), Color(0xFF5EBEF5))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                if (sharedViewModel.selectedImageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("물고기 사진을 업로드하세요", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("사진 업로드", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    AsyncImage(
                        model = sharedViewModel.selectedImageUri, // ViewModel의 URI 사용
                        contentDescription = "selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(28.dp))
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        sharedViewModel.selectedImageUri?.let { uri ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val file = uriToFile(uri, context) ?: return@launch
                                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                    val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                    val response = RetrofitInstance.api.analyzeImage(multipartBody)

                                    withContext(Dispatchers.Main) {
                                        val responseBody = response.body()?.toString()
                                        Log.d("SERVER_RESPONSE", "Raw Response: $responseBody")

                                        if (response.isSuccessful) {
                                            val analysisResponse = response.body()
                                            sharedViewModel.analysisResults = analysisResponse?.predictions ?: emptyList()
                                            navToAnalyze() // 화면 전환
                                        } else {
                                            Log.e("API_CALL", "Failed: ${response.code()}")
                                        }
                                    }
                                } catch (e: IOException) {
                                    Log.e("API_CALL", "Network Error: ${e.message}")
                                } catch (e: Exception) {
                                    Log.e("API_CALL", "Error: ${e.message}")
                                }
                            }
                        }
                    },
                    enabled = sharedViewModel.selectedImageUri != null,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("이미지 분석", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onOpenDictionary, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("발견한 물고기 사전")
                }
            }
        }
    }
}


/* -------------------- ANALYZE (결과) -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    sharedViewModel: SharedViewModel,
    onBack: () -> Unit
) {
    // ViewModel에서 데이터를 가져옵니다.
    val heroUrl = sharedViewModel.selectedImageUri
    val detections = sharedViewModel.analysisResults

    var selected by remember { mutableStateOf<FishItem?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("분석 결과") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (heroUrl != null) {
                Card(shape = RoundedCornerShape(20.dp)) {
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = "uploaded",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text("상위 3가지 결과", fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(detections) { det ->
                    DetectionCard(det = det, onClick = { selected = det })
                }
            }
        }
    }
    if (selected != null) {
        FishDetailDialog(fish = selected!!, onDismiss = { selected = null })
    }
}

@Composable
fun DetectionCard(
    det: FishItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.clickable { onClick() } // ✅ 카드 탭
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val bitmap = base64ToBitmap(det.imageData)

            if (bitmap != null) {
                // Bitmap을 Image Composable로 표시
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Detected Fish: ${det.name}",
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 디코딩 실패 시 Placeholder 표시
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(Color.Gray)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 로드 실패", color = Color.White, fontSize = 10.sp)
                }
            }
//            val imageBase64 = "data:image/jpeg;base64,${det.imageData}"
//            Log.d("IMAGE_DATA", "Loading Base64: $imageBase64") // 이 로그를 추가하여 데이터 확인
//
//            AsyncImage(
//                model = imageBase64, // "data:image/jpeg;base64,..." 형태의 문자열을 여기에 바로 전달
//                contentDescription = "Detected Fish: ${det.name}",
//                modifier = Modifier
//                    .size(100.dp) // 이미지가 충분히 보이도록 크기를 지정해주세요. (원하는 크기로 조절)
//                    .clip(RoundedCornerShape(8.dp)),
//                contentScale = ContentScale.Crop // 이미지가 잘려도 화면에 채워지도록
//            )

            // 텍스트 영역: 이름 1줄 + 설명(남은 공간만큼)
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    det.name.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ✅ 설명: 카드의 남은 세로공간을 최대한 사용해서 표시
                //   - weight(1f)로 남은 공간을 차지하게 하고
                //   - 길면 Ellipsis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = 60.dp) // 카드 높이 과도 증가 방지 (필요시 조절)
                ) {
                    Text(
                        det.description,
                        fontSize = 11.sp,
                        maxLines = 3, // 남은 공간 예측이 어려우니 현실적으로 2~3줄 제한
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// 분석결과 세부정보 모달창
@Composable
fun FishDetailDialog(
    fish: FishItem,
    onDismiss: () -> Unit
) {
    if (fish == null) return  // null이면 아무것도 표시하지 않음

    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        title = { Text(fish.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    // ✅ 다이얼로그 본문을 스크롤 가능하게
                    .heightIn(min = 0.dp, max = 360.dp)
                    .verticalScroll(scroll)
            ) {

                val bitmap = base64ToBitmap(fish.imageData)

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = fish.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    // 디코딩 실패 시 Placeholder 표시
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                            .background(Color.Gray)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("이미지 로드 실패", color = Color.White)
                    }
                }
                Text(fish.description) // 길면 스크롤로 모두 보임
            }
        }
    )
}


/* -------------------- DICTIONARY 화면 -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    var showDetail by remember { mutableStateOf<FishItem?>(null) }

    val items = remember {
        listOf(
            FishItem("물고기1", "물고기1 설명", "https://images.unsplash.com/photo-1524704796725-9fc3044a58b2?w=600"),
            FishItem("물고기2", "물고기2 설명", "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=600"),
            FishItem("물고기3", "물고기3 설명", "https://images.unsplash.com/photo-1560275619-4662e36fa65c?w=600"),
            FishItem("물고기4", "물고기4 설명", "https://images.unsplash.com/photo-1545464333-9a7123dba20d?w=600"),
            FishItem("물고기5", "물고기5 설명", "https://images.unsplash.com/photo-1552410260-67cda8d2d5d1?w=600"),
            FishItem("물고기6", "물고기6 설명", "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0?w=600"),
        ).let { it + it }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("수집한 물고기") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { fish ->
                FishCard(
                    item = fish,
                    onClick = { showDetail = fish }
                )
            }
        }

        if (showDetail != null) {
            FishDetailDialog(
                fish = showDetail!!,
                onDismiss = { showDetail = null }
            )
        }
    }
}


@Composable
fun FishCard(
    item: FishItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth()) {
            val isBase64 = item.imageData.startsWith("data:image")
            val modelImage = if (isBase64) {
                // Base64 형식일 경우 그대로 사용
                item.imageData
            } else {
                "data:image/jpeg;base64,${item.imageData}" // Base64 문자열이 아닌 경우, 접두사 추가
            }

            // 이미지 부분
            AsyncImage(
                model = modelImage,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // 정사각형
            )

            // 이름 부분: 흰 배경 + 가운데 정렬
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
            }
        }
    }
}