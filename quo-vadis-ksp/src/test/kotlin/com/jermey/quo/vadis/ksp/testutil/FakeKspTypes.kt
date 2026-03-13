package com.jermey.quo.vadis.ksp.testutil

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
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
}

/**
 * Fake [KSAnnotation] for testing.
 * Only [shortName] is functional — other members throw.
 */
class FakeKSAnnotation(annotationName: String) : KSAnnotation {
    override val shortName: KSName = FakeKSName(annotationName)
    override val useSiteTarget = null
    override val annotationType: KSTypeReference get() = notImplemented("annotationType")
    override val arguments: List<KSValueArgument> = emptyList()
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
) : KSClassDeclaration {
    override val simpleName: KSName = FakeKSName(name)
    override val qualifiedName: KSName = FakeKSName(qualifiedName)
    override val packageName: KSName = FakeKSName(packageName)
    override val annotations: Sequence<KSAnnotation> =
        annotationNames.map { FakeKSAnnotation(it) }.asSequence()
    override val containingFile: KSFile? = null
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val docString: String? = null
    override val origin: Origin = Origin.KOTLIN
    override val location: Location = NonExistLocation
    override val parent: KSNode? = null
    override val declarations: Sequence<KSDeclaration> = emptySequence()
    override val isCompanionObject: Boolean = false
    override val superTypes: Sequence<KSTypeReference> = emptySequence()
    override val primaryConstructor: KSFunctionDeclaration? = null

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

    override fun toString(): String = "FakeKSClassDeclaration($qualifiedName)"
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
 * Creates a fake [Resolver] using a dynamic proxy.
 * All method calls throw [UnsupportedOperationException] since the validation
 * methods under test do not use the resolver.
 */
fun fakeResolver(): Resolver {
    val handler = InvocationHandler { proxy, method, args ->
        when (method.name) {
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

private fun notImplemented(name: String): Nothing =
    throw UnsupportedOperationException("FakeKsp: $name not implemented")
