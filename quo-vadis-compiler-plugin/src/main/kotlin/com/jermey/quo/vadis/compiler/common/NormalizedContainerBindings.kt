package com.jermey.quo.vadis.compiler.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

data class NormalizedTabsContainerBinding(
    val tabClassId: ClassId,
    val scopeKey: String,
    val wrapperKey: String,
    val wrapperFunctionFqn: FqName,
)

data class NormalizedPaneContainerBinding(
    val paneClassId: ClassId,
    val scopeKey: String,
    val wrapperKey: String,
    val wrapperFunctionFqn: FqName,
)

fun NavigationMetadata.normalizedTabsContainerBindings(): List<NormalizedTabsContainerBinding> {
    val tabsByClassId = tabs.associateBy { it.classId.normalizedContainerOwner() }
    val bindings = tabsContainers.map { container ->
        val normalizedTabClassId = container.tabClassId.normalizedContainerOwner()
        val tabsMetadata = tabsByClassId[normalizedTabClassId]
        // When @TabsContainer references a @Tabs class from a dependency module,
        // the @Tabs metadata won't be locally collected. Derive scope key from class name.
        val scopeKey = if (tabsMetadata != null) {
            tabsMetadata.name.ifEmpty { tabsMetadata.classId.shortClassName.asString() }
        } else {
            normalizedTabClassId.shortClassName.asString()
                .replaceFirstChar { it.lowercase() }
        }
        NormalizedTabsContainerBinding(
            tabClassId = tabsMetadata?.classId ?: container.tabClassId,
            scopeKey = scopeKey,
            wrapperKey = scopeKey,
            wrapperFunctionFqn = container.functionFqn,
        )
    }

    validateDuplicateBindings(
        bindings = bindings,
        wrapperKey = { it.wrapperKey },
        wrapperFunctionFqn = { it.wrapperFunctionFqn },
        annotationName = "@TabsContainer",
    )

    // @Tabs without a @TabsContainer in this module are silently skipped.
    // Cross-module tabs (e.g. navigation-api defining @Tabs, composeApp providing @TabsContainer)
    // will generate the binding in the module that has the wrapper.

    return bindings
}

fun NavigationMetadata.normalizedPaneContainerBindings(): List<NormalizedPaneContainerBinding> {
    if (paneContainers.isEmpty()) return emptyList()

    val panesByClassId = panes.associateBy { it.classId.normalizedContainerOwner() }
    val bindings = paneContainers.map { container ->
        val normalizedPaneClassId = container.paneClassId.normalizedContainerOwner()
        val paneMetadata = panesByClassId[normalizedPaneClassId]
            ?: error(
                "Quo Vadis compiler plugin: @PaneContainer wrapper ${container.functionFqn.asString()} " +
                    "targets ${container.paneClassId.asSingleFqName().asString()}, but no matching @Pane container was collected.",
            )
        val scopeKey = paneMetadata.name.ifEmpty { paneMetadata.classId.shortClassName.asString() }
        NormalizedPaneContainerBinding(
            paneClassId = paneMetadata.classId,
            scopeKey = scopeKey,
            wrapperKey = scopeKey,
            wrapperFunctionFqn = container.functionFqn,
        )
    }

    validateDuplicateBindings(
        bindings = bindings,
        wrapperKey = { it.wrapperKey },
        wrapperFunctionFqn = { it.wrapperFunctionFqn },
        annotationName = "@PaneContainer",
    )

    return bindings
}

private fun <T> validateDuplicateBindings(
    bindings: List<T>,
    wrapperKey: (T) -> String,
    wrapperFunctionFqn: (T) -> FqName,
    annotationName: String,
) {
    val duplicateBindings = bindings.groupBy(wrapperKey).filterValues { it.size > 1 }
    if (duplicateBindings.isEmpty()) return

    val duplicates = duplicateBindings.entries.joinToString { (key, entries) ->
        val wrappers = entries.joinToString { wrapperFunctionFqn(it).asString() }
        "$key -> [$wrappers]"
    }
    error(
        "Quo Vadis compiler plugin: multiple $annotationName wrappers resolved to the same wrapper key: $duplicates",
    )
}

private fun ClassId.normalizedContainerOwner(): ClassId =
    if (shortClassName.asString() == "Companion") {
        outerClassId ?: this
    } else {
        this
    }