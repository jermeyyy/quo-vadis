package com.jermey.quo.vadis.compiler.fir

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object AnnotationExtractor {

    /**
     * Extract a String argument by name from the annotation.
     */
    fun FirAnnotation.stringArgument(name: String): String? {
        val argument = findArgument(name) ?: return null
        return (argument as? FirLiteralExpression)?.value as? String
    }

    /**
     * Extract a Boolean argument by name.
     */
    fun FirAnnotation.booleanArgument(name: String): Boolean? {
        val argument = findArgument(name) ?: return null
        return (argument as? FirLiteralExpression)?.value as? Boolean
    }

    /**
     * Extract an Int argument by name.
     */
    fun FirAnnotation.intArgument(name: String): Int? {
        val argument = findArgument(name) ?: return null
        val value = (argument as? FirLiteralExpression)?.value ?: return null
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Number -> value.toInt()
            else -> null
        }
    }

    /**
     * Extract a KClass argument resolved to ClassId.
     */
    fun FirAnnotation.classArgument(name: String): ClassId? {
        val argument = findArgument(name) ?: return null
        return when (argument) {
            is FirGetClassCall -> {
                val classArg = argument.arguments.firstOrNull() ?: return null
                classArg.resolvedType.classId
            }
            else -> null
        }
    }

    /**
     * Extract an enum argument as its entry name string.
     */
    fun FirAnnotation.enumArgument(name: String): String? {
        val argument = findArgument(name) ?: return null
        return when (argument) {
            is FirPropertyAccessExpression -> {
                argument.calleeReference.name.asString()
            }
            else -> null
        }
    }

    /**
     * Extract an array of KClass arguments resolved to ClassIds.
     */
    fun FirAnnotation.classArrayArgument(name: String): List<ClassId> {
        val argument = findArgument(name) ?: return emptyList()
        val elements = when (argument) {
            is FirVarargArgumentsExpression -> argument.arguments
            is FirCollectionLiteral -> argument.argumentList.arguments
            else -> return emptyList()
        }
        return elements.mapNotNull { expr ->
            when (expr) {
                is FirGetClassCall -> {
                    val classArg = expr.arguments.firstOrNull() ?: return@mapNotNull null
                    classArg.resolvedType.classId
                }
                else -> null
            }
        }
    }

    private fun FirAnnotation.findArgument(name: String): FirExpression? {
        val mapping = argumentMapping.mapping
        return mapping[Name.identifier(name)]
    }
}
