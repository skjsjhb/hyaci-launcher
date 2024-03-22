package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.vfs.VanillaFs
import skjsjhb.mc.hyaci.vfs.VfsManager
import java.nio.file.Path

class MakeFsCommandHandler : AbstractCommandHandler("mkfs", "mkfs.vanilla") {
    override fun handle(command: Command): Boolean {
        val path = command.get("path", 0)
        val name = command.get("name", 1)
        if (VfsManager.exists(name)) {
            askConfirm("VFS named $name already exists. Overwrite?").let { if (!it) return false }
        }
        VfsManager.put(VanillaFs(name, Path.of(path)))
        tinfo("Registered vanilla fs '$name' at $path")
        return true
    }
}

class ListFsCommandHandler : AbstractCommandHandler("lsfs") {
    override fun handle(command: Command): Boolean {
        VfsManager.getAll().forEach {
            tinfo("- ${it.name()} at ${it.resolve(".")} (${it::class.simpleName})")
        }
        return true
    }
}

class RemoveFsCommandHandler : AbstractCommandHandler("rmfs") {
    override fun handle(command: Command): Boolean {
        val fsName = command.get("fs", 0)
        val fs = VfsManager.get(fsName)
        tinfo("Will delete VFS '$fsName' at ${fs.resolve(".")}")
        twarn("This is irrevocable!")
        askConfirm("Continue removing?").let { if (!it) return false }
        VfsManager.remove(fsName)
        tinfo("Removed specified VFS.")
        return true
    }
}