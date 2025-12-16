package com.jermey.quo.vadis.ksp.generators.base

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

/**
 * Shared utilities for building KotlinPoet CodeBlocks.
 *
 * Provides type-safe builders for common patterns used across generators:
 * - DSL block generation (screens, tabs, stacks, panes)
 * - When expressions
 * - Collection initializers
 * - Lambda expressions
 *
 * ## Usage
 *
 * ```kotlin
 * val screenBlock = CodeBlockBuilders.buildScreenBlock(
 *     destinationClass = ClassName("com.example", "HomeDestination"),
 *     screenContent = CodeBlock.of("HomeScreen(navigator = navigator)")
 * )
 * ```
 *
 * All methods are stateless and can be called directly on the object.
 */
object CodeBlockBuilders {

    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN BLOCKS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a `screen<DestinationType> { }` DSL block.
     *
     * @param destinationClass The destination class type parameter
     * @param screenContent The composable function call CodeBlock
     * @return CodeBlock for the screen registration
     */
    fun buildScreenBlock(
        destinationClass: ClassName,
        screenContent: CodeBlock
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("screen<%T>", destinationClass)
            .add(screenContent)
            .endControlFlow()
            .build()
    }

    /**
     * Builds a `screen<DestinationType> { }` DSL block with simple function call.
     *
     * @param destinationClass The destination class type parameter
     * @param functionName The composable function name to invoke
     * @param hasNavigator Whether to pass navigator parameter
     * @param hasDestination Whether to pass destination parameter
     * @return CodeBlock for the screen registration
     */
    fun buildSimpleScreenBlock(
        destinationClass: ClassName,
        functionName: String,
        hasNavigator: Boolean = true,
        hasDestination: Boolean = false
    ): CodeBlock {
        val params = buildList {
            if (hasDestination) add("destination = it")
            if (hasNavigator) add("navigator = navigator")
        }.joinToString(", ")

        return CodeBlock.builder()
            .addStatement("screen<%T> { %L(%L) }", destinationClass, functionName, params)
            .build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTAINER BLOCKS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a `tabs<ContainerType> { }` DSL block.
     *
     * @param containerClass The container destination class type parameter
     * @param scopeKey The scope key for this container
     * @param wrapperKey Optional wrapper key
     * @param tabsContent CodeBlock containing tab definitions
     * @return CodeBlock for the tabs container
     */
    fun buildTabsBlock(
        containerClass: ClassName,
        scopeKey: String,
        wrapperKey: String? = null,
        tabsContent: CodeBlock
    ): CodeBlock {
        val builder = CodeBlock.builder()

        val params = buildList {
            add("scopeKey = %S")
            if (wrapperKey != null) add("wrapperKey = %S")
        }.joinToString(", ")

        val args = buildList<Any> {
            add(containerClass)
            add(scopeKey)
            if (wrapperKey != null) add(wrapperKey)
        }

        builder.beginControlFlow("tabs<%T>($params)", *args.toTypedArray())
        builder.add(tabsContent)
        builder.endControlFlow()

        return builder.build()
    }

    /**
     * Builds a `stack<ContainerType> { }` DSL block.
     *
     * @param containerClass The container destination class type parameter
     * @param scopeKey The scope key for this container
     * @param stackContent CodeBlock containing stack screen definitions
     * @return CodeBlock for the stack container
     */
    fun buildStackBlock(
        containerClass: ClassName,
        scopeKey: String,
        stackContent: CodeBlock
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("stack<%T>(scopeKey = %S)", containerClass, scopeKey)
            .add(stackContent)
            .endControlFlow()
            .build()
    }

    /**
     * Builds a `panes<ContainerType> { }` DSL block.
     *
     * @param containerClass The container destination class type parameter
     * @param scopeKey The scope key for this container
     * @param wrapperKey Optional wrapper key
     * @param panesContent CodeBlock containing pane definitions
     * @return CodeBlock for the panes container
     */
    fun buildPanesBlock(
        containerClass: ClassName,
        scopeKey: String,
        wrapperKey: String? = null,
        panesContent: CodeBlock
    ): CodeBlock {
        val builder = CodeBlock.builder()

        val params = buildList {
            add("scopeKey = %S")
            if (wrapperKey != null) add("wrapperKey = %S")
        }.joinToString(", ")

        val args = buildList<Any> {
            add(containerClass)
            add(scopeKey)
            if (wrapperKey != null) add(wrapperKey)
        }

        builder.beginControlFlow("panes<%T>($params)", *args.toTypedArray())
        builder.add(panesContent)
        builder.endControlFlow()

        return builder.build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAB ENTRIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a `tab()` entry for tabs container.
     *
     * @param destination The destination instance expression
     * @param title Optional tab title
     * @param icon Optional icon expression
     * @return CodeBlock for the tab entry
     */
    fun buildTabEntry(
        destination: String,
        title: String? = null,
        icon: String? = null
    ): CodeBlock {
        val params = buildList {
            add(destination)
            if (title != null) add("title = \"$title\"")
            if (icon != null) add("icon = $icon")
        }.joinToString(", ")

        return CodeBlock.of("tab($params)\n")
    }

    /**
     * Builds a `tab()` entry with nested stack content.
     *
     * @param destination The destination instance expression
     * @param title Optional tab title
     * @param icon Optional icon expression
     * @param stackContent CodeBlock containing nested stack screens
     * @return CodeBlock for the nested tab entry
     */
    fun buildNestedTabEntry(
        destination: String,
        title: String? = null,
        icon: String? = null,
        stackContent: CodeBlock
    ): CodeBlock {
        val params = buildList {
            add(destination)
            if (title != null) add("title = \"$title\"")
            if (icon != null) add("icon = $icon")
        }.joinToString(", ")

        return CodeBlock.builder()
            .beginControlFlow("tab($params)")
            .add(stackContent)
            .endControlFlow()
            .build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCOPE AND TRANSITION BLOCKS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a `scope()` DSL call with multiple destination classes.
     *
     * Returns an empty CodeBlock if the destination list is empty.
     *
     * @param scopeKey The scope key identifier
     * @param destinationClasses List of destination classes in the scope
     * @return CodeBlock for the scope definition
     */
    fun buildScopeBlock(
        scopeKey: String,
        destinationClasses: List<ClassName>
    ): CodeBlock {
        if (destinationClasses.isEmpty()) return CodeBlock.of("")

        val builder = CodeBlock.builder()
        builder.add("scope(%S", scopeKey)

        destinationClasses.forEach { className ->
            builder.add(",\n    %T::class", className)
        }

        builder.add("\n)\n")
        return builder.build()
    }

    /**
     * Builds a `transition<DestinationType>()` DSL call.
     *
     * @param destinationClass The destination class type parameter
     * @param transitionExpression The transition instance expression
     * @return CodeBlock for the transition registration
     */
    fun buildTransitionBlock(
        destinationClass: ClassName,
        transitionExpression: String
    ): CodeBlock {
        return CodeBlock.of("transition<%T>($transitionExpression)\n", destinationClass)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRAPPER BLOCKS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a `tabWrapper()` DSL block.
     *
     * @param wrapperKey The wrapper key identifier
     * @param wrapperContent CodeBlock containing wrapper composable content
     * @return CodeBlock for the tab wrapper registration
     */
    fun buildTabWrapperBlock(
        wrapperKey: String,
        wrapperContent: CodeBlock
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("tabWrapper(%S)", wrapperKey)
            .add(wrapperContent)
            .endControlFlow()
            .build()
    }

    /**
     * Builds a `paneWrapper()` DSL block.
     *
     * @param wrapperKey The wrapper key identifier
     * @param wrapperContent CodeBlock containing wrapper composable content
     * @return CodeBlock for the pane wrapper registration
     */
    fun buildPaneWrapperBlock(
        wrapperKey: String,
        wrapperContent: CodeBlock
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("paneWrapper(%S)", wrapperKey)
            .add(wrapperContent)
            .endControlFlow()
            .build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTROL FLOW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a when expression with type matching cases.
     *
     * @param subject The expression being matched (e.g., "destination")
     * @param cases List of pairs: (type pattern, result CodeBlock)
     * @param elseCase Optional else case CodeBlock
     * @return CodeBlock for the when expression
     */
    fun buildWhenExpression(
        subject: String,
        cases: List<Pair<String, CodeBlock>>,
        elseCase: CodeBlock? = null
    ): CodeBlock {
        if (cases.isEmpty() && elseCase == null) return CodeBlock.of("")

        val builder = CodeBlock.builder()
            .beginControlFlow("when ($subject)")

        cases.forEach { (pattern, result) ->
            builder.add("is $pattern -> ")
            builder.add(result)
            builder.add("\n")
        }

        if (elseCase != null) {
            builder.add("else -> ")
            builder.add(elseCase)
            builder.add("\n")
        }

        builder.endControlFlow()
        return builder.build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COLLECTION BUILDERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a mapOf() initialization with entries.
     *
     * Returns `emptyMap()` if entries list is empty.
     *
     * @param keyType Type of map keys (unused, for documentation)
     * @param valueType Type of map values (unused, for documentation)
     * @param entries List of pairs for map entries (key CodeBlock, value CodeBlock)
     * @return CodeBlock for the map initialization
     */
    @Suppress("UNUSED_PARAMETER")
    fun buildMapOf(
        keyType: TypeName,
        valueType: TypeName,
        entries: List<Pair<CodeBlock, CodeBlock>>
    ): CodeBlock {
        if (entries.isEmpty()) {
            return CodeBlock.of("emptyMap()")
        }

        val builder = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()

        entries.forEachIndexed { index, (key, value) ->
            builder.add(key)
            builder.add(" to ")
            builder.add(value)
            if (index < entries.size - 1) {
                builder.add(",")
            }
            builder.add("\n")
        }

        builder.unindent()
            .add(")")

        return builder.build()
    }

    /**
     * Builds a setOf() initialization with elements.
     *
     * Returns `emptySet()` if elements list is empty.
     *
     * @param elements List of element CodeBlocks
     * @return CodeBlock for the set initialization
     */
    fun buildSetOf(elements: List<CodeBlock>): CodeBlock {
        if (elements.isEmpty()) {
            return CodeBlock.of("emptySet()")
        }

        val builder = CodeBlock.builder()
            .add("setOf(\n")
            .indent()

        elements.forEachIndexed { index, element ->
            builder.add(element)
            if (index < elements.size - 1) {
                builder.add(",")
            }
            builder.add("\n")
        }

        builder.unindent()
            .add(")")

        return builder.build()
    }

    /**
     * Builds a listOf() initialization with elements.
     *
     * Returns `emptyList()` if elements list is empty.
     *
     * @param elements List of element CodeBlocks
     * @return CodeBlock for the list initialization
     */
    fun buildListOf(elements: List<CodeBlock>): CodeBlock {
        if (elements.isEmpty()) {
            return CodeBlock.of("emptyList()")
        }

        val builder = CodeBlock.builder()
            .add("listOf(\n")
            .indent()

        elements.forEachIndexed { index, element ->
            builder.add(element)
            if (index < elements.size - 1) {
                builder.add(",")
            }
            builder.add("\n")
        }

        builder.unindent()
            .add(")")

        return builder.build()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASS REFERENCES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a class reference expression (`ClassName::class`).
     *
     * @param className The class to reference
     * @return CodeBlock for `ClassName::class`
     */
    fun classReference(className: ClassName): CodeBlock {
        return CodeBlock.of("%T::class", className)
    }

    /**
     * Creates a typed class reference expression.
     *
     * @param className The class to reference
     * @param typeParam Optional type parameter for the KClass
     * @return CodeBlock for the class reference
     */
    fun typedClassReference(className: ClassName, typeParam: TypeName? = null): CodeBlock {
        return if (typeParam != null) {
            CodeBlock.of("%T::class as %T<%T>", className, KCLASS, typeParam)
        } else {
            CodeBlock.of("%T::class", className)
        }
    }

    private val KCLASS = ClassName("kotlin.reflect", "KClass")
}
