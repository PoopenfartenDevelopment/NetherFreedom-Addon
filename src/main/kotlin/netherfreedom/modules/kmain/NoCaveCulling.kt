package netherfreedom.modules.kmain

import netherfreedom.MeteorModule
import netherfreedom.modules.NetherFreedom

class NoCaveCulling:MeteorModule(NetherFreedom.MAIN, "NoCaveCulling", "Disables Minecraft's cave culling algorithm.") {

    override fun onActivate() {
        super.onActivate()
        mc.chunkCullingEnabled = false
        mc.worldRenderer.reload()
    }

    override fun onDeactivate() {
        super.onDeactivate()
        mc.chunkCullingEnabled = true
        mc.worldRenderer.reload()
    }

}
