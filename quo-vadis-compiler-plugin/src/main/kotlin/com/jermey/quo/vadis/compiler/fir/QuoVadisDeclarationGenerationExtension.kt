package com.jermey.quo.vadis.compiler.fir

import com.jermey.quo.vadis.compiler.QuoVadisGeneratedKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * FIR Declaration Generation Extension for Quo Vadis compiler plugin.
 *
 * Generates two synthetic object declarations in the `com.jermey.quo.vadis.generated` package:
 * - `{modulePrefix}NavigationConfig` implementing `NavigationConfig`
 * - `{modulePrefix}DeepLinkHandler` implementing `DeepLinkRegistry`
 *
 * These are FIR-level stubs visible to the IDE and type checker.
 * Bodies are provided in Phase 3 (IR generation).
 */
class QuoVadisDeclarationGenerationExtension(
    session: FirSession,
    modulePrefix: String,
) : FirDeclarationGenerationExtension(session) {

    companion object {
        val GENERATED_PACKAGE = FqName("com.jermey.quo.vadis.generated")
    }

    // region Synthetic class IDs

    private val configClassId = ClassId(
        GENERATED_PACKAGE,
        Name.identifier("${modulePrefix}NavigationConfig"),
    )
    private val deepLinkHandlerClassId = ClassId(
        GENERATED_PACKAGE,
        Name.identifier("${modulePrefix}DeepLinkHandler"),
    )
    private val screenRegistryClassId = ClassId(
        GENERATED_PACKAGE,
        Name.identifier("${modulePrefix}ScreenRegistryImpl"),
    )

    // endregion

    // region Supertype class IDs

    private val navigationConfigId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.config"),
        Name.identifier("NavigationConfig"),
    )
    private val generatedNavigationConfigId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.config"),
        Name.identifier("GeneratedNavigationConfig"),
    )
    private val deepLinkRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("DeepLinkRegistry"),
    )

    // endregion

    // region Registry type class IDs

    private val screenRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("ScreenRegistry"),
    )
    private val scopeRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("ScopeRegistry"),
    )
    private val transitionRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("TransitionRegistry"),
    )
    private val containerRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("ContainerRegistry"),
    )
    private val paneRoleRegistryId = ClassId(
        FqName("com.jermey.quo.vadis.core.registry"),
        Name.identifier("PaneRoleRegistry"),
    )

    // endregion

    // region Compose scope type class IDs

    private val sharedTransitionScopeId = ClassId(
        FqName("androidx.compose.animation"),
        Name.identifier("SharedTransitionScope"),
    )
    private val animatedVisibilityScopeId = ClassId(
        FqName("androidx.compose.animation"),
        Name.identifier("AnimatedVisibilityScope"),
    )
    private val composableAnnotationId = ClassId(
        FqName("androidx.compose.runtime"),
        Name.identifier("Composable"),
    )

    // endregion

    // region Other domain type class IDs

    private val navDestinationId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.destination"),
        Name.identifier("NavDestination"),
    )
    private val deepLinkTypeId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.destination"),
        Name.identifier("DeepLink"),
    )
    private val navigatorTypeId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.navigator"),
        Name.identifier("Navigator"),
    )
    private val navNodeId = ClassId(
        FqName("com.jermey.quo.vadis.core.navigation.node"),
        Name.identifier("NavNode"),
    )

    // endregion

    // region Kotlin standard type class IDs

    private val kClassId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
    private val setClassId = ClassId(FqName("kotlin.collections"), Name.identifier("Set"))
    private val listClassId = ClassId(FqName("kotlin.collections"), Name.identifier("List"))
    private val mapClassId = ClassId(FqName("kotlin.collections"), Name.identifier("Map"))
    private val function1ClassId = ClassId(FqName("kotlin"), Name.identifier("Function1"))
    private val function2ClassId = ClassId(FqName("kotlin"), Name.identifier("Function2"))

    // endregion

    // region Reusable cone types (lazy-init since they depend on session)

    private val stringType: ConeKotlinType by lazy {
        session.builtinTypes.stringType.coneType
    }
    private val nullableStringType: ConeKotlinType by lazy {
        ClassId(FqName("kotlin"), Name.identifier("String"))
            .createConeType(session, emptyArray(), nullable = true)
    }
    private val booleanType: ConeKotlinType by lazy {
        session.builtinTypes.booleanType.coneType
    }
    private val unitType: ConeKotlinType by lazy {
        session.builtinTypes.unitType.coneType
    }
    private val navDestinationType: ConeKotlinType by lazy {
        navDestinationId.createConeType(session)
    }
    private val nullableNavDestinationType: ConeKotlinType by lazy {
        navDestinationId.createConeType(session, emptyArray(), nullable = true)
    }
    private val navigationConfigType: ConeKotlinType by lazy {
        navigationConfigId.createConeType(session)
    }
    private val nullableNavNodeType: ConeKotlinType by lazy {
        navNodeId.createConeType(session, emptyArray(), nullable = true)
    }
    private val navigatorType: ConeKotlinType by lazy {
        navigatorTypeId.createConeType(session)
    }
    private val deepLinkConeType: ConeKotlinType by lazy {
        deepLinkTypeId.createConeType(session)
    }

    /** `KClass<out NavDestination>` */
    private val kClassOutNavDestinationType: ConeKotlinType by lazy {
        kClassId.createConeType(
            session,
            arrayOf(ConeKotlinTypeProjectionOut(navDestinationType)),
        )
    }

    /** `Set<KClass<out NavDestination>>` */
    private val setKClassOutNavDestinationType: ConeKotlinType by lazy {
        setClassId.createConeType(session, arrayOf(kClassOutNavDestinationType))
    }

    /** `Map<String, String>` */
    private val mapStringStringType: ConeKotlinType by lazy {
        mapClassId.createConeType(session, arrayOf(stringType, stringType))
    }

    /** `(Map<String, String>) -> NavDestination` */
    private val factoryFunctionType: ConeKotlinType by lazy {
        function1ClassId.createConeType(
            session,
            arrayOf(mapStringStringType, navDestinationType),
        )
    }

    /** `(Navigator, Map<String, String>) -> Unit` */
    private val actionFunctionType: ConeKotlinType by lazy {
        function2ClassId.createConeType(
            session,
            arrayOf(navigatorType, mapStringStringType, unitType),
        )
    }

    private val nullableSharedTransitionScopeType: ConeKotlinType by lazy {
        sharedTransitionScopeId.createConeType(session, emptyArray(), nullable = true)
    }
    private val nullableAnimatedVisibilityScopeType: ConeKotlinType by lazy {
        animatedVisibilityScopeId.createConeType(session, emptyArray(), nullable = true)
    }

    /** `List<String>` */
    private val listStringType: ConeKotlinType by lazy {
        listClassId.createConeType(session, arrayOf(stringType))
    }

    // endregion

    // region Callable name sets

    private val configPropertyNames = setOf(
        "screenRegistry", "scopeRegistry", "transitionRegistry",
        "containerRegistry", "deepLinkRegistry", "paneRoleRegistry", "roots",
    ).mapTo(mutableSetOf()) { Name.identifier(it) }

    private val configFunctionNames = setOf(
        "buildNavNode", "plus",
    ).mapTo(mutableSetOf()) { Name.identifier(it) }

    private val configCallableNames: Set<Name> =
        configPropertyNames + configFunctionNames + SpecialNames.INIT

    private val deepLinkFunctionNames = setOf(
        "resolve", "register", "registerAction", "handle",
        "createUri", "canHandle", "getRegisteredPatterns", "handleDeepLink",
    ).mapTo(mutableSetOf()) { Name.identifier(it) }

    private val deepLinkCallableNames: Set<Name> =
        deepLinkFunctionNames + SpecialNames.INIT

    private val screenRegistryCallableNames: Set<Name> = setOf(
        Name.identifier("Content"),
        Name.identifier("hasContent"),
        SpecialNames.INIT,
    )

    // endregion

    // region FirDeclarationGenerationExtension overrides

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == GENERATED_PACKAGE
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelClassIds(): Set<ClassId> {
        // In KMP, each source set (commonMain, nativeMain, appleMain, iosMain, iosArm64Main)
        // gets its own FIR session with its own extension instance. Without this guard,
        // each session would declare the same class IDs, causing duplicate declarations
        // when the compiler merges them for the final target compilation.
        // Only generate in the root source set (empty dependsOnDependencies) — downstream
        // source sets see these declarations through the dependency chain.
        if (session.moduleData.dependsOnDependencies.isNotEmpty()) {
            return emptySet()
        }
        return setOf(configClassId, deepLinkHandlerClassId, screenRegistryClassId)
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(
        classId: ClassId,
    ): FirClassLikeSymbol<*>? {
        return when (classId) {
            configClassId -> createTopLevelClass(
                classId, QuoVadisGeneratedKey, ClassKind.OBJECT,
            ) {
                superType(generatedNavigationConfigId.createConeType(session))
            }.symbol

            deepLinkHandlerClassId -> createTopLevelClass(
                classId, QuoVadisGeneratedKey, ClassKind.OBJECT,
            ) {
                superType(deepLinkRegistryId.createConeType(session))
            }.symbol

            screenRegistryClassId -> createTopLevelClass(
                classId, QuoVadisGeneratedKey, ClassKind.CLASS,
            ) {
                superType(screenRegistryId.createConeType(session))
            }.symbol

            else -> null
        }
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Member,
    ): Set<Name> {
        return when (classSymbol.classId) {
            configClassId -> configCallableNames
            deepLinkHandlerClassId -> deepLinkCallableNames
            screenRegistryClassId -> screenRegistryCallableNames
            else -> emptySet()
        }
    }

    override fun generateConstructors(
        context: DeclarationGenerationContext.Member,
    ): List<FirConstructorSymbol> {
        val owner = context.owner
        if (owner.classId != configClassId && owner.classId != deepLinkHandlerClassId && owner.classId != screenRegistryClassId) {
            return emptyList()
        }
        return listOf(
            createConstructor(
                owner,
                QuoVadisGeneratedKey,
                isPrimary = true,
                generateDelegatedNoArgConstructorCall = true,
            ).symbol,
        )
    }

    override fun generateProperties(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?,
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.classId != configClassId) return emptyList()

        val name = callableId.callableName
        val (type, shouldOverride) = resolveConfigPropertyType(name) ?: return emptyList()

        return listOf(
            createMemberProperty(owner, QuoVadisGeneratedKey, name, type) {
                if (shouldOverride) {
                    status { isOverride = true }
                }
            }.symbol,
        )
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?,
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val name = callableId.callableName
        return when (callableId.classId) {
            configClassId -> generateConfigFunctions(owner, name)
            deepLinkHandlerClassId -> generateDeepLinkFunctions(owner, name)
            screenRegistryClassId -> generateScreenRegistryFunctions(owner, name)
            else -> emptyList()
        }
    }

    // endregion

    // region NavigationConfig helpers

    private fun resolveConfigPropertyType(name: Name): Pair<ConeKotlinType, Boolean>? {
        return when (name.asString()) {
            "screenRegistry" -> screenRegistryId.createConeType(session) to true
            "scopeRegistry" -> scopeRegistryId.createConeType(session) to true
            "transitionRegistry" -> transitionRegistryId.createConeType(session) to true
            "containerRegistry" -> containerRegistryId.createConeType(session) to true
            "deepLinkRegistry" -> deepLinkRegistryId.createConeType(session) to true
            "paneRoleRegistry" -> paneRoleRegistryId.createConeType(session) to true
            "roots" -> setKClassOutNavDestinationType to false
            else -> null
        }
    }

    private fun generateConfigFunctions(
        owner: FirClassSymbol<*>,
        name: Name,
    ): List<FirNamedFunctionSymbol> {
        return when (name.asString()) {
            "buildNavNode" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, nullableNavNodeType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("destinationClass"), kClassOutNavDestinationType)
                    valueParameter(Name.identifier("key"), nullableStringType)
                    valueParameter(Name.identifier("parentKey"), nullableStringType)
                }.symbol,
            )

            "plus" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, navigationConfigType) {
                    status {
                        isOverride = true
                        isOperator = true
                    }
                    valueParameter(Name.identifier("other"), navigationConfigType)
                }.symbol,
            )

            else -> emptyList()
        }
    }

    // endregion

    // region DeepLinkHandler helpers

    @Suppress("LongMethod")
    private fun generateDeepLinkFunctions(
        owner: FirClassSymbol<*>,
        name: Name,
    ): List<FirNamedFunctionSymbol> {
        return when (name.asString()) {
            "resolve" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, nullableNavDestinationType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("uri"), stringType)
                }.symbol,
                createMemberFunction(owner, QuoVadisGeneratedKey, name, nullableNavDestinationType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("deepLink"), deepLinkConeType)
                }.symbol,
            )

            "register" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, unitType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("pattern"), stringType)
                    valueParameter(Name.identifier("factory"), factoryFunctionType)
                }.symbol,
            )

            "registerAction" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, unitType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("pattern"), stringType)
                    valueParameter(Name.identifier("action"), actionFunctionType)
                }.symbol,
            )

            "handle" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, booleanType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("uri"), stringType)
                    valueParameter(Name.identifier("navigator"), navigatorType)
                }.symbol,
            )

            "createUri" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, nullableStringType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("destination"), navDestinationType)
                    valueParameter(Name.identifier("scheme"), stringType)
                }.symbol,
            )

            "canHandle" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, booleanType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("uri"), stringType)
                }.symbol,
            )

            "getRegisteredPatterns" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, listStringType) {
                    status { isOverride = true }
                }.symbol,
            )

            "handleDeepLink" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, nullableNavDestinationType) {
                    valueParameter(Name.identifier("uri"), stringType)
                }.symbol,
            )

            else -> emptyList()
        }
    }

    // endregion

    // region ScreenRegistryImpl helpers

    private fun generateScreenRegistryFunctions(
        owner: FirClassSymbol<*>,
        name: Name,
    ): List<FirNamedFunctionSymbol> {
        return when (name.asString()) {
            "Content" -> {
                val contentFunction = createMemberFunction(owner, QuoVadisGeneratedKey, name, unitType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("destination"), navDestinationType)
                    valueParameter(Name.identifier("sharedTransitionScope"), nullableSharedTransitionScopeType)
                    valueParameter(Name.identifier("animatedVisibilityScope"), nullableAnimatedVisibilityScopeType)
                }
                // Add explicit @Composable annotation so the Compose compiler processes this override
                val composableConeType = composableAnnotationId.createConeType(session)
                contentFunction.replaceAnnotations(
                    contentFunction.annotations + buildAnnotation {
                        annotationTypeRef = buildResolvedTypeRef {
                            coneType = composableConeType
                        }
                        argumentMapping = FirEmptyAnnotationArgumentMapping
                    },
                )
                listOf(contentFunction.symbol)
            }
            "hasContent" -> listOf(
                createMemberFunction(owner, QuoVadisGeneratedKey, name, booleanType) {
                    status { isOverride = true }
                    valueParameter(Name.identifier("destination"), navDestinationType)
                }.symbol,
            )
            else -> emptyList()
        }
    }

    // endregion
}
