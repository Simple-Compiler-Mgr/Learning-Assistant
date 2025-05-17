package com.sompiler.lass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sompiler.lass.ui.theme.LearningAssistantTheme
import com.sompiler.lass.service.TimerService
import com.sompiler.lass.viewmodel.TimerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import java.text.SimpleDateFormat
import java.util.*
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow

object AppState {
    var shortestLapTime: Long? = null
}

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    val allReminders = repository.allReminders.asLiveData()
    val allQuestions = repository.allQuestions.asLiveData()
    val allStudyRecords = repository.allStudyRecords.asLiveData()
    
    // 计时器状态
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    // 秒表状态
    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()
    
    fun updateTimerState(
        selectedTime: Long? = null,
        remainingTime: Long? = null,
        isRunning: Boolean? = null,
        startTime: Long? = null
    ) {
        _timerState.value = _timerState.value.copy(
            selectedTime = selectedTime ?: _timerState.value.selectedTime,
            remainingTime = remainingTime ?: _timerState.value.remainingTime,
            isRunning = isRunning ?: _timerState.value.isRunning,
            startTime = startTime ?: _timerState.value.startTime
        )
    }
    
    fun updateStopwatchState(
        elapsedTime: Long? = null,
        isRunning: Boolean? = null,
        startTime: Long? = null,
        lapTimes: List<Long>? = null
    ) {
        _stopwatchState.value = _stopwatchState.value.copy(
            elapsedTime = elapsedTime ?: _stopwatchState.value.elapsedTime,
            isRunning = isRunning ?: _stopwatchState.value.isRunning,
            startTime = startTime ?: _stopwatchState.value.startTime,
            lapTimes = lapTimes ?: _stopwatchState.value.lapTimes
        )
    }
    
    fun insertReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.insertReminder(reminder)
    }
    
    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder)
    }
    
    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch {
        repository.deleteReminder(reminder)
    }
    
    fun insertQuestion(question: QuestionEntity) = viewModelScope.launch {
        repository.insertQuestion(question)
    }
    
    fun deleteQuestion(question: QuestionEntity) = viewModelScope.launch {
        repository.deleteQuestion(question)
    }
    
    fun insertStudyRecord(record: StudyRecordEntity) = viewModelScope.launch {
        repository.insertStudyRecord(record)
    }
    
    fun deleteStudyRecord(record: StudyRecordEntity) = viewModelScope.launch {
        repository.deleteStudyRecord(record)
    }
}

data class TimerState(
    val selectedTime: Long = 0L,
    val remainingTime: Long = 0L,
    val isRunning: Boolean = false,
    val startTime: Long? = null
)

data class StopwatchState(
    val elapsedTime: Long = 0L,
    val isRunning: Boolean = false,
    val startTime: Long? = null,
    val lapTimes: List<Long> = emptyList()
)

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = AppRepository(database)
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(repository) as T
            }
        }
    }
    
    private val timerViewModel: TimerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = AppRepository(database)
                val viewModel = TimerViewModel(application)
                viewModel.setRepository(repository)
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            LearningAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel, timerViewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel, timerViewModel: TimerViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    "计时" to Icons.Filled.Home,
                    "秒表" to Icons.Filled.Timer,
                    "提醒" to Icons.Filled.Notifications,
                    "错题" to Icons.Filled.List,
                    "关于" to Icons.Filled.Info
                )
                
                items.forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> TimerScreen(viewModel)
                1 -> StopwatchScreen(viewModel)
                2 -> ReminderScreen(viewModel)
                3 -> QuestionScreen(viewModel)
                4 -> AboutScreen()
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: AppViewModel) {
    val timerState by viewModel.timerState.collectAsState()
    var showCustomTime by remember { mutableStateOf(false) }
    val isRunning = timerState.isRunning
    val presetTimes = listOf(30L, 60L, 90L, 120L)
    val scope = rememberCoroutineScope()

    // 右上角窗口状态
    var showHistory by remember { mutableStateOf(false) }
    var showDuration by remember { mutableStateOf(false) }

    // 动画：背景脉冲
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // 动画：按钮分裂
    var showSplit by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) { showSplit = isRunning }

    // 动画：自定义弹窗
    var showCustomAnim by remember { mutableStateOf(false) }
    LaunchedEffect(showCustomTime) {
        if (showCustomTime) showCustomAnim = true
    }

    LaunchedEffect(isRunning, timerState.startTime, timerState.selectedTime) {
        if (isRunning) {
            val startTimestamp = timerState.startTime ?: System.currentTimeMillis()
            val totalMillis = timerState.remainingTime
            val startRemain = timerState.remainingTime
            val startAt = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startAt
                val left = startRemain - elapsed
                if (!isRunning || left <= 0) break
                viewModel.updateTimerState(remainingTime = left)
                delay(100L)
            }
            if (isRunning) {
                // 倒计时结束
                viewModel.updateTimerState(isRunning = false, remainingTime = 0L, startTime = null)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 背景脉冲动画
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .alpha(0.08f)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部栏，右上角按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.History, contentDescription = "历史记录")
                    }
                    IconButton(onClick = { showDuration = true }) {
                        Icon(Icons.Filled.Assessment, contentDescription = "学习时长记录")
                    }
                }
            }
            // 顶部大号时间（无跳动动画）
            Text(
                text = formatTime(timerState.remainingTime),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 88.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 0.dp)
            )
            Spacer(modifier = Modifier.height(64.dp))
            // 预设时间按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                presetTimes.forEach { time ->
                    PresetTimeCard1to1(
                        time = time,
                        selected = false,
                        onClick = {
                            if (!isRunning) {
                                // 未开始时覆盖
                                viewModel.updateTimerState(
                                    selectedTime = time,
                                    remainingTime = time * 60 * 1000L
                                )
                            } else {
                                // 计时中叠加
                                val addMillis = time * 60 * 1000L
                                val newRemain = timerState.remainingTime + addMillis
                                val newSelected = (newRemain / 1000 / 60).coerceAtLeast(1L)
                                viewModel.updateTimerState(
                                    selectedTime = newSelected,
                                    remainingTime = newRemain
                                )
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 自定义按钮
            CustomTimeCard1to1(onClick = { showCustomTime = true })
            Spacer(modifier = Modifier.weight(1f))
            // 底部操作按钮（分裂动画）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp), // 更靠上
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showSplit,
                    transitionSpec = {
                        fadeIn(tween(200)) + slideInHorizontally { it } togetherWith
                        fadeOut(tween(200)) + slideOutHorizontally { -it }
                    }, label = "split"
                ) { split ->
                    if (!split) {
                        MainActionButton1to1(
                            text = "开始",
                            icon = Icons.Filled.PlayArrow,
                            onClick = {
                                viewModel.updateTimerState(isRunning = true, startTime = System.currentTimeMillis())
                            },
                            enabled = timerState.selectedTime > 0 && timerState.remainingTime > 0
                        )
                    } else {
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            MainActionButton1to1(
                                text = "暂停",
                                icon = Icons.Filled.Pause,
                                onClick = {
                                    viewModel.updateTimerState(isRunning = false, startTime = null)
                                }
                            )
                            MainActionButton1to1(
                                text = "重置",
                                icon = Icons.Filled.Clear,
                                onClick = {
                                    viewModel.updateTimerState(
                                        isRunning = false,
                                        startTime = null,
                                        remainingTime = timerState.selectedTime * 60 * 1000L
                                    )
                                },
                                isRed = true
                            )
                        }
                    }
                }
            }
        }
        // 自定义时间弹窗动画
        AnimatedVisibility(
            visible = showCustomTime,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            CustomTimeScreen(
                onDismiss = { showCustomTime = false },
                onTimeSelected = { time ->
                    viewModel.updateTimerState(
                        selectedTime = time,
                        remainingTime = time * 60 * 1000L
                    )
                    showCustomTime = false
                }
            )
        }
        // 历史记录窗口
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            HistoryWindow(viewModel, { showHistory = false })
        }
        // 学习时长记录窗口
        AnimatedVisibility(
            visible = showDuration,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            DurationWindow(viewModel, { showDuration = false })
        }
    }
}

@Composable
fun PresetTimeCard1to1(time: Long, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )
    Surface(
        modifier = Modifier
            .size(88.dp)
            .scale(scale)
            .shadow(10.dp, CircleShape),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = time.toString(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CustomTimeCard1to1(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )
    Surface(
        modifier = Modifier
            .height(48.dp)
            .width(120.dp)
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "自定义",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainActionButton1to1(text: String, icon: ImageVector, onClick: () -> Unit, isRed: Boolean = false, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(56.dp)
            .width(160.dp)
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        interactionSource = interactionSource
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StopwatchScreen(viewModel: AppViewModel) {
    val stopwatchState by viewModel.stopwatchState.collectAsState()
    
    // 动画状态
    val buttonScale by animateFloatAsState(
        targetValue = if (stopwatchState.isRunning) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    LaunchedEffect(stopwatchState.isRunning) {
        if (stopwatchState.isRunning) {
            while (stopwatchState.isRunning) {
                delay(10)
                val currentTime = System.currentTimeMillis()
                val newElapsedTime = currentTime - (stopwatchState.startTime ?: currentTime)
                viewModel.updateStopwatchState(elapsedTime = newElapsedTime)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier
                    .size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景动画
                if (stopwatchState.isRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .alpha(0.1f)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
                
                // 秒表显示
                Text(
                    text = formatStopwatchTime(stopwatchState.elapsedTime),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.scale(buttonScale)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!stopwatchState.isRunning) {
                    Button(
                        onClick = { 
                            viewModel.updateStopwatchState(
                                isRunning = true,
                                startTime = System.currentTimeMillis()
                            )
                        },
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "开始",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.updateStopwatchState(
                                elapsedTime = 0,
                                lapTimes = emptyList()
                            )
                        },
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "重置",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = { 
                            viewModel.updateStopwatchState(
                                isRunning = false,
                                startTime = null
                            )
                            // 记录学习时间
                            viewModel.insertStudyRecord(
                                StudyRecordEntity(
                                    startTime = stopwatchState.startTime ?: System.currentTimeMillis(),
                                    endTime = System.currentTimeMillis(),
                                    duration = stopwatchState.elapsedTime,
                                    type = "stopwatch"
                                )
                            )
                        },
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "停止",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            val currentLapTime = stopwatchState.elapsedTime - (stopwatchState.lapTimes.lastOrNull() ?: 0L)
                            val newLapTimes = stopwatchState.lapTimes + stopwatchState.elapsedTime
                            viewModel.updateStopwatchState(lapTimes = newLapTimes)
                            if (AppState.shortestLapTime == null || currentLapTime < AppState.shortestLapTime!!) {
                                AppState.shortestLapTime = currentLapTime
                            }
                        },
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "计圈",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            if (stopwatchState.lapTimes.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(stopwatchState.lapTimes.size) { index ->
                        val lapTime = if (index == 0) stopwatchState.lapTimes[0] else stopwatchState.lapTimes[index] - stopwatchState.lapTimes[index - 1]
                        val isShortest = lapTime == AppState.shortestLapTime
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isShortest) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "第 ${index + 1} 圈",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = formatStopwatchTime(lapTime),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isShortest) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelector(
    selectedTime: Long,
    onTimeSelected: (Long) -> Unit
) {
    val times = listOf(5L, 10L, 15L, 20L, 25L, 30L, 45L, 60L)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "选择时间",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            times.forEach { time ->
                FilterChip(
                    selected = time == selectedTime,
                    onClick = { onTimeSelected(time) },
                    label = { Text("${time}分钟") }
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun formatStopwatchTime(milliseconds: Long): String {
    val minutes = milliseconds / 60000
    val seconds = (milliseconds % 60000) / 1000
    val millis = (milliseconds % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, millis)
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 应用信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "学习助手",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "版本 1.0.0",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "构建版本 2024031501",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "一个帮助你提高学习效率的应用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 开发者信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "开发者信息",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoRow("开发者", "Sompiler")
                InfoRow("Bilibili", "Simple Compiler")
                InfoRow("GitHub", "github.com/sompiler")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 技术信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "技术信息",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoRow("开发语言", "Kotlin")
                InfoRow("UI框架", "Jetpack Compose")
                InfoRow("最低支持", "Android 8.0")
                InfoRow("目标版本", "Android 14")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 版权信息
        Text(
            text = "© 2024 Sompiler. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ReminderScreen(viewModel: AppViewModel) {
    var showCompleted by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }
    
    // 观察提醒事项
    val reminders by viewModel.allReminders.observeAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "提醒事项",
                style = MaterialTheme.typography.headlineMedium
            )
            Row {
                Switch(
                    checked = showCompleted,
                    onCheckedChange = { showCompleted = it },
                    thumbContent = {
                        Icon(
                            imageVector = if (showCompleted) Icons.Filled.CheckCircle else Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
                Text(
                    text = if (showCompleted) "显示已完成" else "隐藏已完成",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reminders.filter { showCompleted || !it.isCompleted }) { reminder ->
                ReminderItem(
                    reminder = Reminder(
                        id = reminder.id,
                        title = reminder.title,
                        description = reminder.description,
                        isCompleted = reminder.isCompleted,
                        isGoal = reminder.isGoal,
                        createdAt = reminder.createdAt
                    ),
                    onToggleComplete = { isCompleted ->
                        viewModel.updateReminder(reminder.copy(isCompleted = isCompleted))
                    },
                    onDelete = {
                        viewModel.deleteReminder(reminder)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { showInput = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加提醒")
        }
    }
    
    if (showInput) {
        AddReminderDialog(
            onDismiss = { showInput = false },
            onAdd = { title, description, isGoal ->
                viewModel.insertReminder(
                    ReminderEntity(
                        title = title,
                        description = description,
                        isCompleted = false,
                        isGoal = isGoal,
                        createdAt = System.currentTimeMillis()
                    )
                )
                showInput = false
            }
        )
    }
}

@Composable
fun QuestionItem(
    question: Question,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (question.subject.isNotBlank()) {
                        Text(
                            text = question.subject,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "答案：${question.answer}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "收起" else "查看答案",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier.rotate(rotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionScreen(viewModel: AppViewModel) {
    var showInput by remember { mutableStateOf(false) }
    
    // 观察错题记录
    val questions by viewModel.allQuestions.observeAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "错题本",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(questions) { question ->
                QuestionItem(
                    question = Question(
                        id = question.id,
                        subject = question.subject,
                        question = question.question,
                        answer = question.answer
                    ),
                    onDelete = {
                        viewModel.deleteQuestion(question)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { showInput = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加错题")
        }
    }
    
    if (showInput) {
        AddQuestionDialog(
            onDismiss = { showInput = false },
            onAdd = { subject, question, answer ->
                viewModel.insertQuestion(
                    QuestionEntity(
                        subject = subject,
                        question = question,
                        answer = answer
                    )
                )
                showInput = false
            }
        )
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onToggleComplete: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var isChecked by remember { mutableStateOf(reminder.isCompleted) }
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isChecked = !isChecked
                        onToggleComplete(isChecked)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                        contentDescription = if (isChecked) "已完成" else "未完成",
                        tint = if (isChecked) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isChecked) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    
    // 添加动画状态
    val slideInOffset = remember { Animatable(1000f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            slideInOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, slideInOffset.value.toInt()) }
            .alpha(alpha.value),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "添加错题",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("科目") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("题目") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("答案") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onAdd(subject, question, answer)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "添加",
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isGoal by remember { mutableStateOf(false) }
    
    // 添加动画状态
    val slideInOffset = remember { Animatable(1000f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            slideInOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, slideInOffset.value.toInt()) }
            .alpha(alpha.value),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "添加提醒",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述（可选）") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "是否为学习目标",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isGoal,
                    onCheckedChange = { isGoal = it },
                    thumbContent = {
                        Icon(
                            imageVector = if (isGoal) Icons.Filled.CheckCircle else Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title, description, isGoal)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "添加",
                    fontSize = 20.sp
                )
            }
        }
    }
}

data class Reminder(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isGoal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Question(
    val id: Int,
    val subject: String,
    val question: String,
    val answer: String
)

data class StudyRecord(
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val title: String = "",
    val description: String = ""
)

private fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return when {
        hours > 0 -> String.format("%d小时%02d分%02d秒", hours, minutes, remainingSeconds)
        minutes > 0 -> String.format("%d分%02d秒", minutes, remainingSeconds)
        else -> String.format("%d秒", remainingSeconds)
    }
}

private fun isToday(timestamp: Long): Boolean {
    val today = java.time.LocalDate.now()
    val recordDate = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return today == recordDate
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val date1 = java.time.Instant.ofEpochMilli(timestamp1)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    val date2 = java.time.Instant.ofEpochMilli(timestamp2)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date1 == date2
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isGoal: Boolean,
    val createdAt: Long
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val question: String,
    val answer: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_records")
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val type: String // "timer" or "stopwatch"
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity)
    
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>
    
    @Insert
    suspend fun insertQuestion(question: QuestionEntity)
    
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
}

@Dao
interface StudyRecordDao {
    @Query("SELECT * FROM study_records ORDER BY startTime DESC")
    fun getAllStudyRecords(): Flow<List<StudyRecordEntity>>
    
    @Insert
    suspend fun insertStudyRecord(record: StudyRecordEntity)
    
    @Delete
    suspend fun deleteStudyRecord(record: StudyRecordEntity)
}

@Database(
    entities = [ReminderEntity::class, QuestionEntity::class, StudyRecordEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun questionDao(): QuestionDao
    abstract fun studyRecordDao(): StudyRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AppRepository(private val database: AppDatabase) {
    val allReminders = database.reminderDao().getAllReminders()
    val allQuestions = database.questionDao().getAllQuestions()
    val allStudyRecords = database.studyRecordDao().getAllStudyRecords()
    
    suspend fun insertReminder(reminder: ReminderEntity) {
        database.reminderDao().insertReminder(reminder)
    }
    
    suspend fun updateReminder(reminder: ReminderEntity) {
        database.reminderDao().updateReminder(reminder)
    }
    
    suspend fun deleteReminder(reminder: ReminderEntity) {
        database.reminderDao().deleteReminder(reminder)
    }
    
    suspend fun insertQuestion(question: QuestionEntity) {
        database.questionDao().insertQuestion(question)
    }
    
    suspend fun deleteQuestion(question: QuestionEntity) {
        database.questionDao().deleteQuestion(question)
    }
    
    suspend fun insertStudyRecord(record: StudyRecordEntity) {
        database.studyRecordDao().insertStudyRecord(record)
    }
    
    suspend fun deleteStudyRecord(record: StudyRecordEntity) {
        database.studyRecordDao().deleteStudyRecord(record)
    }
}

@Composable
fun CustomTimeScreen(
    onDismiss: () -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showHearts by remember { mutableStateOf(false) }
    val maxMinutes = 300000L
    val buttonShape = CircleShape
    val confirmShape = RoundedCornerShape(24.dp)
    val deleteColor = MaterialTheme.colorScheme.errorContainer
    val deleteTextColor = MaterialTheme.colorScheme.onErrorContainer
    val confirmColor = MaterialTheme.colorScheme.primary
    val confirmTextColor = MaterialTheme.colorScheme.onPrimary
    val normalColor = MaterialTheme.colorScheme.secondaryContainer
    val normalTextColor = MaterialTheme.colorScheme.onSecondaryContainer

    // 彩蛋动画（简单实现：弹出爱心）
    val hearts = remember { mutableStateListOf<Int>() }
    LaunchedEffect(showHearts) {
        if (showHearts) {
            repeat(20) {
                hearts.add(it)
                delay(80)
            }
            delay(1200)
            showHearts = false
            hearts.clear()
        }
    }
    LaunchedEffect(input) {
        if (input == "251024") showHearts = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 彩蛋动画
            hearts.forEach { idx ->
                val x = Random.nextInt(0, 300)
                val y = Random.nextInt(0, 600)
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .offset { IntOffset(x, y) }
                        .size(32.dp)
                        .alpha(0.7f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(32.dp))
                    }
                    Text("自定义时间", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(48.dp))
                }
                // 时间显示（无冒号，直接显示输入数字，前导补零）
                Text(
                    text = input.padStart(6, '0'),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                // 拨号盘
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dialPad = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("删除", "0", "确定")
                    )
                    dialPad.forEachIndexed { rowIdx, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { colIdx, label ->
                                when (label) {
                                    "删除" -> Button(
                                        onClick = { if (input.isNotEmpty()) input = input.dropLast(1) },
                                        modifier = Modifier.size(72.dp),
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = deleteColor)
                                    ) {
                                        Icon(Icons.Filled.Backspace, contentDescription = "删除", tint = deleteTextColor)
                                    }
                                    "确定" -> Button(
                                        onClick = {
                                            val minutes = input.toLongOrNull() ?: 0L
                                            if (minutes in 1..maxMinutes) onTimeSelected(minutes)
                                        },
                                        modifier = Modifier.width(80.dp).height(72.dp),
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                                    ) {
                                        Text(
                                            "确定",
                                            color = confirmTextColor,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                    else -> Button(
                                        onClick = {
                                            if (input.length < 6) {
                                                val newInput = input + label
                                                val minutes = newInput.toLongOrNull() ?: 0L
                                                if (minutes <= maxMinutes) input = newInput
                                            }
                                        },
                                        modifier = Modifier.size(72.dp),
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = normalColor)
                                    ) { Text(label, color = normalTextColor, fontSize = 18.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 历史记录窗口内容
@Composable
fun HistoryWindow(viewModel: AppViewModel, onClose: () -> Unit) {
    val records by viewModel.allStudyRecords.observeAsState(initial = emptyList())
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("历史记录", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(records) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDateTime(record.startTime), fontSize = 14.sp)
                        Text(formatDuration(record.duration / 1000), fontSize = 14.sp)
                        Text(if (record.type == "timer") "计时" else "秒表", fontSize = 14.sp)
                    }
                    Divider()
                }
            }
        }
    }
}

// 学习时长窗口内容
@Composable
fun DurationWindow(viewModel: AppViewModel, onClose: () -> Unit) {
    val records by viewModel.allStudyRecords.observeAsState(initial = emptyList())
    val now = System.currentTimeMillis()
    val today = records.filter { isToday(it.startTime) }
    val week = records.filter {
        val cal1 = Calendar.getInstance().apply { timeInMillis = it.startTime }
        val cal2 = Calendar.getInstance()
        cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR) &&
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
    val total = records
    fun sumDuration(list: List<StudyRecordEntity>) = list.sumOf { it.duration } / 1000
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("学习时长记录", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("今日学习时长：${formatDuration(sumDuration(today))}", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("本周学习时长：${formatDuration(sumDuration(week))}", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("累计学习时长：${formatDuration(sumDuration(total))}", fontSize = 18.sp)
        }
    }
}