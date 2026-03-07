package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.common.AdaptStrategy
import com.jermey.quo.vadis.compiler.common.ArgumentMetadata
import com.jermey.quo.vadis.compiler.common.ArgumentType
import com.jermey.quo.vadis.compiler.common.DestinationMetadata
import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.common.PaneBackBehavior
import com.jermey.quo.vadis.compiler.common.PaneContainerMetadata
import com.jermey.quo.vadis.compiler.common.PaneItemMetadata
import com.jermey.quo.vadis.compiler.common.PaneMetadata
import com.jermey.quo.vadis.compiler.common.PaneRole
import com.jermey.quo.vadis.compiler.common.ScreenMetadata
import com.jermey.quo.vadis.compiler.common.StackMetadata
import com.jermey.quo.vadis.compiler.common.TabItemMetadata
import com.jermey.quo.vadis.compiler.common.TabItemType
import com.jermey.quo.vadis.compiler.common.TabsContainerMetadata
import com.jermey.quo.vadis.compiler.common.TabsMetadata
import com.jermey.quo.vadis.compiler.common.TransitionMetadata
import com.jermey.quo.vadis.compiler.common.TransitionType
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.ARGUMENT_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.DESTINATION_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.PANE_CONTAINER_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.PANE_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.PANE_ITEM_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.SCREEN_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.STACK_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.TABS_CONTAINER_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.TABS_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.TAB_ITEM_FQN
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates.TRANSITION_FQN
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Scans IR module declarations for annotation-marked classes/functions
 * and builds [NavigationMetadata] from annotated elements.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class IrMetadataCollector(private val modulePrefix: String) {

    fun collect(moduleFragment: IrModuleFragment): NavigationMetadata {
        val stacks = mutableListOf<StackMetadata>()
        val tabs = mutableListOf<TabsMetadata>()
        val panes = mutableListOf<PaneMetadata>()
        val screens = mutableListOf<ScreenMetadata>()
        val tabsContainers = mutableListOf<TabsContainerMetadata>()
        val paneContainers = mutableListOf<PaneContainerMetadata>()
        val transitions = mutableListOf<TransitionMetadata>()
        val stackClassIds = mutableSetOf<ClassId>()
        val stackDestinationClassIdsByStack = mutableMapOf<ClassId, List<ClassId>>()

        // Pass 1: Collect all @Stack class IDs so we can determine TabItemType
        for (file in moduleFragment.files) {
            for (declaration in file.declarations) {
                collectStackClassIds(
                    declaration as? IrClass ?: continue,
                    stackClassIds,
                    stackDestinationClassIdsByStack,
                )
            }
        }

        // Pass 2: Build full metadata
        for (file in moduleFragment.files) {
            for (declaration in file.declarations) {
                when (declaration) {
                    is IrClass -> processClass(
                        declaration,
                        stacks,
                        tabs,
                        panes,
                        transitions,
                        stackClassIds,
                        stackDestinationClassIdsByStack,
                    )
                    is IrSimpleFunction -> processFunction(
                        declaration, screens, tabsContainers, paneContainers,
                    )
                    else -> Unit
                }
            }
        }

        return NavigationMetadata(
            modulePrefix = modulePrefix,
            stacks = stacks,
            tabs = tabs,
            panes = panes,
            screens = screens,
            tabsContainers = tabsContainers,
            paneContainers = paneContainers,
            transitions = transitions,
        )
    }

    // ── Pass 1 helper ────────────────────────────────────────────────────────

    private fun collectStackClassIds(
        irClass: IrClass,
        stackClassIds: MutableSet<ClassId>,
        stackDestinationClassIdsByStack: MutableMap<ClassId, List<ClassId>>,
    ) {
        if (irClass.hasAnnotation(STACK_FQN)) {
            irClass.classId?.let { classId ->
                stackClassIds.add(classId)
                stackDestinationClassIdsByStack[classId] = irClass.sealedSubclasses.mapNotNull { subclassSymbol ->
                    subclassSymbol.owner.classId
                }
            }
        }
        for (inner in irClass.declarations) {
            if (inner is IrClass) {
                collectStackClassIds(inner, stackClassIds, stackDestinationClassIdsByStack)
            }
        }
    }

    // ── Pass 2: class processing ────────────────────────────────────────────

    private fun processClass(
        irClass: IrClass,
        stacks: MutableList<StackMetadata>,
        tabs: MutableList<TabsMetadata>,
        panes: MutableList<PaneMetadata>,
        transitions: MutableList<TransitionMetadata>,
        stackClassIds: Set<ClassId>,
        stackDestinationClassIdsByStack: Map<ClassId, List<ClassId>>,
    ) {
        processStack(irClass, stacks, transitions)
        processTabs(irClass, tabs, stackClassIds, stackDestinationClassIdsByStack)
        processPane(irClass, panes, transitions)

        // Recurse into nested classes
        for (inner in irClass.declarations) {
            if (inner is IrClass) {
                processClass(
                    inner,
                    stacks,
                    tabs,
                    panes,
                    transitions,
                    stackClassIds,
                    stackDestinationClassIdsByStack,
                )
            }
        }
    }

    private fun processStack(
        irClass: IrClass,
        stacks: MutableList<StackMetadata>,
        transitions: MutableList<TransitionMetadata>,
    ) {
        val stackAnn = irClass.getAnnotation(STACK_FQN) ?: return
        val classId = irClass.classId ?: return
        val name = stackAnn.getStringArgument(0) ?: return
        val startDestArg = stackAnn.getClassArgument(1)

        val destinations = irClass.sealedSubclasses.mapNotNull { subclassSymbol ->
            val subclass = subclassSymbol.owner
            val subclassId = subclass.classId ?: return@mapNotNull null
            val destAnn = subclass.getAnnotation(DESTINATION_FQN)
            val route = destAnn?.getStringArgument(0)

            // Collect @Transition on destinations
            collectTransition(subclass, transitions)

            DestinationMetadata(
                classId = subclassId,
                route = route,
                arguments = extractArguments(subclass),
                transitionType = extractTransitionType(subclass),
            )
        }

        val startDestination = if (startDestArg == null ||
            startDestArg.asFqNameString() == "kotlin.Unit"
        ) {
            destinations.firstOrNull()?.classId ?: return
        } else {
            startDestArg
        }

        stacks.add(
            StackMetadata(
                name = name,
                startDestination = startDestination,
                sealedClassId = classId,
                destinations = destinations,
            ),
        )
    }

    private fun processTabs(
        irClass: IrClass,
        tabs: MutableList<TabsMetadata>,
        stackClassIds: Set<ClassId>,
        stackDestinationClassIdsByStack: Map<ClassId, List<ClassId>>,
    ) {
        val tabsAnn = irClass.getAnnotation(TABS_FQN) ?: return
        val classId = irClass.classId ?: return
        val name = tabsAnn.getStringArgument(0) ?: return
        val initialTab = tabsAnn.getClassArgument(1)?.takeUnlessUnit()
        val itemClasses = tabsAnn.getClassArrayArgument(2)

        val items = itemClasses.map { itemClassId ->
            TabItemMetadata(
                classId = itemClassId,
                type = if (itemClassId in stackClassIds) TabItemType.NESTED_STACK else TabItemType.FLAT_SCREEN,
            )
        }
        val allDestinationClassIds = items.flatMap { item ->
            when (item.type) {
                TabItemType.FLAT_SCREEN -> listOf(item.classId)
                TabItemType.NESTED_STACK -> stackDestinationClassIdsByStack[item.classId].orEmpty()
                    .ifEmpty { listOf(item.classId) }
            }
        }.distinct()

        tabs.add(
            TabsMetadata(
                name = name,
                classId = classId,
                initialTab = initialTab,
                items = items,
                allDestinationClassIds = allDestinationClassIds,
            ),
        )
    }

    private fun processPane(
        irClass: IrClass,
        panes: MutableList<PaneMetadata>,
        transitions: MutableList<TransitionMetadata>,
    ) {
        val paneAnn = irClass.getAnnotation(PANE_FQN) ?: return
        val classId = irClass.classId ?: return
        val name = paneAnn.getStringArgument(0) ?: return
        val backBehavior = paneAnn.getEnumArgument(1)?.toPaneBackBehavior()
            ?: PaneBackBehavior.POP_UNTIL_SCAFFOLD_VALUE_CHANGE

        // Collect ALL sealed subclass ClassIds for scope registration
        val allDestinationClassIds = irClass.sealedSubclasses.mapNotNull { subclassSymbol ->
            subclassSymbol.owner.classId
        }

        val items = irClass.sealedSubclasses.mapNotNull { subclassSymbol ->
            val subclass = subclassSymbol.owner
            val subclassId = subclass.classId ?: return@mapNotNull null
            val paneItemAnn = subclass.getAnnotation(PANE_ITEM_FQN) ?: return@mapNotNull null

            // Collect @Transition on pane destinations too
            collectTransition(subclass, transitions)

            val role = paneItemAnn.getEnumArgument(0)?.toPaneRole() ?: return@mapNotNull null
            val adaptStrategy = paneItemAnn.getEnumArgument(1)?.toAdaptStrategy()
                ?: AdaptStrategy.HIDE

            PaneItemMetadata(
                classId = subclassId,
                role = role,
                adaptStrategy = adaptStrategy,
            )
        }

        panes.add(
            PaneMetadata(
                name = name,
                classId = classId,
                backBehavior = backBehavior,
                items = items,
                allDestinationClassIds = allDestinationClassIds,
            ),
        )
    }

    // ── Pass 2: function processing ─────────────────────────────────────────

    private fun processFunction(
        function: IrSimpleFunction,
        screens: MutableList<ScreenMetadata>,
        tabsContainers: MutableList<TabsContainerMetadata>,
        paneContainers: MutableList<PaneContainerMetadata>,
    ) {
        processScreen(function, screens)
        processTabsContainer(function, tabsContainers)
        processPaneContainer(function, paneContainers)
    }

    private fun processScreen(
        function: IrSimpleFunction,
        screens: MutableList<ScreenMetadata>,
    ) {
        val screenAnn = function.getAnnotation(SCREEN_FQN) ?: return
        val destClassId = screenAnn.getClassArgument(0) ?: return
        val functionFqn = function.fqNameWhenAvailable ?: return

        val hasDestinationParam = function.parameters.any { param ->
            param.type.classFqName?.asString()?.let { it != "com.jermey.quo.vadis.core.navigation.navigator.Navigator" }
                ?: false
        }

        screens.add(
            ScreenMetadata(
                functionFqn = functionFqn,
                destinationClassId = destClassId,
                hasDestinationParam = hasDestinationParam,
            ),
        )
    }

    private fun processTabsContainer(
        function: IrSimpleFunction,
        tabsContainers: MutableList<TabsContainerMetadata>,
    ) {
        val ann = function.getAnnotation(TABS_CONTAINER_FQN) ?: return
        val tabClassId = ann.getClassArgument(0) ?: return
        val functionFqn = function.fqNameWhenAvailable ?: return
        tabsContainers.add(TabsContainerMetadata(functionFqn = functionFqn, tabClassId = tabClassId))
    }

    private fun processPaneContainer(
        function: IrSimpleFunction,
        paneContainers: MutableList<PaneContainerMetadata>,
    ) {
        val ann = function.getAnnotation(PANE_CONTAINER_FQN) ?: return
        val paneClassId = ann.getClassArgument(0) ?: return
        val functionFqn = function.fqNameWhenAvailable ?: return
        paneContainers.add(PaneContainerMetadata(functionFqn = functionFqn, paneClassId = paneClassId))
    }

    // ── Transition helpers ──────────────────────────────────────────────────

    private fun collectTransition(
        irClass: IrClass,
        transitions: MutableList<TransitionMetadata>,
    ) {
        val transAnn = irClass.getAnnotation(TRANSITION_FQN) ?: return
        val classId = irClass.classId ?: return
        val type = transAnn.getEnumArgument(0)?.toTransitionType() ?: return
        val customClass = transAnn.getClassArgument(1)?.takeUnlessUnit()
        transitions.add(TransitionMetadata(destinationClassId = classId, type = type, customClass = customClass))
    }

    private fun extractTransitionType(irClass: IrClass): TransitionType? {
        val transAnn = irClass.getAnnotation(TRANSITION_FQN) ?: return null
        return transAnn.getEnumArgument(0)?.toTransitionType()
    }

    // ── Argument extraction ─────────────────────────────────────────────────

    private fun extractArguments(irClass: IrClass): List<ArgumentMetadata> {
        val primaryConstructor = irClass.primaryConstructor ?: return emptyList()
        return primaryConstructor.parameters
            .filter { param -> param.annotations.any { it.isAnnotation(ARGUMENT_FQN) } }
            .mapNotNull { param ->
                val argAnn = param.annotations.firstOrNull { it.isAnnotation(ARGUMENT_FQN) }
                if (argAnn == null) {
                    // @Argument annotation expected but not found — skip this parameter
                    return@mapNotNull null
                }
                val key = argAnn.getStringArgument(0)?.takeIf { it.isNotEmpty() }
                    ?: param.name.asString()
                val optional = argAnn.getBooleanArgument(1) ?: false
                ArgumentMetadata(
                    name = param.name.asString(),
                    key = key,
                    type = param.type.classFqName.toArgumentType(),
                    optional = optional,
                )
            }
    }

    // ── IR annotation helpers ───────────────────────────────────────────────

    private fun IrClass.hasAnnotation(fqName: FqName): Boolean =
        annotations.any { it.isAnnotation(fqName) }

    private fun IrClass.getAnnotation(fqName: FqName): IrConstructorCall? =
        annotations.firstOrNull { it.isAnnotation(fqName) }

    private fun IrSimpleFunction.getAnnotation(fqName: FqName): IrConstructorCall? =
        annotations.firstOrNull { it.isAnnotation(fqName) }

    private fun IrConstructorCall.isAnnotation(fqName: FqName): Boolean {
        val annotationClass = type.classOrNull?.owner ?: return false
        return annotationClass.fqNameWhenAvailable == fqName
    }

    private fun IrConstructorCall.argumentAt(index: Int): IrExpression? {
        val param = symbol.owner.parameters.getOrNull(index) ?: return null
        return arguments[param]
    }

    private fun IrConstructorCall.getStringArgument(index: Int): String? {
        val arg = argumentAt(index) ?: return null
        return (arg as? IrConst)?.value as? String
    }

    private fun IrConstructorCall.getBooleanArgument(index: Int): Boolean? {
        val arg = argumentAt(index) ?: return null
        return (arg as? IrConst)?.value as? Boolean
    }

    private fun IrConstructorCall.getClassArgument(index: Int): ClassId? {
        val arg = argumentAt(index) ?: return null
        return when (arg) {
            is IrClassReference -> arg.classType.classOrNull?.owner?.classId
            else -> null
        }
    }

    private fun IrConstructorCall.getClassArrayArgument(index: Int): List<ClassId> {
        val arg = argumentAt(index) ?: return emptyList()
        val vararg = arg as? IrVararg ?: return emptyList()
        return vararg.elements.mapNotNull { element ->
            when (element) {
                is IrClassReference -> element.classType.classOrNull?.owner?.classId
                else -> null
            }
        }
    }

    private fun IrConstructorCall.getEnumArgument(index: Int): String? {
        val arg = argumentAt(index) ?: return null
        return (arg as? IrGetEnumValue)?.symbol?.owner?.name?.asString()
    }

    // ── Enum mapping ────────────────────────────────────────────────────────

    private fun ClassId.takeUnlessUnit(): ClassId? =
        takeIf { it.asFqNameString() != "kotlin.Unit" }

    private fun String.toTransitionType(): TransitionType? = when (this) {
        "SlideHorizontal" -> TransitionType.SLIDE_HORIZONTAL
        "SlideVertical" -> TransitionType.SLIDE_VERTICAL
        "Fade" -> TransitionType.FADE
        "None" -> TransitionType.NONE
        "Custom" -> TransitionType.CUSTOM
        else -> null
    }

    private fun String.toPaneBackBehavior(): PaneBackBehavior? = when (this) {
        "PopUntilScaffoldValueChange" -> PaneBackBehavior.POP_UNTIL_SCAFFOLD_VALUE_CHANGE
        "PopUntilContentChange" -> PaneBackBehavior.POP_UNTIL_CONTENT_CHANGE
        "PopLatest" -> PaneBackBehavior.POP_LATEST
        else -> null
    }

    private fun String.toPaneRole(): PaneRole? = when (this) {
        "PRIMARY" -> PaneRole.PRIMARY
        "SECONDARY" -> PaneRole.SECONDARY
        "EXTRA" -> PaneRole.EXTRA
        else -> null
    }

    private fun String.toAdaptStrategy(): AdaptStrategy? = when (this) {
        "HIDE" -> AdaptStrategy.HIDE
        "OVERLAY" -> AdaptStrategy.OVERLAY
        "COLLAPSE", "SHOW_AS_DIALOG" -> AdaptStrategy.SHOW_AS_DIALOG
        "REFLOW" -> AdaptStrategy.OVERLAY
        else -> null
    }

    private fun FqName?.toArgumentType(): ArgumentType = when (this?.asString()) {
        "kotlin.String" -> ArgumentType.STRING
        "kotlin.Int" -> ArgumentType.INT
        "kotlin.Long" -> ArgumentType.LONG
        "kotlin.Float" -> ArgumentType.FLOAT
        "kotlin.Double" -> ArgumentType.DOUBLE
        "kotlin.Boolean" -> ArgumentType.BOOLEAN
        else -> ArgumentType.ENUM
    }
}
