plugins {
  id("otel.java-conventions")
  id("org.graalvm.buildtools.native")
}

description = "OpenTelemetry Graal Integration Tests"
otelJava.moduleName.set("io.opentelemetry.graal.integration.tests")


dependencies {
  implementation("org.jctools:jctools-core")
}

graalvmNative {
  binaries {
    named("main") {
      verbose.set(true)
    }
  }
  toolchainDetection.set(false)
}
