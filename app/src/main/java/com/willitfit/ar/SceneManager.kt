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
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.willitfit.data.model.Product
import java.util.concurrent.CompletableFuture

class SceneManager {

    private var productAnchor: Anchor? = null
    private var productNode: AnchorNode? = null
    private var boxNode: Node? = null
    private var currentRotation = 0f
    private var currentHeightM = 0f

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
        currentHeightM = heightM

        // Create materials
        val fillMaterialFuture = MaterialFactory.makeTransparentWithColor(
            context,
            Color(0.2f, 0.4f, 0.8f, 0.3f)  // More transparent fill
        )
        val edgeMaterialFuture = MaterialFactory.makeOpaqueWithColor(
            context,
            Color(1.0f, 1.0f, 1.0f, 1.0f)  // White edges
        )

        CompletableFuture.allOf(fillMaterialFuture, edgeMaterialFuture).thenAccept {
            val fillMaterial = fillMaterialFuture.get()
            val edgeMaterial = edgeMaterialFuture.get()

            // Create main box
            val box = ShapeFactory.makeCube(
                Vector3(widthM, heightM, depthM),
                Vector3.zero(),
                fillMaterial
            )

            val anchorNode = AnchorNode(anchor).apply {
                setParent(sceneView.scene)
            }

            val node = Node().apply {
                renderable = box
                setParent(anchorNode)

                if (onWall) {
                    localRotation = Quaternion.axisAngle(Vector3.right(), 90f)
                    localPosition = Vector3(0f, heightM / 2, 0f)
                } else {
                    // Floor placement - no Y offset (anchor is at floor level)
                    localPosition = Vector3.zero()
                }
            }

            // Add wireframe edges
            addWireframeEdges(context, node, widthM, heightM, depthM, edgeMaterial)

            productNode = anchorNode
            boxNode = node
        }
    }

    private fun addWireframeEdges(
        context: Context,
        parentNode: Node,
        width: Float,
        height: Float,
        depth: Float,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        val edgeThickness = 0.006f  // 6mm thick edges
        val halfW = width / 2
        val halfH = height / 2
        val halfD = depth / 2

        // Bottom edges (4)
        addEdge(context, parentNode, Vector3(-halfW, -halfH, -halfD), Vector3(halfW, -halfH, -halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(-halfW, -halfH, halfD), Vector3(halfW, -halfH, halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(-halfW, -halfH, -halfD), Vector3(-halfW, -halfH, halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(halfW, -halfH, -halfD), Vector3(halfW, -halfH, halfD), edgeThickness, material)

        // Top edges (4)
        addEdge(context, parentNode, Vector3(-halfW, halfH, -halfD), Vector3(halfW, halfH, -halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(-halfW, halfH, halfD), Vector3(halfW, halfH, halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(-halfW, halfH, -halfD), Vector3(-halfW, halfH, halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(halfW, halfH, -halfD), Vector3(halfW, halfH, halfD), edgeThickness, material)

        // Vertical edges (4)
        addEdge(context, parentNode, Vector3(-halfW, -halfH, -halfD), Vector3(-halfW, halfH, -halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(halfW, -halfH, -halfD), Vector3(halfW, halfH, -halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(-halfW, -halfH, halfD), Vector3(-halfW, halfH, halfD), edgeThickness, material)
        addEdge(context, parentNode, Vector3(halfW, -halfH, halfD), Vector3(halfW, halfH, halfD), edgeThickness, material)
    }

    private fun addEdge(
        context: Context,
        parentNode: Node,
        start: Vector3,
        end: Vector3,
        thickness: Float,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        val direction = Vector3.subtract(end, start)
        val length = direction.length()
        if (length < 0.001f) return

        // Create thin box for edge
        val edge = ShapeFactory.makeCube(
            Vector3(thickness, length, thickness),
            Vector3.zero(),
            material
        )

        val edgeNode = Node().apply {
            renderable = edge
            setParent(parentNode)

            // Position at midpoint
            val midpoint = Vector3.add(start, end).scaled(0.5f)
            localPosition = midpoint

            // Rotate to align with direction
            val up = Vector3.up()
            val normalizedDir = direction.normalized()

            // Calculate rotation from up to direction
            if (kotlin.math.abs(Vector3.dot(up, normalizedDir)) < 0.999f) {
                localRotation = Quaternion.lookRotation(normalizedDir, Vector3.forward())
                // Adjust rotation since box is Y-aligned
                localRotation = Quaternion.multiply(
                    localRotation,
                    Quaternion.axisAngle(Vector3.right(), 90f)
                )
            }
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

    fun getBoxWorldPosition(): Vector3? = boxNode?.worldPosition
}
