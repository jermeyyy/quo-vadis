@file:OptIn(UnsafeDuringIrConstructionAPI::class)
@file:Suppress("DEPRECATION")

package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.util.jar.JarFile

/**
 * Discovers `@GeneratedConfig`-annotated [NavigationConfig] implementations
 * across all dependency modules and the current module by scanning the
 * well-known generated package.
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
    private val configuration: CompilerConfiguration,
) {
    fun discoverGeneratedConfigs(): List<IrClassSymbol> {
        val configs = mutableMapOf<String, IrClassSymbol>()

        // Scan dependency modules via descriptor-based package enumeration
        discoverFromDependencies(configs)

        // Scan classpath roots because package views may miss sibling project outputs
        discoverFromClasspath(configs)

        // Scan current module's IR files as supplement
        discoverFromCurrentModule(configs)

        return configs.values.sortedBy { it.owner.fqNameWhenAvailable?.asString() ?: "" }
    }

    /**
     * Enumerates classifier names in [GENERATED_PACKAGE] using the module
     * descriptor's package view, which includes contributions from all
     * transitive dependencies.
     */
    private fun discoverFromDependencies(
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        val packageView = moduleFragment.descriptor.getPackage(GENERATED_PACKAGE)
        val classifierNames = packageView.memberScope.getClassifierNames() ?: return

        for (name in classifierNames) {
            if (isExcludedByName(name)) continue
            val classSymbol = pluginContext.referenceClass(ClassId(GENERATED_PACKAGE, name)) ?: continue
            if (!hasGeneratedConfigAnnotation(classSymbol)) continue

            val fqn = classSymbol.owner.fqNameWhenAvailable?.asString() ?: continue
            configs[fqn] = classSymbol
        }
    }

    private fun discoverFromClasspath(
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)

        contentRoots
            .filterIsInstance<JvmClasspathRoot>()
            .mapNotNull(::extractRootFile)
            .distinct()
            .forEach { root ->
                when {
                    root.isDirectory -> discoverFromDirectory(root, configs)
                    root.isFile && root.extension == "jar" -> discoverFromJar(root, configs)
                }
            }
    }

    private fun extractRootFile(root: JvmClasspathRoot): File? {
        val getter = root.javaClass.methods.firstOrNull {
            it.name == "getFile" && it.parameterCount == 0
        } ?: return null
        return getter.invoke(root) as? File
    }

    private fun discoverFromDirectory(
        root: File,
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        val generatedDir = root.resolve(GENERATED_PACKAGE_PATH)
        if (!generatedDir.isDirectory) return

        generatedDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == "class" }
            ?.map { it.name.removeSuffix(".class") }
            ?.forEach { discoverCandidate(it, configs) }
    }

    private fun discoverFromJar(
        jarFile: File,
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .map { it.name }
                .filter { entry ->
                    entry.startsWith("$GENERATED_PACKAGE_PATH/") &&
                        entry.endsWith(".class") &&
                        !entry.substringAfterLast('/').contains('$')
                }
                .map { entry -> entry.substringAfterLast('/').removeSuffix(".class") }
                .forEach { discoverCandidate(it, configs) }
        }
    }

    private fun discoverCandidate(
        simpleName: String,
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        if (!simpleName.endsWith("NavigationConfig")) return

        val name = Name.identifier(simpleName)
        if (isExcludedByName(name)) return

        val classSymbol = pluginContext.referenceClass(ClassId(GENERATED_PACKAGE, name)) ?: return
        if (!hasGeneratedConfigAnnotation(classSymbol)) return

        val fqn = classSymbol.owner.fqNameWhenAvailable?.asString() ?: return
        configs[fqn] = classSymbol
    }

    /**
     * Walks [IrModuleFragment.files] to discover configs generated during the
     * current compilation that may not yet be visible through descriptors.
     */
    private fun discoverFromCurrentModule(
        configs: MutableMap<String, IrClassSymbol>,
    ) {
        for (file in moduleFragment.files) {
            if (file.packageFqName != GENERATED_PACKAGE) continue
            for (declaration in file.declarations) {
                if (declaration !is IrClass) continue
                if (isExcludedByName(declaration.name)) continue
                if (!hasGeneratedConfigAnnotation(declaration.symbol)) continue

                val fqn = declaration.fqNameWhenAvailable?.asString() ?: continue
                configs[fqn] = declaration.symbol
            }
        }
    }

    private fun isExcludedByName(name: Name): Boolean {
        val nameStr = name.asString()
        return nameStr in EXCLUDED_NAMES || AGGREGATED_CONFIG_SUFFIX in nameStr
    }

    private fun hasGeneratedConfigAnnotation(
        classSymbol: IrClassSymbol,
    ): Boolean {
        return classSymbol.owner.hasAnnotation(GENERATED_CONFIG_ANNOTATION_FQN)
    }

    private companion object {
        val GENERATED_PACKAGE = FqName("com.jermey.quo.vadis.generated")
        const val GENERATED_PACKAGE_PATH = "com/jermey/quo/vadis/generated"
        val GENERATED_CONFIG_ANNOTATION_FQN = FqName("com.jermey.quo.vadis.core.navigation.config.GeneratedConfig")
        val EXCLUDED_NAMES = setOf(
            "EmptyNavigationConfig",
            "CompositeNavigationConfig",
        )
        const val AGGREGATED_CONFIG_SUFFIX = "__AggregatedConfig"
    }
}
