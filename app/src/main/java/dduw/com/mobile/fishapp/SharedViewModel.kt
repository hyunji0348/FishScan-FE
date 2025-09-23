package dduw.com.mobile.fishapp

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    var selectedImageUri: Uri? by mutableStateOf(null)
    var analysisResults: List<FishItem> by mutableStateOf(emptyList())
}