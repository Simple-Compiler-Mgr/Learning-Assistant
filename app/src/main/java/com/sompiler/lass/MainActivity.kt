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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sompiler.lass.ui.theme.LearningAssistantTheme
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

object AppState {
    var shortestLapTime: Long? = null
}

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    val allReminders = repository.allReminders.asLiveData()
    val allQuestions = repository.allQuestions.asLiveData()
    val allStudyRecords = repository.allStudyRecords.asLiveData()
    
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            LearningAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
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
    var selectedTime by remember { mutableStateOf(30) }
    var remainingTime by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var showCustomTime by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    
    // 观察学习记录
    val studyRecords by viewModel.allStudyRecords.observeAsState(initial = emptyList())
    
    val presetTimes = listOf(30, 60, 90, 120)
    
    // 添加更多动画状态
    val timeScale by animateFloatAsState(
        targetValue = if (isRunning) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timeScale"
    )
    
    val timeColor by animateColorAsState(
        targetValue = if (isRunning) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "timeColor"
    )
    
    val buttonScale by animateFloatAsState(
        targetValue = if (isRunning) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    // 添加脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // 计算字体大小
    val timeText = formatTime(remainingTime)
    val fontSize = when {
        timeText.length <= 5 -> 96.sp
        timeText.length <= 7 -> 72.sp
        timeText.length <= 9 -> 48.sp
        else -> 36.sp
    }
    
    // 计时器逻辑
    LaunchedEffect(isRunning) {
        if (isRunning && remainingTime > 0) {
            if (startTime == null) {
                startTime = System.currentTimeMillis()
            }
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
                elapsedTime += 1000
            }
            isRunning = false
            // 记录学习时间
            startTime?.let { start ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - start
                // 只记录有效的学习时间（倒计时结束时的实际时间）
                viewModel.insertStudyRecord(
                    StudyRecordEntity(
                        startTime = start,
                        endTime = endTime,
                        duration = duration,
                        type = "timer"
                    )
                )
                startTime = null
            }
        } else if (!isRunning && startTime != null) {
            // 如果中途暂停，不记录学习时间
            startTime = null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 添加背景动画
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f)
                    .scale(pulseScale)
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
        
        AnimatedVisibility(
            visible = showCustomTime,
            enter = fadeIn() + expandVertically() + slideInVertically { it },
            exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
        ) {
            CustomTimeScreen(
                onBack = { showCustomTime = false },
                onTimeSelected = { minutes ->
                    selectedTime = minutes
                    remainingTime = minutes * 60
                    showCustomTime = false
                }
            )
        }
        
        AnimatedVisibility(
            visible = !showCustomTime,
            enter = fadeIn() + expandVertically() + slideInVertically { -it },
            exit = fadeOut() + shrinkVertically() + slideOutVertically { -it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
    Text(
                        text = "计时器",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row {
                        // 历史记录按钮
                        IconButton(
                            onClick = { showHistory = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "历史记录",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 报告按钮
                        IconButton(
                            onClick = { showReport = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = "学习报告",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // 时间显示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = fontSize,
                        color = timeColor,
                        modifier = Modifier.scale(timeScale)
                    )
                }
                
                // 中间控制区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 预设时间按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            presetTimes.forEach { time ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 1.2f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessVeryLow
                                    ),
                                    label = "scale"
                                )
                                
                                val buttonColor by animateColorAsState(
                                    targetValue = if (selectedTime == time && !isRunning) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surface,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "buttonColor"
                                )
                                
                                val textColor by animateColorAsState(
                                    targetValue = if (selectedTime == time && !isRunning) 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "textColor"
                                )
                                
                                Button(
                                    onClick = { 
                                        if (isRunning) {
                                            remainingTime += time * 60
                                        } else {
                                            selectedTime = time
                                            remainingTime = time * 60
                                        }
                                    },
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(80.dp)
                                        .scale(scale),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = buttonColor
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    interactionSource = interactionSource
                                ) {
                                    Text(
                                        text = "${time}",
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 自定义时间按钮
                        val customButtonScale by animateFloatAsState(
                            targetValue = if (isRunning) 0.9f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "customButtonScale"
                        )
                        
                        Button(
                            onClick = { showCustomTime = true },
                            modifier = Modifier
                                .width(120.dp)
                                .height(48.dp)
                                .scale(customButtonScale),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            enabled = !isRunning
                        ) {
                            Text(
                                text = "自定义",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // 控制按钮
                        if (!isRunning && remainingTime == 0) {
                            Button(
                                onClick = { 
                                    remainingTime = selectedTime * 60
                                    isRunning = true
                                },
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "开始",
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "开始计时",
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // 暂停/继续按钮
                                Button(
                                    onClick = { isRunning = !isRunning },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(64.dp)
                                        .scale(buttonScale),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRunning) 
                                            MaterialTheme.colorScheme.error 
                                        else 
                                            MaterialTheme.colorScheme.primary
                                    ),
                                    shape = CircleShape,
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 8.dp,
                                        pressedElevation = 12.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isRunning) "暂停" else "继续",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .graphicsLayer {
                                                if (isRunning) {
                                                    rotationZ = sin(elapsedTime * 0.2f) * 10f
                                                }
                                            }
                                    )
                                }
                                
                                // 重置按钮
                                Button(
                                    onClick = { 
                                        isRunning = false
                                        remainingTime = 0
                                        startTime = null
                                    },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "重置",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 历史记录全屏显示
        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
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
                        Text(
                            text = "学习历史",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(
                            onClick = { showHistory = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(studyRecords.sortedByDescending { it.startTime }) { record ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically() + slideInVertically { it },
                                exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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
                                            text = "开始时间：${formatDateTime(record.startTime)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "结束时间：${formatDateTime(record.endTime)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "学习时长：${formatDuration(record.duration)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 学习报告全屏显示
        AnimatedVisibility(
            visible = showReport,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
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
                        Text(
                            text = "今日学习报告",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(
                            onClick = { showReport = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    val todayRecords = studyRecords.filter {
                        isToday(it.startTime)
                    }
                    val totalDuration = todayRecords.sumOf { it.duration / 1000 }
                    val todayCount = todayRecords.size
                    
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically() + slideInVertically { it },
                        exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = formatDuration(totalDuration),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "今日学习总时长",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
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
                                        text = "今日学习次数：${todayCount}次",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (todayCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "平均每次：${formatDuration(totalDuration / todayCount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTimeScreen(
    onBack: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    // 计算按钮大小和间距
    val buttonSize = with(density) { 80.dp }
    val buttonSpacing = with(density) { 16.dp }
    val screenPadding = with(density) { 16.dp }
    
    // 爱心动画状态
    val hearts = remember { mutableStateListOf<Heart>() }
    val infiniteTransition = rememberInfiniteTransition(label = "hearts")
    
    // 检查彩蛋
    LaunchedEffect(customMinutes) {
        if (customMinutes == "251024") {
            showEasterEgg = true
            // 添加多个爱心
            repeat(20) {
                hearts.add(
                    Heart(
                        x = Random.nextFloat() * 1000,
                        y = 1000f,
                        scale = Random.nextFloat() * 0.5f + 0.5f,
                        speed = Random.nextFloat() * 2f + 1f
                    )
                )
            }
        }
    }
    
    // 更新爱心位置
    LaunchedEffect(showEasterEgg) {
        if (showEasterEgg) {
            while (true) {
                delay(16)
                hearts.forEachIndexed { index, heart ->
                    hearts[index] = heart.copy(
                        y = heart.y - heart.speed,
                        x = heart.x + sin(heart.y * 0.01f) * 2f
                    )
                }
                // 移除超出屏幕的爱心
                hearts.removeAll { it.y < -100 }
                // 添加新的爱心
                if (hearts.size < 20) {
                    hearts.add(
                        Heart(
                            x = Random.nextFloat() * 1000,
                            y = 1000f,
                            scale = Random.nextFloat() * 0.5f + 0.5f,
                            speed = Random.nextFloat() * 2f + 1f
                        )
                    )
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 绘制爱心
        if (showEasterEgg) {
            hearts.forEach { heart ->
                Text(
                    text = "❤",
                    modifier = Modifier
                        .offset { IntOffset(heart.x.toInt(), heart.y.toInt()) }
                        .scale(heart.scale),
                    color = Color.Red,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "自定义时间",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 确定按钮上移
            Button(
                onClick = {
                    val minutes = customMinutes.toIntOrNull() ?: 0
                    if (minutes > 0 && minutes <= 300000) {
                        onTimeSelected(minutes)
                    }
                },
                modifier = Modifier
                    .width(buttonSize * 2)
                    .height(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "确定",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "当前输入：${customMinutes}分钟",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (showError) {
                Text(
                    text = "时间不能超过300000分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 数字键盘
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "删除")
            )
            
            numbers.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { number ->
                        if (number.isNotEmpty()) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    if (number == "删除") {
                                        customMinutes = customMinutes.dropLast(1)
                                        showError = false
                                    } else {
                                        val newValue = customMinutes + number
                                        if (newValue.toIntOrNull() ?: 0 <= 300000) {
                                            customMinutes = newValue
                                            showError = false
                                        } else {
                                            showError = true
                                        }
                                    }
                                },
                                modifier = Modifier.size(buttonSize),
                                shape = CircleShape,
                                containerColor = if (number == "删除") 
                                    MaterialTheme.colorScheme.errorContainer 
                                else 
                                    MaterialTheme.colorScheme.surface,
                                contentColor = if (number == "删除")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 12.dp
                                )
                            ) {
                                Text(
                                    text = if (number == "删除") "⌫" else number,
                                    fontSize = 24.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(buttonSize))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(buttonSpacing))
            }
        }
    }
}

// 爱心数据类
data class Heart(
    val x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float
)

@Composable
fun StopwatchScreen(viewModel: AppViewModel) {
    var elapsedTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var lapTimes by remember { mutableStateOf(listOf<Long>()) }
    var showHistory by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    
    // 观察学习记录
    val studyRecords by viewModel.allStudyRecords.observeAsState(initial = emptyList())
    
    // 添加动画状态
    val startButtonScale by animateFloatAsState(
        targetValue = if (isRunning) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "startButtonScale"
    )
    
    val lapButtonScale by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lapButtonScale"
    )
    
    val resetButtonScale by animateFloatAsState(
        targetValue = if (elapsedTime > 0) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "resetButtonScale"
    )
    
    // 添加更多动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val progressRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    LaunchedEffect(isRunning) {
        if (isRunning) {
            if (startTime == null) {
                startTime = System.currentTimeMillis()
            }
            while (true) {
                delay(10)
                elapsedTime += 10
            }
        } else if (startTime != null) {
            // 记录学习时间
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime!!
            viewModel.insertStudyRecord(
                StudyRecordEntity(
                    startTime = startTime!!,
                    endTime = endTime,
                    duration = duration,
                    type = "stopwatch"
                )
            )
            startTime = null
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "秒表",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = { showHistory = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "历史记录",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 修改环形进度条和时间显示
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景环形
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp
                )
                
                // 进度环形
                CircularProgressIndicator(
                    progress = (elapsedTime % 60000) / 60000f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer {
                            rotationZ = progressRotation
                        },
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp
                )
                
                // 时间显示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        if (isRunning) {
                            rotationZ = sin(elapsedTime * 0.1f) * 5f
                        }
                    }
                ) {
                    Text(
                        text = formatStopwatchMinutes(elapsedTime),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatStopwatchSeconds(elapsedTime),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 修改控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 开始/暂停按钮
                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .width(120.dp)
                        .height(64.dp)
                        .scale(startButtonScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    ),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "暂停" else "开始",
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                if (isRunning) {
                                    rotationZ = sin(elapsedTime * 0.2f) * 10f
                                }
                            }
                    )
                }
                
                // 计圈按钮
                Button(
                    onClick = { 
                        if (isRunning) {
                            lapTimes = lapTimes + elapsedTime
                            // 更新最短记录
                            val currentLapTime = if (lapTimes.size > 1) {
                                elapsedTime - lapTimes[lapTimes.size - 2]
                            } else {
                                elapsedTime
                            }
                            if (AppState.shortestLapTime == null || currentLapTime < AppState.shortestLapTime!!) {
                                AppState.shortestLapTime = currentLapTime
                            }
                        }
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .height(64.dp)
                        .scale(lapButtonScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = CircleShape,
                    enabled = isRunning
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = "计圈",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // 重置按钮
                Button(
                    onClick = { 
                        isRunning = false
                        elapsedTime = 0
                        lapTimes = emptyList()
                        AppState.shortestLapTime = null
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .height(64.dp)
                        .scale(resetButtonScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "重置",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 计圈列表
            AnimatedVisibility(
                visible = lapTimes.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Text(
                        text = "记录",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(lapTimes.size) { index ->
                            val lapTime = lapTimes[index]
                            val previousLapTime = if (index > 0) lapTimes[index - 1] else 0
                            val splitTime = lapTime - previousLapTime
                            
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically() + slideInVertically { it },
                                exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
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
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatStopwatchTime(splitTime),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = formatStopwatchTime(lapTime),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 历史记录全屏显示
        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
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
                        Text(
                            text = "历史记录",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(
                            onClick = { showHistory = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(lapTimes.size) { index ->
                            val lapTime = lapTimes[index]
                            val previousLapTime = if (index > 0) lapTimes[index - 1] else 0
                            val splitTime = lapTime - previousLapTime
                            
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically() + slideInVertically { it },
                                exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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
                                            text = "记录 ${index + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "分段用时：${formatStopwatchTime(splitTime)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "累计用时：${formatStopwatchTime(lapTime)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatStopwatchMinutes(timeMillis: Long): String {
    val minutes = (timeMillis % 3600000) / 60000
    return String.format("%02d", minutes)
}

private fun formatStopwatchSeconds(timeMillis: Long): String {
    val seconds = (timeMillis % 60000) / 1000
    val milliseconds = (timeMillis % 1000) / 10
    return String.format("%02d.%02d", seconds, milliseconds)
}

private fun formatStopwatchTime(timeMillis: Long): String {
    val hours = timeMillis / 3600000
    val minutes = (timeMillis % 3600000) / 60000
    val seconds = (timeMillis % 60000) / 1000
    val milliseconds = (timeMillis % 1000) / 10
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds)
    } else {
        String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
    }
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

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}