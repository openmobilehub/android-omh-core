package com.omh.android.coreplugin.model

import com.omh.android.coreplugin.utils.AUTH_GMS_ADDRESS
import com.omh.android.coreplugin.utils.AUTH_NGMS_ADDRESS
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

open class Bundle @Inject constructor(project: Project) {

    internal val id: Property<String> = project.objects.property(String::class.java)
    private val auth: Service = project.objects.newInstance(
        Service::class.java,
        Service.AUTH,
        AUTH_GMS_ADDRESS,
        AUTH_NGMS_ADDRESS
    )
    private val maps: Service = project.objects.newInstance(
        Service::class.java,
        Service.MAPS,
        "",
        ""
    ) // TODO ADD REFLECTION PATHS
    private val storage: Service = project.objects.newInstance(
        Service::class.java,
        Service.STORAGE,
        "",
        ""
    ) // TODO ADD REFLECTION PATHS

    private val enabledServices: MutableSet<Service> = mutableSetOf()
    private val dependencies = mutableListOf<String>()

    fun auth(configuration: Action<in Service>) {
        enabledServices.add(auth)
        configuration.execute(auth)
    }

    fun maps(configuration: Action<in Service>) {
        enabledServices.add(maps)
        configuration.execute(maps)
    }

    fun storage(configuration: Action<in Service>) {
        enabledServices.add(storage)
        configuration.execute(storage)
    }

    internal fun getDependencies(): List<String> {
        dependencies.clear()
        enabledServices.forEach(::addConfiguredDependencies)
        return dependencies
    }

    private fun addConfiguredDependencies(service: Service) {
        if (service.isGmsDependencySet) {
            dependencies.add(service.gmsService)
        }
        if (service.isNonGmsDependencySet) {
            dependencies.add(service.nonGmsService)
        }
    }

    fun getReflectionPaths(): Map<String, String> {
        val pathMap = mutableMapOf<String, String>()
        for (service in enabledServices) {
            addPathsFromService(pathMap, service)
        }
        return pathMap
    }

    private fun addPathsFromService(
        pathMap: MutableMap<String, String>,
        service: Service
    ) {
        pathMap["${service.key}_GMS_PATH"] = selectPathValue(
            validator = service::isGmsDependencySet,
            getter = service::gmsPath
        )
        pathMap["${service.key}_NON_GMS_PATH"] = selectPathValue(
            validator = service::isNonGmsDependencySet,
            getter = service::nonGmsPath
        )
    }

    private fun selectPathValue(validator: () -> Boolean, getter: () -> String): String {
        // NULL will be converted to the primitive type in the BuildConfigField.
        return if (validator()) getter() else "null"
    }
}
