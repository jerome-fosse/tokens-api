package com.wcomp.app.tokens.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.apache.commons.lang3.StringUtils
import org.bouncycastle.util.io.pem.PemReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.io.IOException
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Dans la V2, ce validator sera intégré directement aux controllers à protéger via le mécanisme Oauth2 de Spring Security.
 */
@Component
class JWTValidator(@Value("\${wcomp.connect.key.public}") publicKeyValue: String) {

    companion object {
        val logger = LoggerFactory.getLogger(JWTValidator::class.java)
    }

    private val jwtVerifier: JWTVerifier

    init {
        Assert.hasText(publicKeyValue, "publicKeyValue shall not be blank")
        this.jwtVerifier = buildJwtVerifier(publicKeyValue)
    }

    @Throws(JWTVerificationException::class)
    fun decodeAndCheckValidity(token: String): DecodedJWT {
        try {
            return jwtVerifier.verify(token)
        } catch (e: JWTVerificationException) {
            logger.error("Invalid JWT token '${StringUtils.substring(token, 96, 120)}'", e)// to get something more different than the header
            throw e
        }
    }

    fun getIuc(idToken: String): String {
        val jwt = decodeAndCheckValidity(idToken)
        return jwt.subject
    }

    private fun buildJwtVerifier(publicKeyValue: String): JWTVerifier {
        try {
            val kf = KeyFactory.getInstance("RSA")
            val publicKey = readPublicKey(kf, publicKeyValue)
            return JWT.require(Algorithm.RSA256(publicKey, null)).build()
        } catch (e: Exception) {
            logger.error("Could not build the JWTVerifier, publicKeyValue was '$publicKeyValue'", e)
            throw RuntimeException(e)
        }

    }

    @Throws(InvalidKeySpecException::class, IOException::class)
    private fun readPublicKey(factory: KeyFactory, keyValue: String): RSAPublicKey {
        PemReader(StringReader(keyValue)).use { pemReader ->
            val content = pemReader.readPemObject().content
            val pubKeySpec = X509EncodedKeySpec(content)
            return factory.generatePublic(pubKeySpec) as RSAPublicKey
        } //finally will close
    }
}
