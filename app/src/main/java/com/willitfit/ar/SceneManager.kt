package com.willitfit.ar

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.willitfit.data.model.Product

class SceneManager {

    private var productAnchor: Anchor? = null
    private var productNode: AnchorNode? = null
    private var boxNode: Node? = null
    private var currentRotation = 0f

    fun placeProductBox(
        sceneView: ArSceneView,
        hitResult: HitResult,
        product: Product,
        onWall: Boolean
    ) {
        removeProductBox()

        val context = sceneView.context
        val anchor = hitResult.createAnchor()
        productAnchor = anchor

        // Convert dimensions from cm to meters
        val widthM = (product.widthCm / 100.0).toFloat()
        val heightM = (product.heightCm / 100.0).toFloat()
        val depthM = (product.depthCm / 100.0).toFloat()

        // Create semi-transparent blue material
        MaterialFactory.makeTransparentWithColor(
            context,
            Color(0.2f, 0.4f, 0.8f, 0.6f)
        ).thenAccept { material ->
            val box = ShapeFactory.makeCube(
                Vector3(widthM, heightM, depthM),
                Vector3.zero(),
                material
            )

            val anchorNode = AnchorNode(anchor).apply {
                setParent(sceneView.scene)
            }

            val node = Node().apply {
                renderable = box
                setParent(anchorNode)

                if (onWall) {
                    // For wall mounting, rotate to face outward
                    localRotation = Quaternion.axisAngle(Vector3.right(), 90f)
                    // Offset so bottom of box is at anchor point
                    localPosition = Vector3(0f, heightM / 2, 0f)
                } else {
                    // For floor placement, offset so bottom touches floor
                    localPosition = Vector3(0f, heightM / 2, 0f)
                }
            }

            productNode = anchorNode
            boxNode = node
        }
    }

    fun moveProductBox(hitResult: HitResult) {
        val currentAnchorNode = productNode ?: return
        val currentBoxNode = boxNode ?: return

        // Remove old anchor
        productAnchor?.detach()

        // Create new anchor at new position
        val newAnchor = hitResult.createAnchor()
        productAnchor = newAnchor

        // Update anchor node
        currentAnchorNode.anchor = newAnchor
    }

    fun rotateProductBox(degrees: Float) {
        val node = boxNode ?: return

        currentRotation += degrees
        currentRotation %= 360f

        // Apply rotation around Y axis
        val currentLocalPos = node.localPosition
        node.localRotation = Quaternion.axisAngle(Vector3.up(), currentRotation)
        node.localPosition = currentLocalPos
    }

    fun removeProductBox() {
        boxNode?.setParent(null)
        boxNode = null

        productNode?.setParent(null)
        productNode = null

        productAnchor?.detach()
        productAnchor = null

        currentRotation = 0f
    }

    fun getProductNode(): AnchorNode? = productNode

    fun getBoxNode(): Node? = boxNode
}
