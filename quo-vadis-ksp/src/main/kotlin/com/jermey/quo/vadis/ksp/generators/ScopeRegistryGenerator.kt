package com.jermey.quo.vadis.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.QuoVadisClassNames
import com.jermey.quo.vadis.ksp.generators.base.StringTemplates
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a ScopeRegistry implementation from @Tab, @Pane, and @Stack annotations.
 *
 * The generated registry maps scope keys (sealed class simple names) to the set of
 * destinations that belong to that scope. Used by TreeMutator to determine whether
 * a destination should be pushed inside or outside a container.
 *
 * ## Generated Code Example
 *
 * ```kotlin
 * public object GeneratedScopeRegistry : ScopeRegistry {
 *     private val scopeMap: Map<String, Set<KClass<out Destination>>> = mapOf(
 *         "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class),
 *         "MasterDetailDestination" to setOf(MasterDetailDestination.List::class, ...),
 *         "AuthFlow" to setOf(AuthFlow.Login::class, AuthFlow.Register::class)
 *     )
 *
 *     override fun isInScope(scopeKey: String, destination: Destination): Boolean {
 *         val scopes = scopeMap[scopeKey] ?: return true
 *         return scopes.any { it.isInstance(destination) }
 *     }
 *
 *     override fun getScopeKey(destination: Destination): String? {
 *         return scopeMap.entries.find { (_, classes) ->
 *             classes.any { it.isInstance(destination) }
 *         }?.key
 *     }
 * }
 * ```
 *
 * @property codeGenerator KSP code generator for writing output files
 * @property logger KSP logger for diagnostic output
 */
public class ScopeRegistryGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    private companion object {
        private const val GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"
        private const val GENERATED_FILE_NAME = "GeneratedScopeRegistry"
        private const val GENERATED_CLASS_NAME = "GeneratedScopeRegistry"
    }

    /**
     * Generate the ScopeRegistry implementation.
     *
     * @param tabInfos List of all TabInfo extracted from @Tab annotations
     * @param paneInfos List of all PaneInfo extracted from @Pane annotations
     * @param stackInfos List of all StackInfo extracted from @Stack annotations
     * @param packageName Target package for generated code (unused, kept for API consistency)
     * @param addDeprecations When true, adds @Deprecated annotations to generated code
     *        pointing users to use GeneratedNavigationConfig instead
     */
    public fun generate(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        stackInfos: List<StackInfo> = emptyList(),
        @Suppress("UNUSED_PARAMETER") packageName: String = GENERATED_PACKAGE,
        addDeprecations: Boolean = false
    ) {
        // Only generate if there are tabs, panes, or stacks
        if (tabInfos.isEmpty() && paneInfos.isEmpty() && stackInfos.isEmpty()) {
            logger.info("No @Tab, @Pane, or @Stack annotations found, skipping ScopeRegistry generation")
            return
        }

        val fileSpec = buildFileSpec(tabInfos, paneInfos, stackInfos, addDeprecations)

        // Collect all source files for dependency tracking
        val sourceFiles = buildList {
            tabInfos.forEach { add(it.classDeclaration.containingFile) }
            paneInfos.forEach { add(it.classDeclaration.containingFile) }
            stackInfos.forEach { add(it.classDeclaration.containingFile) }
        }.filterNotNull()

        val dependencies = Dependencies(
            aggregating = true,
            sources = sourceFiles.toTypedArray()
        )

        fileSpec.writeTo(codeGenerator, dependencies)
        logger.info("Generated $GENERATED_FILE_NAME with ${tabInfos.size} tabs, ${paneInfos.size} panes, and ${stackInfos.size} stacks")
    }

    private fun buildFileSpec(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        stackInfos: List<StackInfo>,
        addDeprecations: Boolean
    ): FileSpec {
        return FileSpec.builder(GENERATED_PACKAGE, GENERATED_FILE_NAME)
            .addFileComment("DO NOT EDIT - Auto-generated by Quo Vadis KSP processor")
            .addType(buildScopeRegistryObject(tabInfos, paneInfos, stackInfos, addDeprecations))
            .build()
    }

    private fun buildScopeRegistryObject(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        stackInfos: List<StackInfo>,
        addDeprecations: Boolean
    ): TypeSpec {
        return TypeSpec.objectBuilder(GENERATED_CLASS_NAME)
            .addKdoc(
                """
                |KSP-generated ScopeRegistry implementation.
                |
                |Maps scope keys to their member destination classes for scope-aware navigation.
                |Used by TreeMutator to determine whether a destination belongs to a tab/pane/stack scope.
                |
                |Generated from @Tab, @Pane, and @Stack annotations found in the codebase.
                |DO NOT EDIT - This file is auto-generated by Quo Vadis KSP processor.
                """.trimMargin()
            )
            .apply {
                if (addDeprecations) {
                    addAnnotation(buildDeprecationAnnotation())
                }
            }
            .addSuperinterface(QuoVadisClassNames.SCOPE_REGISTRY)
            .addProperty(buildScopeMapProperty(tabInfos, paneInfos, stackInfos))
            .addFunction(buildIsInScopeFunction())
            .addFunction(buildGetScopeKeyFunction())
            .build()
    }

    /**
     * Builds the @Deprecated annotation for legacy registry objects.
     */
    private fun buildDeprecationAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(Deprecated::class)
            .addMember("message = %S", StringTemplates.deprecatedRegistryMessage(GENERATED_CLASS_NAME))
            .addMember(
                "replaceWith = %T(%S, %S)",
                ReplaceWith::class,
                "GeneratedNavigationConfig",
                "$GENERATED_PACKAGE.GeneratedNavigationConfig"
            )
            .addMember("level = %T.%L", DeprecationLevel::class, "WARNING")
            .build()
    }

    private fun buildScopeMapProperty(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        stackInfos: List<StackInfo>
    ): PropertySpec {
        val kClassType = ClassName("kotlin.reflect", "KClass")
            .parameterizedBy(WildcardTypeName.producerOf(QuoVadisClassNames.DESTINATION))
        val setType = SET.parameterizedBy(kClassType)
        val mapType = MAP.parameterizedBy(STRING, setType)

        val codeBlock = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()

        val entries = mutableListOf<CodeBlock>()

        // Add tab scopes
        for (tabInfo in tabInfos) {
            val scopeKey = tabInfo.className // e.g., "MainTabs"
            val memberClassRefs = tabInfo.tabs.map { tabItem ->
                // Build the proper ClassName for the tab item
                val tabClassName = buildClassName(tabItem.classDeclaration)
                CodeBlock.of("%T::class", tabClassName)
            }
            if (memberClassRefs.isNotEmpty()) {
                val setOfClasses = CodeBlock.builder()
                    .add("setOf(")
                    .add(memberClassRefs.joinToCode(", "))
                    .add(")")
                    .build()
                entries.add(CodeBlock.of("%S to %L", scopeKey, setOfClasses))
            }
        }

        // Add pane scopes
        for (paneInfo in paneInfos) {
            val scopeKey = paneInfo.className
            val memberClassRefs = paneInfo.panes.map { paneItem ->
                // Build the proper ClassName for the pane destination
                val paneClassName = buildClassName(paneItem.rootGraphClass)
                CodeBlock.of("%T::class", paneClassName)
            }
            if (memberClassRefs.isNotEmpty()) {
                val setOfClasses = CodeBlock.builder()
                    .add("setOf(")
                    .add(memberClassRefs.joinToCode(", "))
                    .add(")")
                    .build()
                entries.add(CodeBlock.of("%S to %L", scopeKey, setOfClasses))
            }
        }

        // Add stack scopes
        for (stackInfo in stackInfos) {
            val scopeKey = stackInfo.className // e.g., "AuthFlow"
            val memberClassRefs = stackInfo.destinations.map { destInfo ->
                // Build the proper ClassName for the stack destination
                val destClassName = buildClassName(destInfo.classDeclaration)
                CodeBlock.of("%T::class", destClassName)
            }
            if (memberClassRefs.isNotEmpty()) {
                val setOfClasses = CodeBlock.builder()
                    .add("setOf(")
                    .add(memberClassRefs.joinToCode(", "))
                    .add(")")
                    .build()
                entries.add(CodeBlock.of("%S to %L", scopeKey, setOfClasses))
            }
        }

        if (entries.isNotEmpty()) {
            codeBlock.add(entries.joinToCode(",\n"))
        }

        codeBlock.unindent()
        codeBlock.add("\n)")

        return PropertySpec.builder("scopeMap", mapType)
            .addModifiers(KModifier.PRIVATE)
            .initializer(codeBlock.build())
            .build()
    }

    /**
     * Builds a ClassName for a KSClassDeclaration, handling nested classes.
     */
    private fun buildClassName(
        classDeclaration: com.google.devtools.ksp.symbol.KSClassDeclaration
    ): ClassName {
        val packageName = classDeclaration.packageName.asString()
        val simpleNames = mutableListOf<String>()

        // Walk up the parent chain to collect all enclosing class names
        var current: com.google.devtools.ksp.symbol.KSDeclaration? = classDeclaration
        while (current is com.google.devtools.ksp.symbol.KSClassDeclaration) {
            simpleNames.add(0, current.simpleName.asString())
            current = current.parentDeclaration
        }

        return ClassName(packageName, simpleNames)
    }

    private fun buildIsInScopeFunction(): FunSpec {
        return FunSpec.builder("isInScope")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("scopeKey", STRING)
            .addParameter("destination", QuoVadisClassNames.DESTINATION)
            .returns(BOOLEAN)
            .addStatement("val scopes = scopeMap[scopeKey] ?: return true")
            .addStatement("return scopes.any { it.isInstance(destination) }")
            .build()
    }

    private fun buildGetScopeKeyFunction(): FunSpec {
        return FunSpec.builder("getScopeKey")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destination", QuoVadisClassNames.DESTINATION)
            .returns(STRING.copy(nullable = true))
            .beginControlFlow("return scopeMap.entries.find { (_, classes) ->")
            .addStatement("classes.any { it.isInstance(destination) }")
            .endControlFlow()
            .addStatement("?.key")
            .build()
    }
}
