package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.ui.term.ArgumentAdapter
import skjsjhb.mc.hyaci.vfs.Vfs
import skjsjhb.mc.hyaci.vfs.VfsManager
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class VfsLoader : ArgumentAdapter<Vfs> {
    override fun get(src: String): Vfs = VfsManager.get(src)

    override fun target(): KType = Vfs::class.createType()
}