/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

import groovy.json.JsonSlurper
import org.asciidoctor.gradle.model5.jvm.extensions.AsciidoctorjDiagram
import org.asciidoctor.gradle.model5.jvm.toolchains.AsciidoctorjToolchain

plugins {
    base
    alias(libs.plugins.asciidoctorj)
    alias(libs.plugins.asciidoctorj.pdf)
    alias(libs.plugins.asciidoctorj.diagram)
}

val generatedDiagramsDir = layout.buildDirectory.dir("docs/asciidoc/html/resources")
val generatedAsciidocDir = layout.buildDirectory.dir("generated/asciidoc")

// The property reference is generated from the metadata that spring-boot-configuration-processor writes
// while the infrastructure module is compiled, so it cannot drift from the code.
val propertyMetadataFile = project(":connector-infrastructure").layout.buildDirectory
    .file("classes/java/main/META-INF/spring-configuration-metadata.json")

val generatePropertyReference = tasks.register("generatePropertyReference") {
    group = "documentation"
    description = "Generates the AsciiDoc property reference from the configuration metadata."
    dependsOn(":connector-infrastructure:compileJava")

    val metadata = propertyMetadataFile
    val outputFile = generatedAsciidocDir.map { it.file("property-reference.adoc") }
    inputs.file(metadata)
    outputs.file(outputFile)

    doLast {
        fun cell(value: Any?): String = (value?.toString() ?: "")
            .replace(Regex("\\{@(?:code|link|literal)\\s+([^}]*)}"), "$1")
            .replace(Regex("<[^>]+>"), "")
            .replace("|", "\\|")
            .replace(Regex("\\s+"), " ")
            .trim()

        fun code(value: Any?): String {
            val text = value?.toString() ?: ""
            return if (text.isEmpty()) "" else "`+" + text.replace("|", "\\|") + "+`"
        }

        @Suppress("UNCHECKED_CAST")
        val root = JsonSlurper().parse(metadata.get().asFile) as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val properties = (root["properties"] as List<Map<String, Any?>>)
            .filter { (it["name"] as String).startsWith("connector.") }
            .sortedBy { it["name"] as String }
        val grouped = properties.groupBy { (it["name"] as String).substringBeforeLast('.') }

        val out = StringBuilder()
        out.appendLine("[[property-reference]]")
        out.appendLine("= Property reference (generated)")
        out.appendLine()
        out.appendLine("This reference is generated from the `@ConfigurationProperties` classes of the Connector at build time.")
        out.appendLine("It lists every property that the code binds, with the type, the default value set in the code and")
        out.appendLine("the description from the source. The chapters of this guide add the context and examples.")
        out.appendLine()
        out.appendLine("NOTE: A *default* is the value used when the property is absent. Values in the shipped template are not defaults.")
        out.appendLine("Properties that are not listed here are not bound by the Connector (they may belong to Spring Boot).")
        grouped.forEach { (prefix, entries) ->
            out.appendLine()
            out.appendLine("== `+$prefix+`")
            out.appendLine()
            out.appendLine("[cols=\"3,2,2,5\", options=\"header\"]")
            out.appendLine("|===")
            out.appendLine("| Property | Type | Default | Description")
            entries.forEach { p ->
                out.appendLine()
                out.appendLine("| ${code(p["name"])}")
                out.appendLine("| ${code(p["type"])}")
                out.appendLine("| ${code(p["defaultValue"])}")
                out.appendLine("| ${cell(p["description"])}")
            }
            out.appendLine("|===")
        }

        // a blank line after the last table keeps Asciidoctor from reporting an unterminated table
        out.appendLine()

        val target = outputFile.get().asFile
        target.parentFile.mkdirs()
        target.writeText(out.toString())
    }
}

asciidoc {
    toolchains {
        named("asciidoctorj", AsciidoctorjToolchain::class.java) {
            asciidocExtensions.named("diagram", AsciidoctorjDiagram::class.java) {
                useDiagram()
                usePlantUml()
            }
        }
    }

    publications {
        named("main") {
            sourceSet {
                sources {
                    include(
                        // complete documentation: single entry point including all guides
                        "index.adoc",
                        "architecture/index.adoc",
                        "configuration/index.adoc",
                        "installation/index.adoc",
                        "changelog/index.adoc",
                        "compatibility/index.adoc"
                    )
                }

                attributes {
                    addAll(
                        mapOf(
                            "project-version" to project.version.toString(),
                            "toc" to "left",
                            "toclevels" to "4",
                            "icons" to "font",
                            "sectnums" to "",
                            "numbered" to "",
                            "source-highlighter" to "rouge",
                            "rouge-style" to "github",
                            "basepath" to file("src/docs/asciidoc").absolutePath,
                            "generated-dir" to generatedAsciidocDir.get().asFile.absolutePath,
                            // "imagesdir" to "../resources",
                            // where the diagram extension writes generated images
                            "imagesoutdir" to generatedDiagramsDir.get().asFile.absolutePath
                        )
                    )
                }
            }
            output("asciidoctorj", "html")
            output("asciidoctorj", "pdf")
        }
    }
}

val copyImages = tasks.register<Copy>("copyImages") {
    from("src/docs/resources/images")
    into(layout.buildDirectory.dir("docs/asciidoc/html/resources"))
}

tasks.matching { it.name.startsWith("asciidoctor") && it.name != "asciidoctorAll" }
    .configureEach { dependsOn(copyImages, generatePropertyReference) }

tasks.named("build") {
    dependsOn("asciidoctorAll")
}
