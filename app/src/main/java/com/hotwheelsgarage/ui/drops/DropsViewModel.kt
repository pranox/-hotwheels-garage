package com.hotwheelsgarage.ui.drops

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.alerts.AlertScheduler
import com.hotwheelsgarage.alerts.DropChecker
import com.hotwheelsgarage.data.DropEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DropsViewModel(app: Application) : AndroidViewModel(app) {
    private val garage = app as HotWheelsApp
    val settings = garage.settings

    val events: StateFlow<List<DropEvent>?> = garage.database.dropDao().observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val checking = MutableStateFlow(false)
    val status = MutableStateFlow<String?>(null)
    val enabled = MutableStateFlow(settings.alertsEnabled)

    fun setEnabled(value: Boolean) {
        settings.alertsEnabled = value
        enabled.value = value
        AlertScheduler.apply(garage)
        if (value) checkNow()
    }

    fun checkNow() {
        if (checking.value) return
        checking.value = true
        viewModelScope.launch {
            val result = DropChecker(garage).check()
            status.value = buildString {
                append(
                    when {
                        result.alerts > 0 -> "${result.alerts} new alert(s)"
                        result.newPosts > 0 -> "${result.newPosts} new post(s), nothing on your list"
                        else -> "Up to date"
                    }
                )
                if (result.errors.isNotEmpty()) append(". Couldn't reach: ${result.errors.joinToString { it.substringBefore(':') }}")
            }
            checking.value = false
        }
    }
}
