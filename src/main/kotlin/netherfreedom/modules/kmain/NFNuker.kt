package netherfreedom.modules.kmain

import netherfreedom.*
import netherfreedom.modules.NetherFreedom
import meteordevelopment.meteorclient.events.world.TickEvent
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import kotlin.math.floor

class NFNuker:MeteorModule(NetherFreedom.MAIN, "NFNuker", "Custom nuker specifically made for the Nether Freedom project.") {

    private val general = settings.defaultGroup

    private var keepY by general.add(IValue("KeepY", 120, "Keeps a specific Y level when digging", -1..255, 1))

    // preserve 2 block tall tunnel for speed bypass
    private var blacklist:MutableList<BlockPos> = ArrayList()

    // previous floored block position of player
    private var prevBlockPos:BlockPos = BlockPos.ORIGIN

    // floored block position of player
    private var playerPos:BlockPos = BlockPos.ORIGIN

    // last time packets were sent
    private var lastUpdateTime:Long = 0

    private var packets:Int = 0

    override fun onActivate() {
        super.onActivate()
        // reset blacklist
        blacklist.clear()
    }

    override fun onDeactivate() {
        super.onDeactivate()
        // reset blacklist
        blacklist.clear()
    }

    @EventHandler
    fun tick(event:TickEvent.Pre) {
        this.prevBlockPos = this.playerPos
        this.playerPos = BlockPos(floor(mc.player!!.x).toInt(),
                                  if (keepY != -1) keepY else floor(mc.player!!.y).toInt(),
                                  floor(mc.player!!.z).toInt())
        if (this.playerPos != this.prevBlockPos || Util.getMeasuringTimeMs() - this.lastUpdateTime > 800) {
            this.getBlacklistedBlockPoses()
            this.doMine(playerPos.add(0, 0, 0))
            this.doMine(playerPos.add(0 * -3, 0, 0 * -3))
            this.lastUpdateTime = Util.getMeasuringTimeMs()
        }
        packets = 0
    }

    private fun doMine(plyerPos:BlockPos = playerPos) {
        for (i in -4..4) {
            this.breakBlock(plyerPos.forward(i))
            this.breakBlock(plyerPos.forward(i).up())
            this.breakBlock(plyerPos.forward(i).up(2))
            this.breakBlock(plyerPos.forward(i).right(1))
            this.breakBlock(plyerPos.forward(i).right(1).up())
            this.breakBlock(plyerPos.forward(i).right(1).up(2))
            this.breakBlock(plyerPos.forward(i).right(2))
            this.breakBlock(plyerPos.forward(i).right(2).up())
            this.breakBlock(plyerPos.forward(i).right(2).up(2))
            this.breakBlock(plyerPos.forward(i).right(3))
            this.breakBlock(plyerPos.forward(i).right(3).up())
            this.breakBlock(plyerPos.forward(i).right(3).up(2))
            this.breakBlock(plyerPos.forward(i).right(4))
            this.breakBlock(plyerPos.forward(i).right(4).up())
            this.breakBlock(plyerPos.forward(i).right(4).up(2))

            this.breakBlock(plyerPos.forward(i).left(1))
            this.breakBlock(plyerPos.forward(i).left(1).up())
            this.breakBlock(plyerPos.forward(i).left(1).up(2))
            this.breakBlock(plyerPos.forward(i).left(2))
            this.breakBlock(plyerPos.forward(i).left(2).up())
            this.breakBlock(plyerPos.forward(i).left(2).up(2))
            this.breakBlock(plyerPos.forward(i).left(3))
            this.breakBlock(plyerPos.forward(i).left(3).up())
            this.breakBlock(plyerPos.forward(i).left(3).up(2))
            this.breakBlock(plyerPos.forward(i).left(4))
            this.breakBlock(plyerPos.forward(i).left(4).up())
            this.breakBlock(plyerPos.forward(i).left(4).up(2))
        }
    }

    private fun getBlacklistedBlockPoses() {
        this.blacklist.clear()
        if (highway in 1..4) {
            this.blacklist.add(this.playerPos.up(2))
            this.blacklist.add(this.playerPos.up(2).backward(1))
            this.blacklist.add(this.playerPos.up(2).backward(2))
            this.blacklist.add(this.playerPos.forward(1).up(2))
            this.blacklist.add(this.playerPos.forward(2).up(2))
            this.blacklist.add(this.playerPos.forward(3).up(2))
            this.blacklist.add(this.playerPos.forward(4).up(2))
            this.blacklist.add(this.playerPos.forward(5).up(2))
        } else {
            val f = MathHelper.sin(mc.player!!.yaw * 0.017453292f)
            val g = MathHelper.cos(mc.player!!.yaw * 0.017453292f)
            for (i in -2..5) {
                val pos = mc.player!!.pos.add(-f * i.toDouble(), 2.0, g * i.toDouble())
                this.blacklist.add(BlockPos(pos))
                this.blacklist.add(BlockPos(pos).left(1))
                this.blacklist.add(BlockPos(pos).left(2))
                this.blacklist.add(BlockPos(pos).right(1))
            }
        }
    }

    private fun breakBlock(blockPos:BlockPos) {
        if (packets >= 130) return
        if (mc.world!!.getBlockState(blockPos).material.isReplaceable) return
        if (mc.world!!.getBlockState(blockPos).block.hardness >= 50) return
        mc.networkHandler!!.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                                                             blockPos,
                                                             Direction.UP))
        mc.networkHandler!!.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                                             blockPos,
                                                             Direction.UP))
        packets += 2
    }
}
