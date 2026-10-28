/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

import org.asciidoctor.gradle.model5.jvm.extensions.AsciidoctorjDiagram
import org.asciidoctor.gradle.model5.jvm.toolchains.AsciidoctorjToolchain

plugins {
    base
    alias(libs.plugins.asciidoctorj)
    alias(libs.plugins.asciidoctorj.pdf)
    alias(libs.plugins.asciidoctorj.diagram)
}

val generatedDiagramsDir = layout.buildDirectory.dir("docs/asciidoc/html/resources")

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
    .configureEach { dependsOn(copyImages) }

tasks.named("build") {
    dependsOn("asciidoctorAll")
}
