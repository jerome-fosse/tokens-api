package com.wcomp.app.tokens

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.util.ResourceUtils
import java.io.IOException
import java.nio.file.Files
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class FakeTokenGenerator {

    var rsaAlgorithm: Algorithm

    init {
        this.rsaAlgorithm = rsaAlgorithm()
    }

    fun generateSignedToken(sub: String, expiresAt: Date): String {
        return JWT.create()
                .withSubject(sub)
                .withExpiresAt(expiresAt)
                .withIssuer("http://r.connect.wcomp.com:80/SvcECZ/oauth2/connect")
                .withClaim("azp", "UNI_01005_DEV")
                .withClaim("tokenName", "id_token")
                .withClaim("realm", "/connect")
                .sign(rsaAlgorithm)
    }

    fun generateNotExpiredSignedToken(sub: String): String {
        return generateSignedToken(sub, Date.from(Instant.now().plus(6, ChronoUnit.HOURS)))
    }

    private fun rsaAlgorithm(): Algorithm {
        try {
            val kf = KeyFactory.getInstance("RSA")
            val privateKey = readPrivateKey(kf)
            return Algorithm.RSA256(null, privateKey)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(InvalidKeySpecException::class, IOException::class)
    private fun readPrivateKey(factory: KeyFactory): RSAPrivateKey {
        val file = ResourceUtils.getFile("classpath:private.der")
        val content = Files.readAllBytes(file.toPath())
        val priKeySpec = PKCS8EncodedKeySpec(content)
        return factory.generatePrivate(priKeySpec) as RSAPrivateKey
    }
}
