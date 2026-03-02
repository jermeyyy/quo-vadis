package com.jermey.quo.vadis.compiler.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction

class QuoVadisAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirDeclarationChecker<FirClass>> = setOf(
            RouteCollisionChecker,
            ArgumentParityChecker,
            ContainerRoleChecker,
            TransitionCompatibilityChecker,
            StructuralChecker,
        )
        override val simpleFunctionCheckers: Set<FirDeclarationChecker<FirNamedFunction>> = setOf(
            ScreenValidationChecker,
        )
    }
}
