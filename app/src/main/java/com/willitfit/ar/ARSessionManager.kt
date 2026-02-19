package com.willitfit.ar

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment
import com.willitfit.data.model.MeasurementPoint
import com.willitfit.data.model.PlaneAlignment
import com.willitfit.data.model.Product
import com.willitfit.data.model.RaycastHitType
import com.willitfit.data.model.SamplingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ARSessionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var arSceneView: ArSceneView? = null
    private var session: Session? = null

    private val samplingManager = SamplingManager(this, scope)
    private val sceneManager = SceneManager()
    private val raycastHelper = RaycastHelper()

    // State
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState

    private val _trackingConfidence = MutableStateFlow(0.0)
    val trackingConfidence: StateFlow<Double> = _trackingConfidence

    private val _isSampling = MutableStateFlow(false)
    val isSampling: StateFlow<Boolean> = _isSampling

    // Callbacks
    var onSamplingComplete: ((SamplingResult) -> Unit)? = null
    var onSamplingFailed: ((String) -> Unit)? = null
    var onSamplingProgress: ((Double) -> Unit)? = null

    fun initialize(sceneView: ArSceneView) {
        arSceneView = sceneView

        try {
            session = Session(context)
            val config = Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                depthMode = Config.DepthMode.AUTOMATIC
            }
            session?.configure(config)
            sceneView.session = session

            // Set up frame update listener
            sceneView.scene.addOnUpdateListener { frameTime ->
                onFrameUpdate()
            }

            // Configure sampling manager callbacks
            samplingManager.onComplete = { result ->
                _isSampling.value = false
                onSamplingComplete?.invoke(result)
            }
            samplingManager.onFailed = { reason ->
                _isSampling.value = false
                onSamplingFailed?.invoke(reason)
            }
            samplingManager.onProgress = { progress ->
                onSamplingProgress?.invoke(progress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onFrameUpdate() {
        val frame = arSceneView?.arFrame ?: return
        val camera = frame.camera

        _trackingState.value = camera.trackingState

        // Update tracking confidence based on state
        _trackingConfidence.value = when (camera.trackingState) {
            TrackingState.TRACKING -> 1.0
            TrackingState.PAUSED -> 0.5
            TrackingState.STOPPED -> 0.0
        }
    }

    fun performPriorityHitTest(x: Float, y: Float, alignment: PlaneAlignment): HitResult? {
        val frame = arSceneView?.arFrame ?: return null
        return raycastHelper.performPriorityHitTest(frame, x, y, alignment)
    }

    fun performStrictPlaneHitTest(x: Float, y: Float, alignment: PlaneAlignment): HitResult? {
        val frame = arSceneView?.arFrame ?: return null
        return raycastHelper.performStrictPlaneHitTest(frame, x, y, alignment)
    }

    fun getHitResultType(hitResult: HitResult): RaycastHitType {
        return raycastHelper.getHitResultType(hitResult)
    }

    fun startSampling(screenX: Float, screenY: Float, alignment: PlaneAlignment, requirePlane: Boolean = false) {
        if (_isSampling.value) return

        _isSampling.value = true
        samplingManager.startSampling(screenX, screenY, alignment, requirePlane)
    }

    fun cancelSampling() {
        samplingManager.cancel()
        _isSampling.value = false
    }

    fun getFrame(): Frame? = arSceneView?.arFrame

    // Product placement methods
    fun placeProductBox(hitResult: HitResult, product: Product, onWall: Boolean) {
        val sceneView = arSceneView ?: return
        sceneManager.placeProductBox(sceneView, hitResult, product, onWall)
    }

    fun moveProductBox(hitResult: HitResult) {
        sceneManager.moveProductBox(hitResult)
    }

    fun rotateProductBox(degrees: Float) {
        sceneManager.rotateProductBox(degrees)
    }

    fun removeProductBox() {
        sceneManager.removeProductBox()
    }

    fun resume() {
        session?.resume()
        arSceneView?.resume()
    }

    fun pause() {
        arSceneView?.pause()
        session?.pause()
    }

    fun destroy() {
        sceneManager.removeProductBox()
        arSceneView?.destroy()
        session?.close()
        arSceneView = null
        session = null
    }
}
