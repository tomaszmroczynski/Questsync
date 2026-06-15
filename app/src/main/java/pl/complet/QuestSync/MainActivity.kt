package pl.complet.QuestSync

import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import pl.complet.QuestSync.service.QuestSnifferService
import pl.complet.QuestSync.ui.spatial.CoachPanel
import pl.complet.QuestSync.ui.spatial.TaglinePanel
import pl.complet.QuestSync.ui.spatial.TelemetryPanel

/**
 * QuestSync immersive entry point (Meta Spatial SDK, native VR).
 *
 * Scene: a plexus point-cloud human figure straight ahead at eye level, framed by a glowing
 * portal ring, over a distant ember constellation backdrop and an "AI × SOFTWARE × HARDWARE"
 * tagline — composing the Ripperdoc bio-scan look. A scan ring sweeps the figure top-to-bottom.
 * Coach and telemetry panels (semi-transparent) flank the figure. The Quest sniffer keeps
 * feeding real-time activity to RipperMCP.
 */
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

        // Viewer forward is +Z; place content at +Z and yaw 180° to face the viewer.
        val faceViewer = Quaternion(0.0f, 180.0f, 0.0f)

        fun unlit(asset: String) =
            Mesh("apk:///$asset".toUri()).apply { defaultShaderOverride = SceneMaterial.UNLIT_SHADER }

        // Real star constellations as the distant backdrop (Big Dipper, Orion, Cassiopeia, Cygnus).
        Entity.create(listOf(unlit("bg-stars.glb"), Transform(Pose(Vector3(0.0f, 0.0f, 0.0f)))))

        // Plexus point-cloud figure straight ahead at eye level, facing the viewer.
        // Three node groups + lines, each a transparent unlit material; node groups slowly
        // pulse their glow/alpha with different phases & periods, so they twinkle like lights.
        val figurePose = Pose(Vector3(0.0f, 0.55f, 1.8f), faceViewer)

        fun transparentMat(r: Float, g: Float, b: Float, a: Float) = Material().apply {
            baseColor = Color4(r, g, b, a)
            unlit = true
            alphaMode = 2
            shader = SceneMaterial.UNLIT_SHADER
        }

        // Plexus parts (3 node groups + lines), each with a base ember tint. They all pulse
        // UNIFORMLY together: fade almost to zero and back, 3 s up + 3 s down (6 s cycle).
        val plexusParts = listOf(
            "plexus_lines.glb" to floatArrayOf(0.95f, 0.60f, 0.30f),
            "plexus_a.glb" to floatArrayOf(1.0f, 0.66f, 0.32f),
            "plexus_b.glb" to floatArrayOf(1.0f, 0.72f, 0.40f),
            "plexus_c.glb" to floatArrayOf(1.0f, 0.88f, 0.60f),
        ).map { (asset, c) ->
            Entity.create(
                listOf(
                    Mesh("apk:///$asset".toUri()),
                    transparentMat(c[0], c[1], c[2], 0.5f),
                    Transform(figurePose),
                )
            ) to c
        }

        val pulsePeriod = 6000f  // 3 s brighten + 3 s fade
        sceneScope.launch {
            var t = 0f
            while (isActive) {
                val k = (0.5 - 0.5 * kotlin.math.cos(2.0 * PI * (t / pulsePeriod))).toFloat() // 0→1→0
                val br = 0.02f + 0.98f * k       // almost to zero, up to full
                val alpha = 0.03f + 0.62f * k    // transparent, fading with the glow
                for ((ent, c) in plexusParts) {
                    ent.setComponent(transparentMat(c[0] * br, c[1] * br, c[2] * br, alpha))
                }
                delay(33)
                t += 33f
            }
        }

        // Telemetry nodes — glowing markers in front of the figure (heart, core, activity).
        for (p in listOf(
            Vector3(0.0f, 1.55f, 1.65f),
            Vector3(0.0f, 1.15f, 1.65f),
            Vector3(0.32f, 1.25f, 1.65f),
        )) {
            Entity.create(listOf(unlit("node.glb"), Transform(Pose(p))))
        }

        // Coach (left) and telemetry (right) panels, same depth line, angled toward viewer.
        Entity.create(
            listOf(
                Panel(R.id.coach_panel),
                Transform(Pose(Vector3(-1.15f, 1.4f, 1.8f), Quaternion(0.0f, -22.0f, 0.0f))),
            )
        )
        Entity.create(
            listOf(
                Panel(R.id.telemetry_panel),
                Transform(Pose(Vector3(1.15f, 1.4f, 1.8f), Quaternion(0.0f, 22.0f, 0.0f))),
            )
        )

        // Tagline high and centred, behind the figure.
        Entity.create(
            listOf(
                Panel(R.id.tagline_panel),
                Transform(Pose(Vector3(0.0f, 2.7f, 3.6f))),
            )
        )

        // Scan ring sweeping the figure top-to-bottom.
        val scan = Entity.create(
            listOf(unlit("scan-ring.glb"), Transform(Pose(Vector3(0.0f, 2.25f, 1.8f))))
        )
        val top = 2.30f
        val bottom = 0.55f
        val periodMs = 4000f
        sceneScope.launch {
            var elapsed = 0f
            while (isActive) {
                val t = (elapsed % periodMs) / periodMs
                val y = top - (top - bottom) * t
                scan.setComponent(Transform(Pose(Vector3(0.0f, y, 1.8f))))
                delay(33)
                elapsed += 33f
            }
        }
    }

    override fun registerPanels(): List<PanelRegistration> =
        listOf(
            composePanel(R.id.coach_panel, 0.62f, 0.92f) { CoachPanel() },
            composePanel(R.id.telemetry_panel, 0.62f, 0.92f) { TelemetryPanel() },
            composePanel(R.id.tagline_panel, 2.6f, 0.7f) { TaglinePanel() },
        )

    private fun composePanel(id: Int, w: Float, h: Float, content: @Composable () -> Unit) =
        ComposeViewPanelRegistration(
            id,
            composeViewCreator = { _, ctx ->
                ComposeView(ctx).apply { setContent { content() } }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = w, height = h),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )

    private fun startSniffer() {
        val intent = Intent(this, QuestSnifferService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
