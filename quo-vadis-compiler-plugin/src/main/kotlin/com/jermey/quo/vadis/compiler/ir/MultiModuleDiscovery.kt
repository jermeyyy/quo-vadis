@file:OptIn(UnsafeDuringIrConstructionAPI::class)
@file:Suppress("DEPRECATION")

package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Discovers [GeneratedNavigationConfig] implementations across all dependency
 * modules and the current module by scanning the well-known generated package.
 *
 * Uses two complementary strategies:
 * 1. **Descriptor-based scanning** – enumerates classifier names from the
 *    generated package across all dependency modules via [ModuleDescriptor].
 * 2. **IR file walking** – scans the current module's IR files to catch
 *    configs synthesized during the current compilation.
 */
class MultiModuleDiscovery(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment,
) {
    fun discoverGeneratedConfigs(): List<IrClassSymbol> {
        val markerClass = pluginContext.referenceClass(GENERATED_NAV_CONFIG_CLASS_ID)
            ?: return emptyList()

        val configs = mutableMapOf<String, IrClassSymbol>()

        // Scan dependency modules via descriptor-based package enumeration
        discoverFromDependencies(markerClass, configs)

        // Scan current module's IR files as supplement
        discoverFromCurrentModule(markerClass, configs)

        return configs.values.sortedBy { it.owner.fqNameWhenAvailable?.asString() ?: "" }
    }

    /**
     * Enumerates classifier names in [GENERATED_PACKAGE] using the module
     * descriptor's package view, which includes contributions from all
     * transitive dependencies.
     */
    private fun discoverFromDependencies(
        markerClass: IrClassSymbol,
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        val packageView = moduleFragment.descriptor.getPackage(GENERATED_PACKAGE)
        val classifierNames = packageView.memberScope.getClassifierNames() ?: return

        for (name in classifierNames) {
            if (isExcludedByName(name)) continue
            val classSymbol = pluginContext.referenceClass(ClassId(GENERATED_PACKAGE, name)) ?: continue
            if (!implementsMarker(classSymbol, markerClass)) continue

            val fqn = classSymbol.owner.fqNameWhenAvailable?.asString() ?: continue
            configs[fqn] = classSymbol
        }
    }

    /**
     * Walks [IrModuleFragment.files] to discover configs generated during the
     * current compilation that may not yet be visible through descriptors.
     */
    private fun discoverFromCurrentModule(
        markerClass: IrClassSymbol,
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        for (file in moduleFragment.files) {
            if (file.packageFqName != GENERATED_PACKAGE) continue
            for (declaration in file.declarations) {
                if (declaration !is IrClass) continue
                if (isExcludedByName(declaration.name)) continue
                if (!implementsMarker(declaration.symbol, markerClass)) continue

                val fqn = declaration.fqNameWhenAvailable?.asString() ?: continue
                configs[fqn] = declaration.symbol
            }
        }
    }

    private fun isExcludedByName(name: Name): Boolean {
        val nameStr = name.asString()
        return nameStr in EXCLUDED_NAMES || AGGREGATED_CONFIG_SUFFIX in nameStr
    }

    private fun implementsMarker(
        classSymbol: IrClassSymbol,
        markerClass: IrClassSymbol,
    ): Boolean {
        return classSymbol.owner.superTypes.any { it.classOrNull == markerClass }
    }

    private companion object {
        val GENERATED_PACKAGE = FqName("com.jermey.quo.vadis.generated")
        val GENERATED_NAV_CONFIG_CLASS_ID = ClassId(
            FqName("com.jermey.quo.vadis.core.navigation.config"),
            Name.identifier("GeneratedNavigationConfig"),
        )
        val EXCLUDED_NAMES = setOf(
            "EmptyNavigationConfig",
            "CompositeNavigationConfig",
            "GeneratedNavigationConfig",
        )
        const val AGGREGATED_CONFIG_SUFFIX = "__AggregatedConfig"
    }
}
