plugins {
    id("lifecycle-base")
    id("jacoco")
    id("checkstyle")
    id("maven-publish")
    id("org.cyclonedx.bom") version "3.5.1"
    id("org.sonarqube") version "7.5.0.8588"
}

allprojects {
    group = "eu.ecodex.connector"
    version = "7.1.0"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://build.shibboleth.net/maven/releases/")
        }
    }
}

jacoco {
    toolVersion = "0.8.14"
}

sonar {
  properties {
    property("sonar.projectKey", "e-CODEX_connector-ng")
    property("sonar.organization", "e-codex")
  }
}

subprojects {
    pluginManager.apply("checkstyle")

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile> {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    checkstyle {
        toolVersion = "10.17.0"

        val localConfigFile = rootProject.file("config/checkstyle/checkstyle.xml")

        config = resources.text.fromFile(localConfigFile)

        configDirectory.set(rootProject.file("config/checkstyle"))

        isIgnoreFailures = true
        isShowViolations = true
    }

    var excludeFromJacoco = listOf("connector-documentation", "connector-distribution")

    if (!excludeFromJacoco.contains(name)) {
        val testTasks = tasks.withType<Test>()
        val jacocoReportTasks = tasks.withType<JacocoReport>()

        testTasks.configureEach {
            useJUnitPlatform()

            // increase memory for integration tests
            maxHeapSize = "1g"

            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = false
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }

            // set mockito agent to run in all subprojects
            val mockitoAgent by configurations.getting
            jvmArgs("-javaagent:${mockitoAgent.asPath}")

            // This ensures every Test task triggers the JacocoReport tasks in this submodule
            finalizedBy(jacocoReportTasks)
        }

        jacocoReportTasks.configureEach {
            // This ensures reports aren't generated if tests failed to produce execution data
            dependsOn(testTasks)

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(true)
            }
        }
    }

    // publish to artifactory

    fun RepositoryHandler.artifactoryMaven() {
        val releaseRepo = providers.gradleProperty("artifactory.url.release").get()
        val snapshotRepo = providers.gradleProperty("artifactory.url.snapshot").get()
        val repoId = providers.gradleProperty("artifactory.repo.id")
            .orElse(providers.environmentVariable("MAVEN_REPO_ID"))
            .get()
        maven {
            name = repoId
            url = uri(if (version.toString().endsWith("-SNAPSHOT")) snapshotRepo else releaseRepo)
            credentials {
                username = System.getenv("MAVEN_REPO_USERNAME")
                password = System.getenv("MAVEN_REPO_PASSWORD")
            }
        }
    }

    val notToPublish = listOf(
        "connector-integrationtest",
        "connector-infrastructure",
        "connector-documentation"
    )

    plugins.withId("java") {
        if (!notToPublish.contains(name)) {
            pluginManager.apply("maven-publish")
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") {
                        from(components["java"])
                    }
                }
                repositories { artifactoryMaven() }
            }
        }
    }
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/generated/**")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("build") {
    dependsOn("cyclonedxBom")
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Delegates to connector-integrationtest:integrationTest"
    dependsOn(":connector-integrationtest:integrationTest")
}

tasks.named("check") {
    dependsOn(":connector-integrationtest:integrationTest")
}

val jacocoExclusions = listOf(
    "**/config/**",
    "**/*Config.class",
    "**/*Configuration.class",
    "**/entity/**",
    "**/property/**",
    "**/org/etsi/uri/**",
    "**/org/w3/**",
    "**/oasis/names/tc/saml/**",
    "**/eu/spocseu/**",
)

tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Generates an aggregate report from all subprojects"

    // Ensure all subproject tests run before generating the root report
    val testTasks = subprojects
        .filter { it.name != "connector-documentation" }
        .map { it.tasks.withType<Test>() }

    dependsOn(testTasks)

    // Configure the aggregated report data
    val subprojectsToAggregate = subprojects.filter { it.name != "connector-documentation" }

    // 1. Collect all execution data (.exec files)
    executionData.setFrom(files(subprojectsToAggregate.map {
        it.layout.buildDirectory.file("jacoco/test.exec")
    }))

    // 2. Collect all source files for the report
    sourceDirectories.setFrom(files(subprojectsToAggregate.map {
        it.layout.projectDirectory.dir("src/main/java")
    }))

    // 3. Collect all compiled class files, excluding generated code if necessary
    classDirectories.setFrom(files(subprojectsToAggregate.map {
        it.layout.buildDirectory.dir("classes/java/main")
    }))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(jacocoExclusions) }
        })
    )
}
