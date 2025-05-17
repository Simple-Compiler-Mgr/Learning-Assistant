package com.sompiler.lass.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null
    
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState
    
    private val _stopwatchState = MutableStateFlow<StopwatchState>(StopwatchState(0L, false))
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                startTimer(duration)
            }
            ACTION_STOP_TIMER -> stopTimer()
            ACTION_START_STOPWATCH -> startStopwatch()
            ACTION_STOP_STOPWATCH -> stopStopwatch()
            ACTION_RESET_STOPWATCH -> resetStopwatch()
        }
        return START_STICKY
    }
    
    private fun startTimer(duration: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            _timerState.value = TimerState.Running(duration)
            var remainingTime = duration
            
            while (remainingTime > 0) {
                delay(1000)
                remainingTime -= 1000
                _timerState.value = TimerState.Running(remainingTime)
            }
            
            _timerState.value = TimerState.Completed
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.Idle
    }
    
    private fun startStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = serviceScope.launch {
            var elapsedTime = _stopwatchState.value.elapsedTime
            _stopwatchState.value = StopwatchState(elapsedTime, true)
            
            while (_stopwatchState.value.isRunning) {
                delay(10)
                elapsedTime += 10
                _stopwatchState.value = StopwatchState(elapsedTime, true)
            }
        }
    }
    
    private fun stopStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = false)
    }
    
    private fun resetStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchState.value = StopwatchState(0L, false)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        stopwatchJob?.cancel()
    }
    
    sealed class TimerState {
        object Idle : TimerState()
        data class Running(val remainingTime: Long) : TimerState()
        object Completed : TimerState()
    }
    
    data class StopwatchState(
        val elapsedTime: Long,
        val isRunning: Boolean
    )
    
    companion object {
        const val ACTION_START_TIMER = "com.sompiler.lass.ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "com.sompiler.lass.ACTION_STOP_TIMER"
        const val ACTION_START_STOPWATCH = "com.sompiler.lass.ACTION_START_STOPWATCH"
        const val ACTION_STOP_STOPWATCH = "com.sompiler.lass.ACTION_STOP_STOPWATCH"
        const val ACTION_RESET_STOPWATCH = "com.sompiler.lass.ACTION_RESET_STOPWATCH"
        const val EXTRA_DURATION = "extra_duration"
    }
} 