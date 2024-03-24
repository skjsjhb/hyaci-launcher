package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
import skjsjhb.mc.hyaci.ui.term.requireConfirm
import skjsjhb.mc.hyaci.ui.term.tinfo
import skjsjhb.mc.hyaci.ui.term.twarn
import skjsjhb.mc.hyaci.vfs.VanillaFs
import skjsjhb.mc.hyaci.vfs.Vfs
import skjsjhb.mc.hyaci.vfs.VfsManager
import java.nio.file.Path

@Suppress("unused")
class VfsCommands : CommandProcessor {
    @CommandName("mkfs")
    @Usage(
        """
        mkfs <path> <name> - Create VFS container.
            path - Root path on the disk.
            name - VFS name.
    """
    )
    fun createVanilla(path: String, name: String) {
        if (VfsManager.exists(name)) {
            requireConfirm("VFS named $name already exists. Overwrite?")
        }
        VfsManager.put(VanillaFs(name, Path.of(path)))
        tinfo("Registered vanilla fs $name at $path")
    }

    @CommandName("lsfs")
    @Usage("lsfs - List all VFS containers.")
    fun listAll() {
        VfsManager.getAll().forEach {
            tinfo("- ${it.name()} at ${it.resolve(".")} (${it::class.simpleName})")
        }
    }

    @WithAdapters(VfsLoader::class)
    @CommandName("rmfs")
    @Usage(
        """
        rmfs <fs> - Remove a VFS container.
            fs - VFS name.
    """
    )
    fun remove(fs: Vfs) {
        tinfo("Will delete VFS ${fs.name()} at ${fs.resolve(".")}")
        twarn("This is irrevocable!")
        requireConfirm("Continue removing?")
        VfsManager.remove(fs.name())
        tinfo("Removed that VFS.")
    }
}