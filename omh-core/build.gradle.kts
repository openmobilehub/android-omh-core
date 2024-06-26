import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

var properties = Properties()
var localPropertiesFile = project.rootProject.file("local.properties")
if(localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}
var useMavenLocal = (rootProject.ext.has("useMavenLocal") && rootProject.ext.get("useMavenLocal") == "true") || (properties.hasProperty("useMavenLocal") && properties.getProperty("useMavenLocal") == "true")

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.dokka") version "1.8.10"
    id("maven-publish")
    id("signing")
}

gradlePlugin {
    plugins {
        create("pluginRelease") {
            id = getPropertyOrFail("id")
            implementationClass = "com.openmobilehub.android.coreplugin.OMHCorePlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

if(useMavenLocal) {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
    }
}else{
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencies {
    implementation(BuildPlugins.android)
    implementation(BuildPlugins.kotlin)
}

// Publishing block
val sourceSets = the(SourceSetContainer::class)
val pluginSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from("dokkaJavadoc.outputDirectory")
}

artifacts {
    add("archives", pluginSourcesJar)
    add("archives", javadocJar)
}

val groupProperty = getPropertyOrFail("group")
val versionProperty = getPropertyOrFail("version")
val artifactId = getPropertyOrFail("id")
val mDescription = getPropertyOrFail("pluginDescription")

fun MavenPublication.setupPublication() {
    groupId = groupProperty
    artifactId = artifactId
    version = versionProperty

    from(project.components["java"])
    artifact(pluginSourcesJar)
    artifact(javadocJar)

    pom {
        name.set(artifactId)
        description.set(mDescription)
        url.set("https://github.com/openmobilehub/omh-core")
        licenses {
            license {
                name.set("Apache-2.0 License")
                url.set("https://github.com/openmobilehub/omh-core/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("Anwera64")
                name.set("Anton Soares")
            }
        }

        // Version control info - if you're using GitHub, follow the
        // format as seen here
        scm {
            connection.set("scm:git:github.com/openmobilehub/omh-core.git")
            developerConnection.set("scm:git:ssh://github.com/openmobilehub/omh-core.git")
            url.set("https://github.com/openmobilehub/omh-core")
        }
    }
}


if(useMavenLocal) {
    publishing {
        publications {
            register<MavenPublication>("pluginMaven") {
                group = groupProperty
                artifactId = artifactId
                version = versionProperty

                afterEvaluate {
                    // from(project.components["java"])
                    artifact(pluginSourcesJar)
                    artifact(javadocJar)
                }
            }
        }
    }
} else {
    group = groupProperty
    version = versionProperty

    afterEvaluate {
        publishing {
            publications {
                register("release", MavenPublication::class.java) {
                    setupPublication()
                }
            }
        }
    }

    signing {
        useInMemoryPgpKeys(
            rootProject.ext["signingKeyId"].toString(),
            rootProject.ext["signingKey"].toString(),
            rootProject.ext["signingPassword"].toString(),
        )
        sign(publishing.publications)
    }
}
