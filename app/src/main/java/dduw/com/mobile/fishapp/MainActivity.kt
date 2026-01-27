// MainActivity.kt
package dduw.com.mobile.fishapp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

// Base64 문자열을 Bitmap으로 변환하는 함수
fun base64ToBitmap(base64Str: String): Bitmap? {
    // 'data:image/jpeg;base64,' 접두사 제거 (서버에서 이 접두사를 포함하여 줄 경우)
    val cleanedStr = base64Str.replaceFirst("data:image/jpeg;base64,", "")
    return try {
        val decodedBytes = Base64.decode(cleanedStr, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        null
    }
}

// URI를 파일로 변환하는 헬퍼 함수 (HomeScreen 내부 또는 별도 파일로 이동 가능)
fun uriToFile(uri: Uri, context: Context): File? {
    val contentResolver = context.contentResolver
    val inputStream: InputStream? = contentResolver.openInputStream(uri) ?: return null
    val tempFile = File(context.cacheDir, "temp_image_file.jpg")
    val outputStream = FileOutputStream(tempFile)
    inputStream.use { input -> outputStream.use { output -> input?.copyTo(output) } }
    return tempFile
}


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E7CF6),
                    secondary = Color(0xFF5EBEF5),
                    surface = Color(0xFFF8FAFF),
                    onSurface = Color(0xFF0A0F1F)
                )
            ) {
                val nav = rememberNavController()
                // ✅ ViewModel 인스턴스 생성 시 ViewModelProvider.Factory 사용
                // AndroidViewModel을 상속받았으므로 기본 Factory로 충분합니다.
                val sharedViewModel: SharedViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return SharedViewModel(application) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                // ✅ RetrofitInstance에 deviceId 설정
                // sharedViewModel이 초기화된 후 deviceId 값을 RetrofitInstance에 넘겨줍니다.
                LaunchedEffect(sharedViewModel.deviceId) {
                    RetrofitInstance.setDeviceId(sharedViewModel.deviceId)
                }

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            navToAnalyze = { nav.navigate("analyze") },
                            sharedViewModel = sharedViewModel,
                            onOpenDictionary = { nav.navigate("dictionary") }
                        )
                    }
                    composable("analyze") {
                        AnalyzeScreen(
                            sharedViewModel = sharedViewModel,
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("dictionary") {
                        DictionaryScreen(
                            onBack = { nav.popBackStack() },
                            sharedViewModel = sharedViewModel // ✅ sharedViewModel 전달
                        )
                    }
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

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ✅ 로딩 상태를 ViewModel에서 가져옴
    val isAnalyzing = sharedViewModel.isAnalyzing // 이미 ViewModel에 mutableStateOf로 정의되어 있다면 직접 접근

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FishScan", fontWeight = FontWeight.Bold) }
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
                        model = sharedViewModel.selectedImageUri,
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
                            // ✅ 로딩 상태 시작
                            sharedViewModel.isAnalyzing = true

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val file = uriToFile(uri, context) ?: return@launch
                                    // ✅ uriToFile 호출
                                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                    val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                    val response = RetrofitInstance.api.analyzeImage(multipartBody) // ✅ RetrofitInstance.api 호출

                                    withContext(Dispatchers.Main) {
                                        val responseBody = response.body()?.toString()
                                        Log.d("SERVER_RESPONSE", "Raw Response: $responseBody")

                                        if (response.isSuccessful) {
                                            val analysisResponse = response.body() // 결과 저장
                                            sharedViewModel.analysisResults = analysisResponse?.predictions ?: emptyList()
                                            navToAnalyze() // 결과 화면 전환
                                        } else {
                                            Log.e("API_CALL", "Failed: ${response.code()}")
                                            Toast.makeText(context, "분석에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: IOException) {
                                    Log.e("API_CALL", "Network Error: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_CALL", "Error: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    // ✅ 로딩 상태 종료 (성공/실패/오류에 관계없이)
                                    sharedViewModel.isAnalyzing = false
                                }
                            }
                        }
                    },
                    // ✅ 버튼 활성화 조건: 이미지가 선택되어 있고, 분석 중이 아닐 때
                    enabled = sharedViewModel.selectedImageUri != null && !isAnalyzing,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    // ✅ 로딩 스피너 표시 로직
                    if (isAnalyzing) {
                        // 로딩 스피너 (CircularProgressIndicator)를 사용합니다.
                        // Material3에서 CircularProgressIndicator를 사용하려면 dependency에 material3가 필요합니다.
                        CircularProgressIndicator(
                            color = Color.White, // 버튼 색상에 따라 조정
                            modifier = Modifier.size(24.dp), // 크기 조정
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text("이미지 분석", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    // ✅ 분석 중일 때는 사전 버튼도 비활성화
                    onClick = onOpenDictionary,
                    enabled = !isAnalyzing,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
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
    val heroUrl = sharedViewModel.selectedImageUri
    val detections = sharedViewModel.analysisResults

    var selected by remember { mutableStateOf<FishItem?>(null) }
    var showNoResultsDialog by remember { mutableStateOf(false) } // ✅ 다이얼로그 표시 여부 상태

    // ✅ 화면 진입 시점에 한 번만 실행되도록 LaunchedEffect 사용
    LaunchedEffect(Unit) { // Unit을 키로 사용하면 컴포저블이 처음 구성될 때만 실행됩니다.
        if (detections.isNotEmpty()) {
            sharedViewModel.addAnalysisResultsToDictionary(detections) // 분석 결과 사전에 추가 api 함수 호출
        } else {
            // ✅ 분석 결과가 없을 경우 다이얼로그 표시 상태를 true로 설정
            showNoResultsDialog = true
        }
    }

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

    // ✅ showNoResultsDialog 상태에 따라 AlertDialog 표시
    if (showNoResultsDialog) {
        AlertDialog(
            onDismissRequest = { showNoResultsDialog = false }, // 다이얼로그 바깥 영역 탭 시 닫힘
            title = { Text("알림") },
            text = { Text("이미지 분석 결과가 없습니다.") },
            confirmButton = {
                TextButton(onClick = { showNoResultsDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
fun DetectionCard(
    det: FishItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val bitmap = base64ToBitmap(det.imageData) // ✅ base64ToBitmap 호출

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Detected Fish: ${det.name}",
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(Color.Gray)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 로드 실패", color = Color.White, fontSize = 10.sp)
                }
            }
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    det.korName ?: det.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = 60.dp)
                ) {
                    Text(
                        det.description,
                        fontSize = 11.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// 카드 세부정보 모달창(분석결과, 사전 페이지 공통)
@Composable
fun FishDetailDialog(
    fish: FishItem,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        title = { Text(fish.korName ?: fish.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(min = 0.dp, max = 360.dp)
                    .verticalScroll(scroll)
            ) {
                // 1. URL인지 Base64인지 확인
                val imageData = fish.imageData
                val isUrl = imageData.startsWith("http://") || imageData.startsWith("https://")

                if (isUrl) {
                    // 2-1. URL인 경우, Coil의 AsyncImage를 사용하여 비동기 로드
                    AsyncImage(
                        model = fish.imageData,
                        contentDescription = fish.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    // 2-2. URL이 아닌 경우 (Base64로 간주), 기존 방식 base64ToBitmap 사용
                    val bitmap = base64ToBitmap(imageData)

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(), // bitmap.asImageBitmap()는 'androidx.compose.ui.graphics.ImageBitmap'을 임포트해야 합니다.
                            contentDescription = fish.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        // Base64 디코딩 실패 시 대체 UI
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                                .background(Color.Gray)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("이미지 로드 실패 (Base64)", color = Color.White)
                        }
                    }
                }

                Text(fish.description)
            }
        }
    )
}



/* -------------------- DICTIONARY 화면 -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onBack: () -> Unit, sharedViewModel: SharedViewModel
) {
    var showDetail by remember { mutableStateOf<FishItem?>(null) }

    val dictionaryItems by sharedViewModel.dictionaryFish.collectAsState()

    // ✅ DictionaryScreen이 활성화될 때마다 사전 데이터를 새로고침하도록 LaunchedEffect 추가
    LaunchedEffect(Unit) { // Unit을 키로 사용하면 컴포저블이 처음 구성될 때만 실행됩니다.
        sharedViewModel.loadDictionaryFromApi()
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
            items(dictionaryItems) { fish ->
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
            // ✅ 이미지 모델 결정 로직 수정
            val modelImage = when {
                // 1. HTTP/HTTPS URL인 경우 (새로 추가된 로직)
                item.imageData.startsWith("http://") || item.imageData.startsWith("https://") -> {
                    item.imageData
                }
                // 2. data:image/base64 스키마가 포함된 경우
                item.imageData.startsWith("data:image") -> {
                    item.imageData
                }
                // 3. 순수 Base64 문자열인 경우 (접두사 추가)
                else -> {
                    "data:image/jpeg;base64,${item.imageData}"
                }
            }

            // 이미지 부분
            AsyncImage(
                model = modelImage, // URL 또는 Base64 문자열
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // 정사각형
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.korName ?: item.name,
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