# 学习助手 (Learning Assistant)

一个功能强大的Android学习辅助应用，帮助用户提高学习效率，管理学习进度。

## 功能特点

### 1. 专注计时
- 智能倒计时功能
- 自定义时间设置
- 动态显示效果
- 学习时间统计

### 2. 精确秒表
- 毫秒级精确计时
- 分段记录功能
- 自动保存最佳记录
- 实时累计显示

### 3. 错题管理
- 分类整理系统
- 详细记录功能
- 快速查看复习
- 持续积累提升

### 4. 学习提醒
- 目标计划管理
- 灵活追踪系统
- 完成标记功能
- 进度可视化

## 技术架构

### 前端技术栈
- Jetpack Compose：现代化UI框架
- Material Design 3：设计规范
- Kotlin Coroutines：异步处理
- ViewModel：状态管理
- LiveData：数据观察
- Room：本地数据库

### 后端技术栈
- Room Database：本地数据存储
- WorkManager：后台任务处理
- DataStore：数据持久化
- Hilt：依赖注入

## 技术难点解析

### 1. 计时器实现
```kotlin
// 使用协程实现精确计时
private fun startTimer() {
    viewModelScope.launch {
        while (isRunning) {
            delay(10) // 10ms精度
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - startTime
            _elapsedTime.value = elapsed
        }
    }
}
```

### 2. 数据持久化
```kotlin
// Room数据库实体定义
@Entity(tableName = "study_records")
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val type: String
)

// 数据访问对象
@Dao
interface StudyRecordDao {
    @Query("SELECT * FROM study_records WHERE date(startTime/1000, 'unixepoch') = date('now')")
    fun getTodayRecords(): Flow<List<StudyRecordEntity>>
}
```

### 3. 动画实现
```kotlin
// 使用Compose动画API
val infiniteTransition = rememberInfiniteTransition()
val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = LinearEasing)
    )
)
```

### 4. 状态管理
```kotlin
// ViewModel状态管理
class MainViewModel @Inject constructor(
    private val studyRecordDao: StudyRecordDao
) : ViewModel() {
    private val _studyRecords = MutableStateFlow<List<StudyRecordEntity>>(emptyList())
    val studyRecords: StateFlow<List<StudyRecordEntity>> = _studyRecords.asStateFlow()
}
```

## 核心接口说明

### 1. 计时器接口
```kotlin
interface TimerInterface {
    fun startTimer()
    fun pauseTimer()
    fun resetTimer()
    fun getElapsedTime(): Long
}
```

### 2. 数据存储接口
```kotlin
interface DataStorageInterface {
    suspend fun saveStudyRecord(record: StudyRecordEntity)
    suspend fun getStudyRecords(): Flow<List<StudyRecordEntity>>
    suspend fun deleteStudyRecord(id: Long)
}
```

### 3. 提醒系统接口
```kotlin
interface ReminderInterface {
    fun scheduleReminder(reminder: ReminderEntity)
    fun cancelReminder(id: Long)
    fun updateReminder(reminder: ReminderEntity)
}
```

## 项目结构
```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sompiler/
│   │   │           └── lass/
│   │   │               ├── data/
│   │   │               │   ├── dao/
│   │   │               │   ├── entity/
│   │   │               │   └── repository/
│   │   │               ├── ui/
│   │   │               │   ├── components/
│   │   │               │   ├── screens/
│   │   │               │   └── theme/
│   │   │               └── viewmodel/
│   │   └── res/
│   └── test/
└── build.gradle.kts
```

## 性能优化

### 1. 数据库优化
- 使用索引提升查询性能
- 实现数据缓存机制
- 优化数据库迁移策略

### 2. UI性能优化
- 使用LazyColumn实现列表
- 实现图片缓存
- 优化动画性能

### 3. 内存优化
- 使用WeakReference避免内存泄漏
- 实现资源回收机制
- 优化大对象处理

## 测试策略

### 1. 单元测试
```kotlin
@Test
fun testTimerAccuracy() {
    val timer = Timer()
    timer.start()
    Thread.sleep(1000)
    assertEquals(1000, timer.getElapsedTime(), 10)
}
```

### 2. UI测试
```kotlin
@Test
fun testTimerScreen() {
    composeTestRule.setContent {
        TimerScreen()
    }
    composeTestRule.onNodeWithText("开始").performClick()
    composeTestRule.onNodeWithText("暂停").assertExists()
}
```

## 版本历史

### V1.0.0 Beta2
- 优化界面动画效果
- 新增功能模块展示动画
- 添加应用图标
- 优化页面布局

### V1.0.0 Beta1
- 首次发布
- 实现基础功能
- 支持数据本地存储

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License

## 联系方式

- 项目维护者：[Your Name]
- 邮箱：[Your Email]
- GitHub：[Your GitHub] 