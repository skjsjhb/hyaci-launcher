package skjsjhb.mc.hyaci.security

import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.model.StoredToken
import com.microsoft.credentialstorage.model.StoredTokenType
import skjsjhb.mc.hyaci.util.info
import java.security.SecureRandom
import java.util.stream.Stream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val phraseLength = 256L
private const val keyLength = 256
private const val salt = "SUPER_SECRET_SALT_DO_NOT_SHARE_OR_YOU_WILL_BE_FIRED"
private const val iterationCount = 10000
private const val systemKeyName = "HyaciLauncherSystemKey"

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
        val credStore = StorageProvider.getTokenStorage(true, StorageProvider.SecureOption.REQUIRED)
            ?: throw UnsupportedOperationException("Secure storage not available")
        credStore[systemKeyName]?.let { return it.value }
        return generateSystemParaphrase().also {
            credStore.add(systemKeyName, StoredToken(it, StoredTokenType.UNKNOWN))
            info("Generated new system key for encryption")
        }
    }

    private fun generateSystemParaphrase(): CharArray =
        SecureRandom().let { rand ->
            Stream.generate { rand.nextInt().toChar() }
                .limit(phraseLength)
                .toArray { CharArray(it).toTypedArray() }
                .toCharArray()
        }
}



