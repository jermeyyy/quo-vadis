package com.jermey.quo.vadis.compiler.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar

class QuoVadisAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(NavigationRootUniquenessChecker.NAVIGATION_ROOT_LOOKUP)
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirDeclarationChecker<FirClass>> = setOf(
            RouteCollisionChecker,
            ArgumentParityChecker,
            ContainerRoleChecker,
            TransitionCompatibilityChecker,
            StructuralChecker,
            NavigationRootUniquenessChecker,
            TabItemChecker,
        )
        override val simpleFunctionCheckers: Set<FirDeclarationChecker<FirNamedFunction>> = setOf(
            ScreenValidationChecker,
        )
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirExpressionChecker<FirFunctionCall>> = setOf(
            NavigationConfigTypeChecker,
        )
    }
}
