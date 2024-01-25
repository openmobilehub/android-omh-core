import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

var properties = Properties()
var localPropertiesFile = project.rootProject.file("local.properties")
if(localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}
var useMavenLocal = (rootProject.ext.has("useMavenLocal") && rootProject.ext.get("useMavenLocal") == "true") || (properties.hasProperty("useMavenLocal") && properties.getProperty("useMavenLocal") == "true")

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

buildscript {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }

    dependencies {
        classpath(BuildPlugins.kotlin)
        classpath(BuildPlugins.detekt)
    }
}

subprojects {
    if(useMavenLocal) {
        repositories {
            mavenLocal()
            google()
            mavenCentral()
        }
    } else {
        repositories {
            mavenCentral()
            google()
            mavenLocal()
        }
    }
}

tasks.register("installPreCommitHook", Copy::class) {
    from("tools/scripts/pre-commit")
    into(".git/hooks")
    fileMode = 0b000_111_111_111
}

tasks {
    val installPreCommitHook by existing
    getByName("prepareKotlinBuildScriptModel").dependsOn(installPreCommitHook)
}

if(!useMavenLocal) {
    val ossrhUsername by extra(getValueFromEnvOrProperties("OSSRH_USERNAME"))
    val ossrhPassword by extra(getValueFromEnvOrProperties("OSSRH_PASSWORD"))
    val mStagingProfileId by extra(getValueFromEnvOrProperties("SONATYPE_STAGING_PROFILE_ID"))
    val signingKeyId by extra(getValueFromEnvOrProperties("SIGNING_KEY_ID"))
    val signingPassword by extra(getValueFromEnvOrProperties("SIGNING_PASSWORD"))
    val signingKey by extra(getValueFromEnvOrProperties("SIGNING_KEY"))

    // Set up Sonatype repository
    nexusPublishing {
        repositories {
            sonatype {
                stagingProfileId.set(mStagingProfileId.toString())
                username.set(ossrhUsername.toString())
                password.set(ossrhPassword.toString())
                // Add these lines if using new Sonatype infra
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            }
        }
    }
}

fun getValueFromEnvOrProperties(name: String): Any? {
    val localProperties =
        com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir)
    return System.getenv(name) ?: localProperties[name]
}
