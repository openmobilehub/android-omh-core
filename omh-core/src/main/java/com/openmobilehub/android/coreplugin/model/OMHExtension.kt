package com.openmobilehub.android.coreplugin.model

import com.openmobilehub.android.coreplugin.utils.ERROR_BUNDLES_BUT_NO_SERVICES
import com.openmobilehub.android.coreplugin.utils.ERROR_BUNDLES_WITHOUT_NAME
import com.openmobilehub.android.coreplugin.utils.ERROR_BUNDLES_WITH_INCORRECT_NAME
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty

open class OMHExtension @Inject constructor(private val project: Project) {
    private val enableLocalProjectsProperty: Property<Boolean> = project.objects.property(Boolean::class.java)
    var enableLocalProjects: Boolean
        set(value) = enableLocalProjectsProperty.set(value)
        get() = enableLocalProjectsProperty.orNull ?: false

    private val bundles: MapProperty<String, Bundle> = project.objects.mapProperty(
        String::class.java,
        Bundle::class.java
    )

    fun bundle(id: String, configuration: Action<in Bundle>) {
        val bundle = project.objects.newInstance(Bundle::class.java, project)
        bundle.id.set(id.trim())
        configuration.execute(bundle)
        bundles.put(bundle.id.get(), bundle)
    }

    //region Validations

    private fun getDependenciesFromAllBundles(): List<String> {
        val dependenciesInAllBundlesList = mutableListOf<String>()
        if (bundles.isPresent && bundles.get().isNotEmpty()) {
            bundles.get().values.forEach {
                dependenciesInAllBundlesList.addAll(it.getDependencies())
            }
        }
        return dependenciesInAllBundlesList
    }

    private fun isThereANamelessBundle(bundlesMap: Map<String, Bundle>): Boolean {
        bundlesMap.forEach { bundle ->
            if (bundle.key.isEmpty()) return true
        }
        return false
    }

    private fun isThereABundleWithIncorrectName(bundlesMap: Map<String, Bundle>): Boolean {
        bundlesMap.forEach { bundle ->
            if (!bundleNameRegex.matches(bundle.key)) return true
        }
        return false
    }

    /**
     * Validate that bundles and services are required at the time of using the plugin in clients.
     */
    @SuppressWarnings("CyclomaticComplexMethod")
    fun validateIntegrity() {
        var errorMsg = ""

        // Bundles Integrity
        if (errorMsg.isEmpty() && bundles.isPresent && bundles.get().isNotEmpty()) {
            val bundlesMap = bundles.get()
            if (isThereANamelessBundle(bundlesMap)) {
                errorMsg = ERROR_BUNDLES_WITHOUT_NAME
            }
            if (errorMsg.isEmpty() && isThereABundleWithIncorrectName(bundlesMap)) {
                errorMsg = ERROR_BUNDLES_WITH_INCORRECT_NAME
            }
            if (errorMsg.isEmpty() && getDependenciesFromAllBundles().isEmpty() && !enableLocalProjects) {
                errorMsg = ERROR_BUNDLES_BUT_NO_SERVICES
            }
        }

        if (errorMsg.isNotEmpty()) throw InvalidUserDataException(errorMsg)
    }
    //endregion

    internal fun getBundles() = bundles

    companion object {
        private val bundleNameRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(?:_[a-zA-Z0-9_]+)*$")
    }
}
