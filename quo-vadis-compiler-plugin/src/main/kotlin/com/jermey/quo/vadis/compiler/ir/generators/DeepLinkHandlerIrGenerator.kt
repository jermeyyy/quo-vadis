@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.ArgumentMetadata
import com.jermey.quo.vadis.compiler.common.ArgumentType
import com.jermey.quo.vadis.compiler.common.DestinationMetadata
import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions

class DeepLinkHandlerIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {
    /** All destinations with non-null routes from all stacks. */
    private val routableDestinations: List<DestinationMetadata> by lazy {
        metadata.stacks.flatMap { it.destinations }.filter { !it.route.isNullOrBlank() }
    }

    fun generate(irClass: IrClass) {
        for (function in irClass.functions) {
            generateFunctionBody(function, irClass)
        }
    }

    @Suppress("CyclomaticComplexity")
    private fun generateFunctionBody(function: IrSimpleFunction, irClass: IrClass) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        when (function.name.asString()) {
            "resolve" -> {
                val paramName = function.parameters.getOrNull(1)?.name?.asString()
                if (paramName == "uri") {
                    generateResolveUri(function, builder, irClass)
                } else {
                    generateResolveDeepLink(function, builder)
                }
            }
            "register", "registerAction" -> {
                // No-op: generated handler uses compile-time routes
                function.body = builder.irBlockBody { }
            }
            "handle" -> generateHandle(function, builder, irClass)
            "createUri" -> generateCreateUri(function, builder)
            "canHandle" -> generateCanHandle(function, builder, irClass)
            "getRegisteredPatterns" -> generateGetRegisteredPatterns(function, builder)
            "handleDeepLink" -> generateHandleDeepLink(function, builder, irClass)
        }
    }

    // region getRegisteredPatterns

    /**
     * Generates: `return listOf("route1", "route2", ...)`
     */
    private fun generateGetRegisteredPatterns(function: IrSimpleFunction, builder: DeclarationIrBuilder) {
        val allRoutes = routableDestinations.mapNotNull { it.route }

        if (allRoutes.isEmpty()) {
            val emptyListFn = symbolResolver.resolveFunctions("kotlin.collections", "emptyList").firstOrNull()
                ?: error("Expected 'emptyList' function in kotlin.collections. Ensure quo-vadis-core version is compatible with compiler plugin.")
            function.body = builder.irBlockBody {
                +irReturn(irCall(emptyListFn))
            }
            return
        }

        val listOfVararg = symbolResolver.listOfFunctions.firstOrNull {
            it.owner.parameters.size == 1 && it.owner.parameters[0].varargElementType != null
        } ?: error("Expected 'listOf(vararg)' function in kotlin.collections. Ensure quo-vadis-core version is compatible with compiler plugin.")
        val stringType = pluginContext.irBuiltIns.stringType

        function.body = builder.irBlockBody {
            val strings = allRoutes.map { irString(it) }
            val listCall = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(stringType)).also {
                it.arguments[listOfVararg.owner.parameters[0]] = irVararg(stringType, strings)
            }
            +irReturn(listCall)
        }
    }

    // endregion

    // region resolve(uri: String)

    /**
     * Generates: `val dl = DeepLink.parse(uri); return resolve(dl)`
     */
    private fun generateResolveUri(
        function: IrSimpleFunction,
        builder: DeclarationIrBuilder,
        irClass: IrClass,
    ) {
        val deepLinkClass = symbolResolver.deepLinkClass.owner
        val companion = deepLinkClass.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.isCompanion }
        val parseFn = companion?.declarations?.filterIsInstance<IrSimpleFunction>()
            ?.firstOrNull { it.name.asString() == "parse" }

        val resolveDeepLinkFn = irClass.functions.firstOrNull {
            it.name.asString() == "resolve" &&
                it.parameters.any { p -> p.name.asString() == "deepLink" }
        }

        if (parseFn == null || companion == null || resolveDeepLinkFn == null) {
            function.body = builder.irBlockBody { +irReturn(irNull()) }
            return
        }

        function.body = builder.irBlockBody {
            val uriParam = function.parameters[1]

            // val dl = DeepLink.parse(uri)
            val dlVar = irTemporary(
                irCall(parseFn.symbol).apply {
                    arguments[parseFn.parameters[0]] = irGetObject(companion.symbol)
                    arguments[parseFn.parameters[1]] = irGet(uriParam)
                },
                nameHint = "dl",
            )

            // return resolve(dl)
            +irReturn(
                irCall(resolveDeepLinkFn.symbol).apply {
                    arguments[resolveDeepLinkFn.parameters[0]] = irGet(function.parameters[0])
                    arguments[resolveDeepLinkFn.parameters[1]] = irGet(dlVar)
                },
            )
        }
    }

    // endregion

    // region resolve(deepLink: DeepLink)

    /**
     * Generates sequential matching of `deepLink.path` against route patterns.
     *
     * Parameterless destinations use exact string equality.
     * Parameterized destinations use Regex-based matching with typed parameter extraction.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun generateResolveDeepLink(function: IrSimpleFunction, builder: DeclarationIrBuilder) {
        val deepLinkClass = symbolResolver.deepLinkClass.owner
        val pathProp = deepLinkClass.declarations.filterIsInstance<IrProperty>()
            .firstOrNull { it.name.asString() == "path" }
        val pathGetter = pathProp?.getter

        if (pathGetter == null) {
            function.body = builder.irBlockBody { +irReturn(irNull()) }
            return
        }

        function.body = builder.irBlockBody {
            val deepLinkParam = function.parameters[1]

            // val path = deepLink.path
            val pathVar = irTemporary(
                irCall(pathGetter.symbol).apply {
                    arguments[pathGetter.parameters[0]] = irGet(deepLinkParam)
                },
                nameHint = "path",
            )

            for ((index, dest) in routableDestinations.withIndex()) {
                val route = dest.route ?: continue
                val destClassSymbol = symbolResolver.resolveClass(dest.classId)
                val destClass = destClassSymbol.owner

                if (dest.arguments.isEmpty()) {
                    // Exact match: if (path == "route") return Dest
                    val resultExpr = if (destClass.kind == ClassKind.OBJECT) {
                        irGetObject(destClassSymbol)
                    } else {
                        val ctor = destClass.constructors.firstOrNull { it.isPrimary } ?: continue
                        irCallConstructor(ctor.symbol, emptyList())
                    }

                    val ifExact = IrWhenImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.unitType,
                        IrStatementOrigin.IF,
                    )
                    ifExact.branches += IrBranchImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        condition = irEquals(irGet(pathVar), irString(route)),
                        result = irReturn(resultExpr),
                    )
                    ifExact.branches += IrElseBranchImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        condition = irTrue(),
                        result = irGetObject(pluginContext.irBuiltIns.unitClass),
                    )
                    +ifExact
                } else {
                    // Parameterized match using regex
                    emitParameterizedMatchBlock(dest, route, pathVar, index)
                }
            }

            // No match: return null
            +irReturn(constNull())
        }
    }

    /**
     * Emits a regex-based matching block for a parameterized destination.
     *
     * Generates IR equivalent to:
     * ```
     * val regex = Regex("^escaped/([^/]+)/path$")
     * val match = regex.matchEntire(path)
     * if (match != null) {
     *     return DestClass(
     *         param1 = match.groupValues[1].toInt(),
     *         param2 = match.groupValues[2],
     *     )
     * }
     * ```
     */
    @Suppress("LongMethod")
    private fun IrBlockBodyBuilder.emitParameterizedMatchBlock(
        dest: DestinationMetadata,
        route: String,
        pathVar: IrVariable,
        index: Int,
    ) {
        val segments = parseRouteTemplate(route)
        val routeParams = segments.filterIsInstance<RouteSegment.Param>()
        val regexPattern = buildRegexPattern(segments)

        // val regex = Regex(pattern)
        val regexCtor = symbolResolver.regexClass.owner.constructors
            .firstOrNull { it.parameters.size == 1 && it.parameters[0].type == pluginContext.irBuiltIns.stringType }
            ?: error("Expected single-parameter constructor in Regex. Ensure quo-vadis-core version is compatible with compiler plugin.")
        val regexVar = irTemporary(
            irCallConstructor(regexCtor.symbol, emptyList()).apply {
                arguments[regexCtor.parameters[0]] = irString(regexPattern)
            },
            nameHint = "regex_$index",
        )

        // val match = regex.matchEntire(path)
        val matchEntireFn = symbolResolver.regexClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "matchEntire" }
            ?: error("Expected function 'matchEntire' in Regex. Ensure quo-vadis-core version is compatible with compiler plugin.")
        val matchVar = irTemporary(
            irCall(matchEntireFn.symbol).apply {
                arguments[matchEntireFn.parameters[0]] = irGet(regexVar)
                arguments[matchEntireFn.parameters[1]] = irGet(pathVar)
            },
            nameHint = "match_$index",
        )

        // Build destination constructor call with typed arguments
        val destClassSymbol = symbolResolver.resolveClass(dest.classId)
        val destClass = destClassSymbol.owner
        val ctor = destClass.constructors.firstOrNull { it.isPrimary } ?: return

        // Resolve MatchResult.groupValues
        val matchResultClass = symbolResolver.resolveClass("kotlin.text", "MatchResult").owner
        val groupValuesProp = matchResultClass.declarations.filterIsInstance<IrProperty>()
            .firstOrNull { it.name.asString() == "groupValues" }
        val groupValuesGetter = groupValuesProp?.getter ?: return

        // Resolve List.get(index)
        val listGetFn = pluginContext.irBuiltIns.listClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "get" }
            ?: error("Expected function 'get' in List. Ensure quo-vadis-core version is compatible with compiler plugin.")

        // Prepare argument expressions (must be built in builder scope, not inside apply {})
        val argExprs = dest.arguments.mapNotNull { arg ->
            val argKey = arg.key.ifEmpty { arg.name }
            val paramGroupIndex = routeParams.indexOfFirst { it.name == argKey }
            if (paramGroupIndex < 0) return@mapNotNull null
            val groupIndex = paramGroupIndex + 1

            val ctorParam = ctor.parameters.firstOrNull {
                it.name.asString() == arg.name
            } ?: return@mapNotNull null

            // match.groupValues[groupIndex]
            val groupValuesExpr = irCall(groupValuesGetter.symbol).apply {
                arguments[groupValuesGetter.parameters[0]] = irGet(matchVar)
            }
            val extractedString = irCall(listGetFn.symbol).apply {
                arguments[listGetFn.parameters[0]] = groupValuesExpr
                arguments[listGetFn.parameters[1]] = irIntConst(groupIndex)
            }

            // Apply type conversion
            ctorParam to irConvertArgument(extractedString, arg, destClass)
        }

        val ctorCall = irCallConstructor(ctor.symbol, emptyList()).apply {
            for ((ctorParam, value) in argExprs) {
                arguments[ctorParam] = value
            }
        }

        // if (match == null) -> skip; else -> return Dest(args...)
        val ifMatch = IrWhenImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            pluginContext.irBuiltIns.unitType,
            IrStatementOrigin.IF,
        )
        ifMatch.branches += IrBranchImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            condition = irEquals(irGet(matchVar), constNull()),
            result = irGetObject(pluginContext.irBuiltIns.unitClass),
        )
        ifMatch.branches += IrElseBranchImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            condition = irTrue(),
            result = irReturn(ctorCall),
        )
        +ifMatch
    }

    // endregion

    // region canHandle

    /**
     * Generates: `return resolve(uri) != null`
     */
    private fun generateCanHandle(
        function: IrSimpleFunction,
        builder: DeclarationIrBuilder,
        irClass: IrClass,
    ) {
        val resolveUriFn = findResolveUriFn(irClass)
        if (resolveUriFn == null) {
            function.body = builder.irBlockBody { +irReturn(irBoolean(false)) }
            return
        }

        function.body = builder.irBlockBody {
            val uriParam = function.parameters[1]

            val resolveResult = irTemporary(
                irCall(resolveUriFn.symbol).apply {
                    arguments[resolveUriFn.parameters[0]] = irGet(function.parameters[0])
                    arguments[resolveUriFn.parameters[1]] = irGet(uriParam)
                },
                nameHint = "result",
            )

            // if (result == null) false else true
            val nullCheck = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.booleanType,
                IrStatementOrigin.IF,
            )
            nullCheck.branches += IrBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irEquals(irGet(resolveResult), constNull()),
                result = irBoolean(false),
            )
            nullCheck.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irBoolean(true),
            )

            +irReturn(nullCheck)
        }
    }

    // endregion

    // region handle

    /**
     * Generates:
     * ```
     * val dest = resolve(uri)
     * if (dest == null) return false
     * navigator.navigate(dest)
     * return true
     * ```
     */
    private fun generateHandle(
        function: IrSimpleFunction,
        builder: DeclarationIrBuilder,
        irClass: IrClass,
    ) {
        val resolveUriFn = findResolveUriFn(irClass)
        val navigateFn = findNavigateFn()

        if (resolveUriFn == null || navigateFn == null) {
            function.body = builder.irBlockBody { +irReturn(irBoolean(false)) }
            return
        }

        function.body = builder.irBlockBody {
            val uriParam = function.parameters[1]
            val navigatorParam = function.parameters[2]

            // val dest = resolve(uri)
            val destVar = irTemporary(
                irCall(resolveUriFn.symbol).apply {
                    arguments[resolveUriFn.parameters[0]] = irGet(function.parameters[0])
                    arguments[resolveUriFn.parameters[1]] = irGet(uriParam)
                },
                nameHint = "dest",
            )

            // if (dest == null) return false
            val earlyReturn = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                IrStatementOrigin.IF,
            )
            earlyReturn.branches += IrBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irEquals(irGet(destVar), constNull()),
                result = irReturn(irBoolean(false)),
            )
            earlyReturn.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irGetObject(pluginContext.irBuiltIns.unitClass),
            )
            +earlyReturn

            // navigator.navigate(dest)
            +irCall(navigateFn.symbol).apply {
                arguments[navigateFn.parameters[0]] = irGet(navigatorParam)
                arguments[navigateFn.parameters[1]] = irGet(destVar)
                // Provide null for the optional transition parameter
                if (navigateFn.parameters.size > 2) {
                    arguments[navigateFn.parameters[2]] = irNull()
                }
            }

            +irReturn(irBoolean(true))
        }
    }

    // endregion

    // region createUri

    /**
     * Generates a when expression with instanceof checks, returning
     * `"$scheme://route"` for each destination.
     */
    private fun generateCreateUri(function: IrSimpleFunction, builder: DeclarationIrBuilder) {
        if (routableDestinations.isEmpty()) {
            function.body = builder.irBlockBody { +irReturn(irNull()) }
            return
        }

        val nullableStringType = pluginContext.irBuiltIns.stringType.makeNullable()

        function.body = builder.irBlockBody {
            val destParam = function.parameters[1]
            val schemeParam = function.parameters[2]

            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                nullableStringType,
                IrStatementOrigin.WHEN,
            )

            for (dest in routableDestinations) {
                val route = dest.route ?: continue
                val destClassSymbol = symbolResolver.resolveClass(dest.classId)
                val destType = destClassSymbol.defaultType

                val condition = IrTypeOperatorCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.booleanType,
                    IrTypeOperator.INSTANCEOF,
                    destType,
                    irGet(destParam),
                )

                val uriExpr = buildUriExpression(dest, route, schemeParam, destParam, destType)

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = condition,
                    result = uriExpr,
                )
            }

            // else -> null
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = constNull(),
            )

            +irReturn(whenExpr)
        }
    }

    /**
     * Builds a string concatenation expression for a URI.
     *
     * For parameterless routes: `"$scheme://route"`
     * For parameterized routes: `"$scheme://prefix${destination.param}suffix..."`
     */
    @Suppress("LongParameterList")
    private fun IrBlockBodyBuilder.buildUriExpression(
        dest: DestinationMetadata,
        route: String,
        schemeParam: IrValueParameter,
        destParam: IrValueParameter,
        destType: IrType,
    ): IrExpression {
        val stringType = pluginContext.irBuiltIns.stringType

        if (dest.arguments.isEmpty()) {
            return IrStringConcatenationImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                stringType,
                listOf(irGet(schemeParam), irString("://"), irString(route)),
            )
        }

        // Parse route template and interpolate destination properties
        val parts = mutableListOf<IrExpression>()
        parts.add(irGet(schemeParam))
        parts.add(irString("://"))

        val segments = parseRouteTemplate(route)
        for (segment in segments) {
            when (segment) {
                is RouteSegment.Literal -> parts.add(irString(segment.value))
                is RouteSegment.Param -> {
                    val destClass = symbolResolver.resolveClass(dest.classId).owner
                    val prop = destClass.declarations.filterIsInstance<IrProperty>()
                        .firstOrNull { it.name.asString() == segment.name }
                    val getter = prop?.getter

                    if (getter != null) {
                        val cast = IrTypeOperatorCallImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            destType,
                            IrTypeOperator.IMPLICIT_CAST,
                            destType,
                            irGet(destParam),
                        )
                        parts.add(
                            irCall(getter.symbol).apply {
                                arguments[getter.parameters[0]] = cast
                            },
                        )
                    } else {
                        error(
                            "Route parameter '${segment.name}' not found as a property " +
                                "on destination class ${dest.classId.asFqNameString()}",
                        )
                    }
                }
            }
        }

        return IrStringConcatenationImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            stringType,
            parts,
        )
    }

    // endregion

    // region handleDeepLink

    /**
     * Generates: `return resolve(uri)`
     */
    private fun generateHandleDeepLink(
        function: IrSimpleFunction,
        builder: DeclarationIrBuilder,
        irClass: IrClass,
    ) {
        val resolveUriFn = findResolveUriFn(irClass)

        if (resolveUriFn == null) {
            function.body = builder.irBlockBody { +irReturn(irNull()) }
            return
        }

        function.body = builder.irBlockBody {
            val uriParam = function.parameters[1]
            +irReturn(
                irCall(resolveUriFn.symbol).apply {
                    arguments[resolveUriFn.parameters[0]] = irGet(function.parameters[0])
                    arguments[resolveUriFn.parameters[1]] = irGet(uriParam)
                },
            )
        }
    }

    // endregion

    // region Helpers

    private fun findResolveUriFn(irClass: IrClass): IrSimpleFunction? {
        return irClass.functions.firstOrNull {
            it.name.asString() == "resolve" &&
                it.parameters.any { p -> p.name.asString() == "uri" }
        }
    }

    private fun findNavigateFn(): IrSimpleFunction? {
        return symbolResolver.navigatorClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull {
                it.name.asString() == "navigate" &&
                    it.parameters.any { p -> p.name.asString() == "destination" }
            }
    }

    private fun constNull(): IrExpression = IrConstImpl.constNull(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        pluginContext.irBuiltIns.nothingNType,
    )

    private fun irIntConst(value: Int): IrExpression = IrConstImpl.int(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        pluginContext.irBuiltIns.intType,
        value,
    )

    /**
     * Builds a regex pattern from parsed route segments.
     * Literal segments are escaped, parameter segments become `([^/]+)` capture groups.
     */
    private fun buildRegexPattern(segments: List<RouteSegment>): String {
        val sb = StringBuilder("^")
        for (segment in segments) {
            when (segment) {
                is RouteSegment.Literal -> sb.append(java.util.regex.Pattern.quote(segment.value))
                is RouteSegment.Param -> sb.append("([^/]+)")
            }
        }
        sb.append('$')
        return sb.toString()
    }

    /**
     * Generates type conversion IR for a regex-extracted string argument.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun IrBlockBodyBuilder.irConvertArgument(
        stringValue: IrExpression,
        arg: ArgumentMetadata,
        destClass: IrClass,
    ): IrExpression = when (arg.type) {
        ArgumentType.STRING -> stringValue
        ArgumentType.INT -> irCallStringConversion(stringValue, "toInt", "toIntOrNull", arg.optional)
        ArgumentType.LONG -> irCallStringConversion(stringValue, "toLong", "toLongOrNull", arg.optional)
        ArgumentType.FLOAT -> irCallStringConversion(stringValue, "toFloat", "toFloatOrNull", arg.optional)
        ArgumentType.DOUBLE -> irCallStringConversion(stringValue, "toDouble", "toDoubleOrNull", arg.optional)
        ArgumentType.BOOLEAN -> irCallStringConversion(stringValue, "toBoolean", "toBooleanStrictOrNull", arg.optional)
        ArgumentType.ENUM -> irCallEnumValueOf(stringValue, arg, destClass)
    }

    /**
     * Generates a call to a String conversion extension function (e.g. `toInt()`, `toIntOrNull()`).
     */
    private fun IrBlockBodyBuilder.irCallStringConversion(
        stringValue: IrExpression,
        fnName: String,
        nullableFnName: String,
        optional: Boolean,
    ): IrExpression {
        val name = if (optional) nullableFnName else fnName
        val convFn = resolveStringExtensionFn(name)
        return irCall(convFn).apply {
            arguments[convFn.owner.parameters[0]] = stringValue
        }
    }

    /**
     * Resolves a String extension function by name, searching `kotlin.text` then `kotlin` packages.
     */
    private fun resolveStringExtensionFn(name: String): IrSimpleFunctionSymbol {
        for (pkg in listOf("kotlin.text", "kotlin")) {
            val fns = symbolResolver.resolveFunctions(pkg, name)
            val fn = fns.firstOrNull { it.owner.parameters.size == 1 }
            if (fn != null) return fn
        }
        error("Quo Vadis compiler plugin: cannot resolve String.$name() conversion function")
    }

    /**
     * Generates an enum `valueOf` call for converting a string to an enum value.
     */
    private fun IrBlockBodyBuilder.irCallEnumValueOf(
        stringValue: IrExpression,
        arg: ArgumentMetadata,
        destClass: IrClass,
    ): IrExpression {
        val ctor = destClass.constructors.firstOrNull { it.isPrimary }
        val ctorParam = ctor?.parameters?.firstOrNull { it.name.asString() == arg.name }
        val enumClassSymbol = ctorParam?.type?.classOrNull
        val enumClass = enumClassSymbol?.owner

        if (enumClass != null) {
            val valueOfFn = enumClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .firstOrNull { it.name.asString() == "valueOf" }
            if (valueOfFn != null) {
                return irCall(valueOfFn.symbol).apply {
                    arguments[valueOfFn.parameters.last()] = stringValue
                }
            }
        }

        // Fallback: return string directly (will likely cause a type mismatch at compile)
        return stringValue
    }

    private sealed class RouteSegment {
        data class Literal(val value: String) : RouteSegment()
        data class Param(val name: String) : RouteSegment()
    }

    /**
     * Parses a route template like `"user/{userId}/post/{postId}"` into
     * a list of [RouteSegment.Literal] and [RouteSegment.Param] segments.
     */
    private fun parseRouteTemplate(route: String): List<RouteSegment> {
        val segments = mutableListOf<RouteSegment>()
        var i = 0
        val sb = StringBuilder()
        while (i < route.length) {
            if (route[i] == '{') {
                if (sb.isNotEmpty()) {
                    segments.add(RouteSegment.Literal(sb.toString()))
                    sb.clear()
                }
                val end = route.indexOf('}', i)
                if (end > i) {
                    segments.add(RouteSegment.Param(route.substring(i + 1, end)))
                    i = end + 1
                } else {
                    sb.append(route[i])
                    i++
                }
            } else {
                sb.append(route[i])
                i++
            }
        }
        if (sb.isNotEmpty()) {
            segments.add(RouteSegment.Literal(sb.toString()))
        }
        return segments
    }

    // endregion
}
