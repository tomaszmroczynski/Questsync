package pl.complet.QuestSync.ui.spatial

import com.meta.spatial.core.Color4
import com.meta.spatial.runtime.SceneMaterial
import com.meta.spatial.toolkit.Material
import android.util.Log

/**
 * Zarządza wizualną tożsamością obiektów 3D.
 */
object CyberStyle {

    const val TAG = "CyberStyle"
    const val PLEXUS_ENABLED = true

    /**
     * Zwraca komponent materiału z wymuszonymi parametrami przezroczystości.
     */
    fun getPlexusMaterial(): Material {
        return Material().apply {
            // Czysta czerwień, Alpha 0.1 (wysoce przezroczyste)
            baseColor = Color4(1.0f, 0.0f, 0.0f, 0.1f) 
            
            unlit = true 
            
            // Tryb 2 = Alpha Blend (nakładanie przezroczystości)
            alphaMode = 2
            
            shader = SceneMaterial.UNLIT_SHADER
        }
    }

    fun getHumanShader(): String {
        return if (PLEXUS_ENABLED) SceneMaterial.UNLIT_SHADER else ""
    }
}
