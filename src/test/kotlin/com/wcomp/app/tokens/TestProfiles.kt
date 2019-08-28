package com.wcomp.app.tokens

import org.junit.jupiter.api.Tag
import org.testcontainers.junit.jupiter.Testcontainers


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("unit")
annotation class UnitTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testcontainers
@Tag("docker")
annotation class DockerTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration")
annotation class IntegrationTest