package com.willitfit.ui.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.willitfit.ar.ARSessionManager
import com.willitfit.data.model.DoorMeasurement
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.MeasurementStep
import com.willitfit.data.model.Product
import com.willitfit.data.model.SamplingResult
import com.willitfit.data.model.SamplingState
import com.willitfit.data.model.SpaceMeasurement
import com.willitfit.data.model.Verdict
import com.willitfit.service.MeasurementFlowController
import com.willitfit.service.VerdictEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private var arSessionManager: ARSessionManager? = null
    private val flowController = MeasurementFlowController()

    // State from flow controller
    val currentStep: StateFlow<MeasurementStep> = flowController.currentStep
    val doorMeasurement: StateFlow<DoorMeasurement> = flowController.doorMeasurement
    val spaceMeasurement: StateFlow<SpaceMeasurement> = flowController.spaceMeasurement
    val isComplete: StateFlow<Boolean> = flowController.isComplete
    val yConsistencyError: StateFlow<String?> = flowController.yConsistencyError

    // AR state
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState

    private val _trackingConfidence = MutableStateFlow(0.0)
    val trackingConfidence: StateFlow<Double> = _trackingConfidence

    private val _samplingState = MutableStateFlow<SamplingState>(SamplingState.Idle)
    val samplingState: StateFlow<SamplingState> = _samplingState

    // Product and mode
    private var product: Product? = null
    private var mode: MeasurementMode = MeasurementMode.DOOR

    // Verdict
    private val _verdict = MutableStateFlow<Verdict?>(null)
    val verdict: StateFlow<Verdict?> = _verdict

    fun initialize(product: Product, mode: MeasurementMode) {
        this.product = product
        this.mode = mode
        flowController.startFlow(mode)

        // Watch for completion
        viewModelScope.launch {
            isComplete.collect { complete ->
                if (complete) {
                    calculateVerdict()
                }
            }
        }
    }

    fun initializeAR(arSceneView: ArSceneView) {
        arSessionManager = ARSessionManager(getApplication(), viewModelScope).apply {
            initialize(arSceneView)

            // Connect state flows
            viewModelScope.launch {
                trackingState.collect { state ->
                    _trackingState.value = state
                }
            }

            viewModelScope.launch {
                trackingConfidence.collect { confidence ->
                    _trackingConfidence.value = confidence
                }
            }

            // Set up sampling callbacks
            onSamplingComplete = { result ->
                handleSamplingComplete(result)
            }

            onSamplingFailed = { reason ->
                _samplingState.value = SamplingState.Failed(reason)
            }

            onSamplingProgress = { progress ->
                _samplingState.value = SamplingState.Sampling(progress)
            }
        }
    }

    fun handleTap(screenX: Float, screenY: Float) {
        val arManager = arSessionManager ?: return

        // Don't process taps if already sampling or complete
        if (_samplingState.value is SamplingState.Sampling) return
        if (currentStep.value is MeasurementStep.Complete) return

        // Don't process if not tracking
        if (_trackingState.value != TrackingState.TRACKING) return

        // Clear any previous Y-consistency error
        flowController.clearYConsistencyError()

        // Start sampling at tap location with plane requirement
        val step = currentStep.value
        _samplingState.value = SamplingState.Sampling(0.0)
        arManager.startSampling(screenX, screenY, step.alignment, step.requiresPlaneHit)
    }

    private fun handleSamplingComplete(result: SamplingResult) {
        flowController.handleSamplingComplete(result, _trackingConfidence.value)

        // Check if Y-consistency error occurred
        val yError = flowController.yConsistencyError.value
        if (yError != null) {
            _samplingState.value = SamplingState.Failed(yError)
        } else {
            _samplingState.value = SamplingState.Idle
        }
    }

    fun retryCurrentPoint() {
        _samplingState.value = SamplingState.Idle
        flowController.retryCurrentPoint()
    }

    fun cancelSampling() {
        arSessionManager?.cancelSampling()
        _samplingState.value = SamplingState.Idle
    }

    private fun calculateVerdict() {
        val currentProduct = product ?: return

        _verdict.value = when (mode) {
            MeasurementMode.DOOR -> {
                VerdictEngine.verdictForDoor(doorMeasurement.value, currentProduct)
            }
            MeasurementMode.SPACE -> {
                VerdictEngine.verdictForSpace(spaceMeasurement.value, currentProduct)
            }
            MeasurementMode.VIRTUAL_PLACEMENT -> {
                // Virtual placement doesn't have a verdict
                null
            }
        }
    }

    fun resetMeasurement() {
        flowController.reset()
        flowController.startFlow(mode)
        _verdict.value = null
        _samplingState.value = SamplingState.Idle
    }

    fun onResume() {
        arSessionManager?.resume()
    }

    fun onPause() {
        arSessionManager?.pause()
    }

    override fun onCleared() {
        super.onCleared()
        arSessionManager?.destroy()
        arSessionManager = null
    }

    fun getProduct(): Product? = product
    fun getMode(): MeasurementMode = mode
}
