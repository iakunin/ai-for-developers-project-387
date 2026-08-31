import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
}

group = "dev.iakunin"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Bean validation is off (see useBeanValidation below), but the generator still stamps
    // required fields with a bare @NotNull as a nullability marker, so the annotation type
    // itself must stay on the classpath. This pulls in no validator implementation.
    implementation("jakarta.validation:jakarta.validation-api")

    // Build-time only: Lombok never reaches the runtime image. Version comes from the BOM.
    // The BOM has to be applied to each configuration separately: platform() on
    // `implementation` alone does not manage `compileOnly`/`annotationProcessor` versions.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-restclient")
}

// The contract is the single source of truth: models are generated, never handwritten.
val contractSpec = layout.projectDirectory.file("../../api-contract/openapi/openapi.yaml")
val generatedSources = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName = "spring"
    inputSpec = contractSpec
    outputDir = generatedSources
    modelPackage = "dev.iakunin.callcalendar.contract.model"
    invokerPackage = "dev.iakunin.callcalendar.contract"
    globalProperties = mapOf(
        "models" to "",
        "modelDocs" to "false",
        "modelTests" to "false",
    )
    configOptions = mapOf(
        "useJakartaEe" to "true",
        "dateLibrary" to "java8",
        "useBeanValidation" to "false",
        "openApiNullable" to "false",
        "hideGenerationTimestamp" to "true",
        "serializationLibrary" to "jackson",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        // Builders for every DTO, so call sites read Booking.builder()....build().
        "generateBuilders" to "true",
    )
}

sourceSets.named("main") {
    java.srcDir(generatedSources.map { it.dir("src/main/java") })
}

tasks.named("compileJava") {
    dependsOn(tasks.openApiGenerate)
}

spotless {
    java {
        // Only our own sources; generated code is not ours to format.
        target("src/**/*.java")
        googleJavaFormat()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Spring Boot's plugin also emits a "-plain" jar; nothing consumes it, and it makes the
// Dockerfile's build/libs/*.jar glob ambiguous.
tasks.named<Jar>("jar") { enabled = false }
