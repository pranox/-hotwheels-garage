package com.hotwheelsgarage.ui.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.data.AiGuess
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.data.CastingDetails
import com.hotwheelsgarage.data.Release
import com.hotwheelsgarage.scan.Candidate
import com.hotwheelsgarage.scan.ScanHints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Stage { Camera, Working, Results, Confirm }

data class ScanUiState(
    val stage: Stage = Stage.Camera,
    val photo: File? = null,
    val message: String = "",
    val hints: ScanHints? = null,
    val aiGuess: AiGuess? = null,
    val notes: List<String> = emptyList(),
    val query: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selected: Candidate? = null,
    val details: CastingDetails? = null,
    val detailsError: String? = null,
    val chosenRelease: Release? = null,
    val isTreasureHunt: Boolean = false,
    val isSuperTreasureHunt: Boolean = false,
    val condition: String = Car.CONDITION_CARDED,
    val rating: Int = 0,
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val garage = app as HotWheelsApp
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    private val photoDir = File(app.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile() = File(photoDir, "car_${System.currentTimeMillis()}.jpg")

    fun onPicked(uri: Uri) = viewModelScope.launch {
        val file = newPhotoFile()
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }.isSuccess
        }
        if (ok) analyze(file) else showError("Couldn't open that photo")
    }

    fun analyze(photo: File) {
        _state.value = ScanUiState(stage = Stage.Working, photo = photo, message = "Reading the photo…")
        viewModelScope.launch {
            try {
                val result = garage.identifier.identify(getApplication(), photo) { msg ->
                    _state.update { it.copy(message = msg) }
                }
                val candidates = withOwnedCounts(result.candidates)
                _state.update {
                    it.copy(
                        stage = Stage.Results,
                        hints = result.hints,
                        aiGuess = result.aiGuess,
                        notes = result.notes,
                        candidates = candidates,
                        query = result.aiGuess?.model ?: result.hints.nameLines.firstOrNull().orEmpty(),
                    )
                }
                // An exact toy-number match is reliable enough to go straight to confirmation.
                candidates.firstOrNull()?.takeIf { it.release != null }?.let(::select)
            } catch (e: Exception) {
                _state.update {
                    it.copy(stage = Stage.Results, notes = listOf("Couldn't read the photo: ${e.message}"))
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(stage = Stage.Working, message = "Searching the wiki for \"$query\"…", query = query) }
        viewModelScope.launch {
            val pages = runCatching { garage.wiki.search(query, 12) }
            val candidates = withOwnedCounts(pages.getOrDefault(emptyList()).map { Candidate(it.title, it.imageUrl, "Search") })
            _state.update {
                it.copy(
                    stage = Stage.Results,
                    candidates = candidates,
                    notes = pages.exceptionOrNull()?.let { e -> listOf("Search failed: ${e.message}. Check your internet connection.") }.orEmpty(),
                )
            }
        }
    }

    fun select(candidate: Candidate) {
        val s = _state.value
        val release = candidate.release
        _state.update {
            it.copy(
                stage = Stage.Confirm,
                selected = candidate,
                details = null,
                detailsError = null,
                chosenRelease = release,
                isTreasureHunt = release?.isTreasureHunt ?: (s.aiGuess?.isTreasureHunt == true),
                isSuperTreasureHunt = release?.isSuperTreasureHunt ?: (s.aiGuess?.isSuperTreasureHunt == true),
            )
        }
        viewModelScope.launch {
            runCatching { garage.wiki.casting(candidate.title) }
                .onSuccess { details ->
                    _state.update { st ->
                        if (st.selected != candidate) return@update st
                        // Prefer the casting page's own row for the matched toy number.
                        val match = release?.toyNumber?.let { code -> details.versions.firstOrNull { it.toyNumber == code } }
                        st.copy(details = details, chosenRelease = match ?: st.chosenRelease)
                    }
                }
                .onFailure { e -> _state.update { it.copy(detailsError = e.message) } }
        }
    }

    fun chooseRelease(release: Release?) = _state.update {
        it.copy(
            chosenRelease = release,
            isTreasureHunt = release?.isTreasureHunt ?: it.isTreasureHunt,
            isSuperTreasureHunt = release?.isSuperTreasureHunt ?: it.isSuperTreasureHunt,
        )
    }

    fun setTreasureHunt(value: Boolean) = _state.update { it.copy(isTreasureHunt = value, isSuperTreasureHunt = it.isSuperTreasureHunt && !value) }
    fun setSuperTreasureHunt(value: Boolean) = _state.update { it.copy(isSuperTreasureHunt = value, isTreasureHunt = it.isTreasureHunt && !value) }
    fun setCondition(value: String) = _state.update { it.copy(condition = value) }
    fun setRating(value: Int) = _state.update { it.copy(rating = value) }

    fun save(wishlist: Boolean, onSaved: () -> Unit) {
        val s = _state.value
        val candidate = s.selected ?: return
        val d = s.details
        val r = s.chosenRelease
        val car = Car(
            name = d?.name ?: candidate.title,
            wikiTitle = candidate.title,
            series = r?.series ?: d?.firstSeries,
            year = r?.year ?: s.aiGuess?.year,
            color = r?.color ?: s.aiGuess?.color,
            toyNumber = r?.toyNumber ?: s.hints?.toyCodes?.firstOrNull(),
            collectorNumber = r?.collectorNumber ?: s.hints?.collectorNumber,
            imageUrl = r?.imageUrl ?: d?.imageUrl ?: candidate.imageUrl,
            photoPath = s.photo?.path,
            designer = d?.designer,
            description = d?.description,
            isTreasureHunt = s.isTreasureHunt,
            isSuperTreasureHunt = s.isSuperTreasureHunt,
            isWishlist = wishlist,
            condition = s.condition,
            rating = s.rating,
        )
        viewModelScope.launch {
            garage.repository.addOrIncrement(car)
            _state.value = ScanUiState()
            onSaved()
        }
    }

    fun back() {
        val s = _state.value
        when (s.stage) {
            Stage.Confirm -> _state.update { it.copy(stage = Stage.Results, selected = null, details = null) }
            Stage.Results -> reset()
            else -> Unit
        }
    }

    /** Back to the camera, deleting the unsaved photo. */
    fun reset() {
        _state.value.photo?.delete()
        _state.value = ScanUiState()
    }

    fun showError(message: String) = _state.update { it.copy(stage = Stage.Results, notes = listOf(message)) }

    private suspend fun withOwnedCounts(candidates: List<Candidate>): List<Candidate> {
        val owned = garage.repository.ownedCounts(candidates.map { it.title })
        return candidates.map { it.copy(ownedCount = owned[it.title] ?: 0) }
    }
}
