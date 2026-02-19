package com.willitfit.ui.placement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.willitfit.ar.ARSessionManager
import com.willitfit.data.model.PlaneAlignment
import com.willitfit.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlacementViewModel(application: Application) : AndroidViewModel(application) {

    private var arSessionManager: ARSessionManager? = null

    // State
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState

    private val _hasPlacedProduct = MutableStateFlow(false)
    val hasPlacedProduct: StateFlow<Boolean> = _hasPlacedProduct

    private val _isOnWall = MutableStateFlow(false)
    val isOnWall: StateFlow<Boolean> = _isOnWall

    private var product: Product? = null

    fun initialize(product: Product) {
        this.product = product
    }

    fun initializeAR(arSceneView: ArSceneView) {
        arSessionManager = ARSessionManager(getApplication(), viewModelScope).apply {
            initialize(arSceneView)

            viewModelScope.launch {
                trackingState.collect { state ->
                    _trackingState.value = state
                }
            }
        }
    }

    fun handleTap(screenX: Float, screenY: Float) {
        val arManager = arSessionManager ?: return
        val currentProduct = product ?: return

        if (_trackingState.value != TrackingState.TRACKING) return

        // Determine required alignment based on product
        val alignment = if (currentProduct.canMountOnWall) {
            // TVs can go on wall or floor
            PlaneAlignment.ALL
        } else {
            // Appliances/furniture must go on floor (horizontal)
            PlaneAlignment.HORIZONTAL
        }

        val hitResult = arManager.performPriorityHitTest(screenX, screenY, alignment) ?: return

        // Enforce placement rules
        val isVerticalHit = isVerticalPlaneHit(hitResult)

        // Non-TV products cannot be placed on walls
        if (!currentProduct.canMountOnWall && isVerticalHit) {
            // Ignore this tap - product can't go on wall
            return
        }

        if (_hasPlacedProduct.value) {
            // Move existing product (maintain same orientation)
            arManager.moveProductBox(hitResult)
        } else {
            // Place new product
            val onWall = isVerticalHit && currentProduct.canMountOnWall
            arManager.placeProductBox(hitResult, currentProduct, onWall)
            _hasPlacedProduct.value = true
            _isOnWall.value = onWall
        }
    }

    private fun isVerticalPlaneHit(hitResult: HitResult): Boolean {
        val trackable = hitResult.trackable
        if (trackable is com.google.ar.core.Plane) {
            return trackable.type == com.google.ar.core.Plane.Type.VERTICAL
        }
        return false
    }

    fun rotateLeft() {
        arSessionManager?.rotateProductBox(-45f)
    }

    fun rotateRight() {
        arSessionManager?.rotateProductBox(45f)
    }

    fun removeProduct() {
        arSessionManager?.removeProductBox()
        _hasPlacedProduct.value = false
        _isOnWall.value = false
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
}
