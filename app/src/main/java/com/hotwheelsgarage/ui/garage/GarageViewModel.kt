package com.hotwheelsgarage.ui.garage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.data.Car
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GarageViewModel(app: Application) : AndroidViewModel(app) {
    val cars: StateFlow<List<Car>?> = (app as HotWheelsApp).repository.cars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class CarDetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = (app as HotWheelsApp).repository
    val wiki = (app as HotWheelsApp).wiki
    private val id: Long = checkNotNull(handle["id"])

    val car: StateFlow<Car?> = repository.observe(id).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(car: Car) = viewModelScope.launch { repository.update(car) }

    fun delete(car: Car, onDone: () -> Unit) = viewModelScope.launch {
        repository.delete(car)
        onDone()
    }
}
