package pl.complet.QuestSync

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.runtime.SceneMaterial
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.vr.VRFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import pl.complet.QuestSync.service.QuestSnifferService
import pl.complet.QuestSync.ui.spatial.CoachPanel
import pl.complet.QuestSync.ui.spatial.TaglinePanel
import pl.complet.QuestSync.ui.spatial.TelemetryPanel

class MainActivity : AppSystemActivity() {

    private val sceneScope = CoroutineScope(Dispatchers.Main)

    override fun registerFeatures(): List<SpatialFeature> =
        listOf(VRFeature(this), ComposeFeature())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startSniffer()
    }

    override fun onSceneReady() {
        super.onSceneReady()

        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        scene.setLightingEnvironment(
            ambientColor = Vector3(1.0f, 1.0f, 1.0f),
            sunColor = Vector3(8.0f, 8.0f, 8.0f),
            sunDirection = -Vector3(1.0f, 3.0f, -2.0f),
            environmentIntensity = 1.0f,
        )

        val faceViewer = Quaternion(0.0f, 180.0f, 0.0f)
        val figurePose = Pose(Vector3(0.0f, 0.9f, 2.0f), faceViewer)

        fun unlit(asset: String) =
            Mesh("apk:///$asset".toUri()).apply { defaultShaderOverride = SceneMaterial.UNLIT_SHADER }

        fun plexusMat(r: Float, g: Float, b: Float, a: Float) = Material().apply {
            baseColor = Color4(r, g, b, a)
            unlit = true
            alphaMode = 2
            shader = SceneMaterial.UNLIT_SHADER
        }

        // Plexus Configuration - Static Style (No pulsing)
        val plexusConfigs = listOf(
            Triple("plexus_lines.glb", floatArrayOf(0.95f, 0.60f, 0.30f), 0.12f), // Steady dim lines
            Triple("plexus_a.glb", floatArrayOf(1.0f, 0.66f, 0.32f), 0.75f),      // Steady glowing points
            Triple("plexus_b.glb", floatArrayOf(1.0f, 0.72f, 0.40f), 0.75f),
            Triple("plexus_c.glb", floatArrayOf(1.0f, 0.88f, 0.60f), 0.75f)
        )

        for ((asset, color, alpha) in plexusConfigs) {
            Entity.create(
                listOf(
                    Mesh("apk:///$asset".toUri()),
                    plexusMat(color[0], color[1], color[2], alpha),
                    Scale(Vector3(1.2f, 1.2f, 1.2f)),
                    Transform(figurePose),
                )
            )
        }

        // Telemetry markers
        for (p in listOf(Vector3(0.0f, 1.9f, 1.85f), Vector3(0.0f, 1.5f, 1.85f), Vector3(0.35f, 1.6f, 1.85f))) {
            Entity.create(listOf(unlit("node.glb"), Transform(Pose(p))))
        }

        // Panels flanking the figure
        Entity.create(
            listOf(
                Panel(R.id.coach_panel),
                Transform(Pose(Vector3(-1.25f, 1.4f, 2.0f), Quaternion(0.0f, -22.0f, 0.0f))),
            )
        )
        Entity.create(
            listOf(
                Panel(R.id.telemetry_panel),
                Transform(Pose(Vector3(1.25f, 1.4f, 2.0f), Quaternion(0.0f, 22.0f, 0.0f))),
            )
        )

        // Tagline at the very bottom, beneath the figure's feet.
        Entity.create(
            listOf(
                Panel(R.id.tagline_panel),
                Transform(Pose(Vector3(0.0f, 0.15f, 2.1f))),
            )
        )
    }

    override fun registerPanels(): List<PanelRegistration> = listOf(
        composePanel(R.id.coach_panel, 0.75f, 1.15f) { CoachPanel() },
        composePanel(R.id.telemetry_panel, 0.75f, 1.15f) { TelemetryPanel() },
        composePanel(R.id.tagline_panel, 2.6f, 0.7f) { TaglinePanel() }
    )

    private fun composePanel(id: Int, w: Float, h: Float, content: @Composable () -> Unit) =
        ComposeViewPanelRegistration(
            id,
            { _, ctx -> ComposeView(ctx).apply { setContent { content() } } },
            {
                UIPanelSettings(
                    QuadShapeOptions(w, h),
                    DpPerMeterDisplayOptions(),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent)
                )
            },
        )

    private fun startSniffer() {
        val intent = Intent(this, QuestSnifferService::class.java)
        startForegroundService(intent)
    }
}
