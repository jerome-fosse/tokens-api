package com.wcomp.app.tokens.utils

import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.wcomp.app.tokens.FakeTokenGenerator
import com.wcomp.app.tokens.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@UnitTest
class JWTValidatorTest {

    private val rsaKey = """-----BEGIN PUBLIC KEY-----
                            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6vB+aOUF7s5sKGLCQPA2
                            bsG9KG3RuoFYpVWTc1/wpHMI+QSE3Hcu540wf00O059NDwVUEl4XaRBfz1KtBx8u
                            IiMX68xdRDnEeqxjnOCx2zyuCW84dqj1c1VuqSw14Qf8syCBNqSERGchtCioYLKs
                            Xf9qceIscVmm2kjVHzxhNz2wvwVDrXZTI7mITGl4UxAUoiP900ohln1aW7zYao8l
                            9Jv9kamR81fDnaOkK+WshSqm4ktfa7CQNS7d50w63K7kQ8balQ7jokIN3RLB6LFS
                            ZTosbv/B+S9skBn/aQxM0jIH6bh74OZd6onCl/rfZ3K/fp1DKIRpJzs4Md7X6PGa
                            iQIDAQAB
                            -----END PUBLIC KEY-----""".trim()

    private val fakeTokenGenerator = FakeTokenGenerator()

    @Test
    fun decodeAndCheckValidity_shouldThrowSignatureVerificationException_whenTokenIsNotValid() {
        val invalidIdToken = "eyJraWQiOiIxZTlnZGs3IiwiYWxnIjoiUlMyNTYifQ.ewogImlzcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiAibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDEzMTEyODA5NzAsCiAibmFtZSI6ICJKYW5lIERvZSIsCiAiZ2l2ZW5fbmFtZSI6ICJKYW5lIiwKICJmYW1pbHlfbmFtZSI6ICJEb2UiLAogImdlbmRlciI6ICJmZW1hbGUiLAogImJpcnRoZGF0ZSI6ICIwMDAwLTEwLTMxIiwKICJlbWFpbCI6ICJqYW5lZG9lQGV4YW1wbGUuY29tIiwKICJwaWN0dXJlIjogImh0dHA6Ly9leGFtcGxlLmNvbS9qYW5lZG9lL21lLmpwZyIKfQ.rHQjEmBqn9Jre0OLykYNnspA10Qql2rvx4FsD00jwlB0Sym4NzpgvPKsDjn_wMkHxcp6CilPcoKrWHcipR2iAjzLvDNAReF97zoJqq880ZD1bwY82JDauCXELVR9O6_B0w3K-E7yM2macAAgNCUwtik6SjoSUZRcf-O5lygIyLENx882p6MtmwaL1hd6qn5RZOQ0TLrOYu0532g9Exxcm-ChymrB4xLykpDj3lUivJt63eEGGN6DH5K6o33TcxkIjNrCD4XB1CKKumZvCedgHHF3IAK4dVEDSUoGlH9z4pP_eWYNXvqQOjGs-rDaQzUHl6cQQWNiDpWOl_lxXjQEvQ"

        val jwtValidator = JWTValidator(rsaKey)
        val thrown = catchThrowable { jwtValidator.decodeAndCheckValidity(invalidIdToken) }

        assertThat(thrown).isInstanceOf(SignatureVerificationException::class.java)
    }

    @Test
    fun decodeAndCheckValidity_shouldThrowTokenExpiredException_whenTokenIsValidButExpired() {
        val expiredIdToken = fakeTokenGenerator.generateSignedToken("batman", Date.from(Instant.now().minus(6, ChronoUnit.HOURS)))

        val jwtValidator = JWTValidator(rsaKey)
        val thrown = catchThrowable { jwtValidator.decodeAndCheckValidity(expiredIdToken) }

        assertThat(thrown).isInstanceOf(TokenExpiredException::class.java)

    }

    @Test
    fun decodeAndCheckValidity_shouldReturnAJwtDecode_whenTokenIsValid() {
        //Assert
        val okIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("batman")

        //Test
        val jwtValidator = JWTValidator(rsaKey)
        val thrown = catchThrowable { jwtValidator.decodeAndCheckValidity(okIdToken) }

        assertThat(thrown).isNull()
    }

    @Test
    fun getIuc_shouldReturnTheIUC_whenTokenIsValid() {
        //Assert
        val okIdToken = fakeTokenGenerator.generateNotExpiredSignedToken("batman")

        //Test
        val jwtValidator = JWTValidator(rsaKey)
        val actual = jwtValidator.getIuc(okIdToken)
        assertThat(actual).isNotNull().isEqualTo("batman")
    }
}