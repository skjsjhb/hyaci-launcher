package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.install.JreInstaller
import skjsjhb.mc.hyaci.install.VanillaInstaller
import skjsjhb.mc.hyaci.vfs.VfsManager

class VanillaInstallerCommandHandler : AbstractCommandHandler("install.vanilla") {
    override fun handle(command: Command): Boolean {
        val id = command.get("id", 0)
        val fsName = command.get("vfs", 1)
        val fs = VfsManager.get(fsName)
        tinfo("Install vanilla game $id on VFS '$fsName'.")
        tinfo("(This may take some minutes and many logs will appear)")
        askConfirm("Is this correct?").let { if (!it) return false }
        VanillaInstaller(id, fs).install()
        tinfo("Completed installation of $id.")
        return true
    }
}

class JreInstallerCommandHandler : AbstractCommandHandler("install.jre", "install.java") {
    override fun handle(command: Command): Boolean {
        val component = command.get("component", 0)
        tinfo("Install JRE component '$component'.")
        askConfirm("Is this correct?").let { if (!it) return false }
        JreInstaller(component).install()
        tinfo("Completed installation of '$component'.")
        return true
    }
}