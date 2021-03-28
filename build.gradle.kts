plugins {
    java
    `java-library`
    application
}

repositories {
    mavenCentral()
    flatDir {
        dir("lib")
    }
}

dependencies {
    implementation("org.antlr:antlr:3.5.2")
    implementation("org.antlr:ST4:4.3.1")


    implementation(
        files(
            //"antlr-3.4-complete.jar",
            "lib/clops-runtime.jar",
            "lib/jml4rt.jar",
            "lib/jmlruntime.jar",
            "lib/jmlspecs.jar",
            "lib/openjml.jar",
            "lib/testng-5.11-modified.jar",
            "lib/testng-6.3.2beta-modified.jar"
        )
    )

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("org.jmlspecs.jmlunitng.JMLUnitNG")
}

tasks.test {
    useJUnitPlatform()
}
