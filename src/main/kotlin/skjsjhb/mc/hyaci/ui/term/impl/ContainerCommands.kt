package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.container.ContainerManager
import skjsjhb.mc.hyaci.container.VanillaContainer
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
import java.nio.file.Path

@Suppress("unused")
class ContainerCommands : CommandProcessor {
    @CommandName("cc")
    @Usage(
        """
        cc <path> <name> - Create container.
            path - Root path on the disk.
            name - Container name.
    """
    )
    fun createVanilla(path: String, name: String) {
        InteractionContext.run {
            if (ContainerManager.exists(name)) {
                requestConfirm("Container named $name already exists. Overwrite?")
            }
            ContainerManager.put(VanillaContainer(name, Path.of(path)))
            info("Registered vanilla container $name at $path")
        }
    }

    @CommandName("lc")
    @Usage("lc - List all containers.")
    fun listAll() {
        InteractionContext.run {
            ContainerManager.getAll().forEach {
                info("- ${it.name()} at ${it.resolve(".")} (${it::class.simpleName})")
            }
        }
    }

    @WithAdapters(ContainerLoader::class)
    @CommandName("rc")
    @Usage(
        """
        rc <c> - Remove a container.
            c - Container name.
    """
    )
    fun remove(c: Container) {
        InteractionContext.run {
            info("Will delete container ${c.name()} at ${c.resolve(".")}")
            info("(Files will not be touched)")
            warn("This is irrevocable!")
            requestConfirm("Continue removing?")
            ContainerManager.remove(c.name())
            info("Removed that container.")
        }
    }
}