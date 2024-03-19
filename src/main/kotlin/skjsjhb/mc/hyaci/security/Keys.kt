package skjsjhb.mc.hyaci.security

import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.model.StoredToken
import com.microsoft.credentialstorage.model.StoredTokenType
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.util.info
import skjsjhb.mc.hyaci.util.warn
import java.nio.file.Files
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val phraseLength = 256
private const val keyLength = 256
private const val salt = "SUPER_SECRET_SALT_DO_NOT_SHARE_OR_YOU_WILL_BE_FIRED"
private const val iterationCount = 10000
private const val systemKeyName = "HyaciLauncherSystemKeyV2"
private const val systemKeyFile = "DO_NOT_SHARE_THIS"

object Keys {
    /**
     * Exports generic purpose system key.
     */
    fun exportSystemKey(): SecretKey {
        val pwd = getSystemParaphrase()
        val spec = PBEKeySpec(pwd, salt.toByteArray(), iterationCount, keyLength)
        pwd.fill(' ')
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).also { spec.clearPassword() }
    }

    private fun getSystemParaphrase(): CharArray {
        runCatching { return getOrEnrollEncryptedParaphrase() }
        warn("Secure storage is not available - the system key will NOT be encrypted!")
        return getOrEnrollPublicParaphrase()
    }

    private fun getOrEnrollPublicParaphrase(): CharArray {
        val pt = dataPathOf(systemKeyFile)
        runCatching { return Files.readString(pt).toCharArray() }
        return generateSystemParaphrase().also {
            Files.createDirectories(pt.parent)
            Files.writeString(pt, String(it))
        }
    }

    private fun getOrEnrollEncryptedParaphrase(): CharArray {
        val credStore = StorageProvider.getTokenStorage(true, StorageProvider.SecureOption.REQUIRED)
            ?: throw UnsupportedOperationException("Secure storage not available")
        credStore[systemKeyName]?.let { return it.value }
        return generateSystemParaphrase().also {
            credStore.add(systemKeyName, StoredToken(it, StoredTokenType.UNKNOWN))
            info("Generated new system key for encryption")
        }
    }

    private fun ByteArray.encode(): CharArray = String(Base64.getEncoder().encode(this)).toCharArray()

    private fun generateSystemParaphrase(): CharArray =
        ByteArray(phraseLength).also { SecureRandom().nextBytes(it) }.encode()
}



