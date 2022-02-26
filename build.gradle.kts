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
    //implementation("org.antlr:antlr:3.5.2")
    implementation("org.antlr:ST4:4.3.1")


    implementation(
        files(
            //"antlr-3.4-complete.jar",
            "lib/clops-runtime.jar",
            "lib/testng-5.11-modified.jar",
            "lib/testng-6.3.2beta-modified.jar"
        )
    )


    implementation("org.key:jmlparser-core:3.24.1-SNAPSHOT")
    implementation("org.key:jmlparser-symbol-solver-core:3.24.1-SNAPSHOT")

    implementation("info.picocli:picocli:4.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

}


repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://maven.pkg.github.com/wadoon/jmlparser")
        credentials {
            username = project.findProperty("gpr.user")?.toString()
            password = project.findProperty("gpr.key")?.toString()
        }
    }
}

application {
    mainClass.set("org.jmlspecs.jmlunitng.JMLUnitNG")
}

tasks.test {
    useJUnitPlatform()
}
