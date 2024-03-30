package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.container.ContainerManager
import skjsjhb.mc.hyaci.ui.term.ArgumentAdapter
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class ContainerLoader : ArgumentAdapter<Container> {
    override fun get(src: String): Container = ContainerManager.get(src)

    override fun target(): KType = Container::class.createType()
}