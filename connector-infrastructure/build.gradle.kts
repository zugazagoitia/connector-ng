/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

plugins {
    id("java")
    id("jacoco")
    id("java-test-fixtures")
}

val mockitoAgent: Configuration = configurations.create("mockitoAgent")
val jaxbTool: Configuration = configurations.create("jaxbTool")

dependencies {
    jaxbTool(libs.jaxb.xjc)
    jaxbTool(libs.jaxb.impl)
    jaxbTool(libs.jakarta.xml.bind.api)
    implementation(project(":connector-domain"))
    implementation(project(":connector-soap-api"))
    implementation(project(":connector-application"))
    // apache
    // Apache CXF
    implementation(platform(libs.apache.cxf.bom))
    implementation("org.apache.cxf:cxf-spring-boot-starter-jaxws")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws")
    implementation("org.apache.cxf:cxf-rt-ws-policy")
    implementation("org.apache.cxf:cxf-rt-ws-security")
    implementation("org.apache.cxf:cxf-rt-features-logging")
    // apache web service security
    implementation(libs.apache.wss4j.common) {
        exclude(group = "org.apache.santuario", module = "xmlsec")
    }
    implementation(libs.apache.wss4j.dom)
    // xml security
    implementation(libs.apache.xmlsec)
    // apache common
    implementation(libs.apache.commons.lang3)
    implementation(libs.apache.commons.io)
    // Apache Tika
    implementation(libs.apache.tika)
    // Apache POI
    implementation(libs.apache.poi)
    // dss tool
    implementation(platform(libs.dss.tool))
    implementation("eu.europa.ec.joinup.sd-dss:dss-document")
    implementation("eu.europa.ec.joinup.sd-dss:dss-xades") {
        exclude(group = "org.apache.santuario", module = "xmlsec")
    }
    implementation("eu.europa.ec.joinup.sd-dss:dss-cades")
    implementation("eu.europa.ec.joinup.sd-dss:dss-pades")
    implementation("eu.europa.ec.joinup.sd-dss:dss-asic-xades")
    implementation("eu.europa.ec.joinup.sd-dss:dss-asic-cades")
    implementation("eu.europa.ec.joinup.sd-dss:dss-token")
    implementation("eu.europa.ec.joinup.sd-dss:dss-service")
    implementation("eu.europa.ec.joinup.sd-dss:dss-policy-jaxb")
    implementation("eu.europa.ec.joinup.sd-dss:dss-validation")
    implementation("eu.europa.ec.joinup.sd-dss:dss-tsl-validation")
    implementation("eu.europa.ec.joinup.sd-dss:dss-certificate-validation-common")
    implementation("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons")
    implementation("eu.europa.ec.joinup.sd-dss:dss-pades-openpdf")
    implementation("eu.europa.ec.joinup.sd-dss:dss-crl-parser-stream")
    // spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-activemq")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // databases
    implementation(libs.h2)
    runtimeOnly(libs.mysql)
    runtimeOnly(libs.mariadb)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.oracle)
    // jakarta
    implementation(libs.jakarta.annotation)
    implementation(libs.jakarta.validation)
    implementation(libs.jakarta.xml.bind.api)
    // jaxb
    implementation(libs.jaxb.impl)
    // activemq
    implementation("org.apache.activemq:activemq-client")
    // jta
    runtimeOnly(libs.atomikos)
    // doc
    implementation(libs.spring.doc)
    // other
    implementation(libs.itextpdf)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)
    // generates META-INF/spring-configuration-metadata.json, used by the property reference of the documentation
    // (must come after Lombok so that the generated accessors are visible)
    annotationProcessor(platform(libs.spring.boot.bom))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation(libs.micrometer)
    implementation(libs.opencsv)
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation(libs.s3)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    // test
    testImplementation(testFixtures(project(":connector-domain")))
    testFixturesImplementation(project(":connector-domain"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")

    testFixturesImplementation(project(":connector-soap-api"))
    testFixturesImplementation(platform(libs.spring.boot.bom))
    testFixturesImplementation("org.springframework:spring-core")
    testFixturesImplementation("org.springframework:spring-jdbc")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesImplementation(libs.jakarta.activation)
    testFixturesImplementation(libs.jakarta.mail)
    testFixturesImplementation("eu.europa.ec.joinup.sd-dss:dss-enumerations:6.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}
configurations.testRuntimeClasspath {
    exclude(group = "com.atomikos")
}

val evidenceJaxbOutputDir = layout.buildDirectory.dir("generated/jaxb")
val evidenceXsdDir = file("src/main/resources/xsd")
val evidenceXjbFile = file("src/main/resources/xjb/spocseu.xjb")

sourceSets {
    main {
        java {
            srcDir(evidenceJaxbOutputDir)
        }
    }
}

tasks.register<JavaExec>("generateEvidenceJaxb") {
    group = "build"
    description = "Generate JAXB classes from REM evidence XSD sources"
    classpath = jaxbTool
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    val outDir = evidenceJaxbOutputDir.get().asFile
    args(
        "-d", outDir.absolutePath,
        "-b", evidenceXjbFile.absolutePath,
        file("$evidenceXsdDir/eDeliveryDetails.xsd").absolutePath,
        file("$evidenceXsdDir/TS102640_v2.xsd").absolutePath,
        file("$evidenceXsdDir/SPOCS_ts102640_soap_body.xsd").absolutePath,
    )
    inputs.files(
        evidenceXjbFile,
        file("$evidenceXsdDir/eDeliveryDetails.xsd"),
        file("$evidenceXsdDir/TS102640_v2.xsd"),
        file("$evidenceXsdDir/SPOCS_ts102640_soap_body.xsd"),
        file("$evidenceXsdDir/XAdES.xsd"),
        file("$evidenceXsdDir/XAdES132.xsd"),
        file("$evidenceXsdDir/xmldsig-core-schema.xsd"),
        file("$evidenceXsdDir/xenc-schema.xsd"),
        file("$evidenceXsdDir/xmlmime.xsd"),
        file("$evidenceXsdDir/saml-schema-assertion-2.0.xsd"),
        file("$evidenceXsdDir/ts_102231v030102_xsd.xsd"),
    )
    outputs.dir(outDir)
    doFirst {
        outDir.deleteRecursively()
        outDir.mkdirs()
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("generateEvidenceJaxb"))
}

tasks.named<JavaCompile>("compileTestJava") {
    dependsOn(tasks.named("generateEvidenceJaxb"))
}

tasks.named<JavaCompile>("compileTestFixturesJava") {
    dependsOn(tasks.named("generateEvidenceJaxb"))
}

tasks.test {
    // Avoid ConnectException noise: no OTLP collector is running during tests
    systemProperty("management.otlp.metrics.export.enabled", "false")
}
