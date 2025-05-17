package com.sompiler.lass.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sompiler.lass.AppRepository
import com.sompiler.lass.StudyRecordEntity
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private var repository: AppRepository? = null
    
    fun setRepository(repo: AppRepository) {
        repository = repo
    }
    
    fun insertStudyRecord(record: StudyRecordEntity) {
        viewModelScope.launch {
            repository?.insertStudyRecord(record)
        }
    }
} 