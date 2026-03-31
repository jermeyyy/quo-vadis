package com.jermey.quo.vadis.ksp.testutil

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Fake [KSName] for testing.
 */
class FakeKSName(private val name: String) : KSName {
    override fun asString(): String = name
    override fun getShortName(): String = name.substringAfterLast('.')
    override fun getQualifier(): String = name.substringBeforeLast('.', "")
    override fun toString(): String = name
}

/**
 * Fake [KSAnnotation] for testing.
 * Only [shortName] is functional — other members throw.
 */
class FakeKSAnnotation(
    annotationName: String,
    override val arguments: List<KSValueArgument> = emptyList(),
) : KSAnnotation {
    override val shortName: KSName = FakeKSName(annotationName)
    override val useSiteTarget = null
    override val annotationType: KSTypeReference get() = notImplemented("annotationType")
    override val defaultArguments: List<KSValueArgument> = emptyList()
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Fake [KSClassDeclaration] for testing.
 *
 * Provides all properties needed by [ValidationEngine] and [ContainerBlockGenerator]:
 * [simpleName], [qualifiedName], [packageName], [classKind], [modifiers], [annotations].
 *
 * Uses [NonExistLocation] and [Origin.KOTLIN] as defaults.
 * Methods not exercised by tests throw [UnsupportedOperationException].
 */
class FakeKSClassDeclaration(
    name: String,
    qualifiedName: String,
    packageName: String = qualifiedName.substringBeforeLast('.'),
    override val classKind: ClassKind = ClassKind.CLASS,
    override val modifiers: Set<Modifier> = emptySet(),
    annotationNames: List<String> = emptyList(),
    override val parentDeclaration: KSDeclaration? = null,
    override val containingFile: KSFile? = null,
    customAnnotations: List<KSAnnotation>? = null,
    primaryConstructorOverride: KSFunctionDeclaration? = null,
) : KSClassDeclaration {
    override val simpleName: KSName = FakeKSName(name)
    override val qualifiedName: KSName = FakeKSName(qualifiedName)
    override val packageName: KSName = FakeKSName(packageName)
    override val annotations: Sequence<KSAnnotation> =
        customAnnotations?.asSequence() ?: annotationNames.map { FakeKSAnnotation(it) }.asSequence()
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val docString: String? = null
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override val declarations: Sequence<KSDeclaration> = emptySequence()
    override val isCompanionObject: Boolean = false
    override val superTypes: Sequence<KSTypeReference> = emptySequence()
    override val primaryConstructor: KSFunctionDeclaration? = primaryConstructorOverride

    override val isActual: Boolean = false
    override val isExpect: Boolean = false
    override fun findActuals(): Sequence<KSDeclaration> = emptySequence()
    override fun findExpects(): Sequence<KSDeclaration> = emptySequence()
    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> = emptySequence()
    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> = emptySequence()
    override fun getAllProperties(): Sequence<KSPropertyDeclaration> = emptySequence()
    override fun asType(typeArguments: List<KSTypeArgument>): KSType = notImplemented("asType")
    override fun asStarProjectedType(): KSType = notImplemented("asStarProjectedType")
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")

    override fun toString(): String = "FakeKSClassDeclaration(${qualifiedName.asString()})"
}

/**
 * Fake [KSPLogger] that records all errors and warnings for assertion.
 */
class FakeKSPLogger : KSPLogger {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val infos = mutableListOf<String>()

    override fun logging(message: String, symbol: KSNode?) { /* no-op */ }
    override fun info(message: String, symbol: KSNode?) { infos += message }
    override fun warn(message: String, symbol: KSNode?) { warnings += message }
    override fun error(message: String, symbol: KSNode?) { errors += message }
    override fun exception(e: Throwable) { errors += "exception: ${e.message}" }
}

/**
 * Fake [KSValueArgument] for testing annotation arguments.
 */
class FakeKSValueArgument(
    argName: String?,
    override val value: Any?,
) : KSValueArgument {
    override val name: KSName? = argName?.let { FakeKSName(it) }
    override val isSpread: Boolean = false
    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Fake [KSValueParameter] for testing constructor parameters.
 */
class FakeKSValueParameter(
    paramName: String,
    paramType: KSType = fakeKSType(),
    override val hasDefault: Boolean = false,
    paramAnnotations: List<KSAnnotation> = emptyList(),
) : KSValueParameter {
    override val name: KSName = FakeKSName(paramName)
    override val type: KSTypeReference = FakeKSTypeReference(paramType)
    override val annotations: Sequence<KSAnnotation> = paramAnnotations.asSequence()
    override val isCrossInline: Boolean = false
    override val isNoInline: Boolean = false
    override val isVal: Boolean = false
    override val isVar: Boolean = false
    override val isVararg: Boolean = false
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Fake [KSTypeReference] for testing.
 * Resolves to the provided [KSType].
 */
class FakeKSTypeReference(
    private val resolvedType: KSType,
) : KSTypeReference {
    override val element: KSReferenceElement? = null
    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val modifiers: Set<Modifier> = emptySet()
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override fun resolve(): KSType = resolvedType
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Creates a fake [Resolver] using a dynamic proxy.
 * All method calls throw [UnsupportedOperationException] since the validation
 * methods under test do not use the resolver.
 */
fun fakeResolver(
    symbolsByAnnotation: Map<String, List<KSAnnotated>> = emptyMap(),
): Resolver {
    val handler = InvocationHandler { proxy, method, args ->
        when (method.name) {
            "getSymbolsWithAnnotation" -> {
                val annotationName = args[0] as String
                (symbolsByAnnotation[annotationName] ?: emptyList()).asSequence()
            }
            "toString" -> "FakeResolver"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.get(0)
            else -> throw UnsupportedOperationException("Resolver.${method.name} not implemented in fake")
        }
    }
    return Proxy.newProxyInstance(
        Resolver::class.java.classLoader,
        arrayOf(Resolver::class.java),
        handler
    ) as Resolver
}

/**
 * Fake [KSFile] for testing.
 * Provides a minimal implementation to satisfy containingFile checks.
 */
class FakeKSFile(
    private val path: String = "FakeFile.kt",
    private val pkg: String = "com.example",
) : KSFile {
    override val fileName: String = path.substringAfterLast('/')
    override val filePath: String = path
    override val packageName: KSName = FakeKSName(pkg)
    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val declarations: Sequence<KSDeclaration> = emptySequence()
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Fake [KSFunctionDeclaration] for testing.
 * Provides the minimum surface needed by [ScreenInfo.functionDeclaration].
 */
class FakeKSFunctionDeclaration(
    name: String,
    packageName: String = "com.example",
    parameterList: List<KSValueParameter> = emptyList(),
) : KSFunctionDeclaration {
    override val simpleName: KSName = FakeKSName(name)
    override val qualifiedName: KSName = FakeKSName("$packageName.$name")
    override val packageName: KSName = FakeKSName(packageName)
    override val containingFile: KSFile? = FakeKSFile(pkg = packageName)
    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val modifiers: Set<Modifier> = emptySet()
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val docString: String? = null
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override val parentDeclaration: KSDeclaration? = null
    override val isActual: Boolean = false
    override val isExpect: Boolean = false
    override val parameters: List<KSValueParameter> = parameterList
    override val returnType: KSTypeReference? = null
    override val extensionReceiver: KSTypeReference? = null
    override val functionKind: FunctionKind = FunctionKind.TOP_LEVEL
    override val isAbstract: Boolean = false
    override val declarations: Sequence<KSDeclaration> = emptySequence()
    override fun findActuals(): Sequence<KSDeclaration> = emptySequence()
    override fun findExpects(): Sequence<KSDeclaration> = emptySequence()
    override fun findOverridee(): KSDeclaration? = null
    override fun asMemberOf(containing: KSType): KSFunction = notImplemented("asMemberOf")
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = notImplemented("accept")
}

/**
 * Creates a fake [KSType] using a dynamic proxy.
 * All method calls throw [UnsupportedOperationException] since
 * ParamInfo.type is not invoked during validation.
 */
fun fakeKSType(qualifiedName: String? = null): KSType {
    val declaration = qualifiedName?.let {
        FakeKSClassDeclaration(
            name = it.substringAfterLast('.'),
            qualifiedName = it,
        )
    }
    val handler = InvocationHandler { proxy, method, args ->
        when (method.name) {
            "getDeclaration" -> declaration
                ?: throw UnsupportedOperationException(
                    "FakeKSType: declaration not available (no qualifiedName provided)"
                )
            "isMarkedNullable" -> false
            "makeNotNullable" -> proxy
            "toString" -> "FakeKSType" + (qualifiedName?.let { "($it)" } ?: "")
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.get(0)
            else -> throw UnsupportedOperationException("KSType.${method.name} not implemented in fake")
        }
    }
    return Proxy.newProxyInstance(
        KSType::class.java.classLoader,
        arrayOf(KSType::class.java),
        handler,
    ) as KSType
}

private fun notImplemented(name: String): Nothing =
    throw UnsupportedOperationException("FakeKsp: $name not implemented")
