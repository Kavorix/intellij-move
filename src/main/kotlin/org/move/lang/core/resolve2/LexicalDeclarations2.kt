package org.move.lang.core.resolve2

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.LetStmtScope.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.util.forEachLeafSpeck

fun processItemsInScope(
    scope: MvElement,
    cameFrom: MvElement,
    namespaces: Set<Namespace>,
    contextScopeInfo: ContextScopeInfo,
    processor: RsResolveProcessor,
): Boolean {
    for (namespace in namespaces) {
        val stop = when (namespace) {

            Namespace.CONST -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.processAll(
//                            contextScopeInfo,
                            module.consts(),
                        )
                    }
                    else -> false
                }
//                if (!found) {
//                    if (scope is MvItemsOwner) {
//                        if (processor.processAll(scope.allUseItems())) return true
//                    }
//                }
                found
            }

            Namespace.NAME -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.processAll(
//                            contextScopeInfo,
                            module.structs(),
                            module.consts(),
                        )
                    }
                    is MvModuleSpecBlock -> processor.processAll(scope.schemaList)
                    is MvScript -> processor.processAll(scope.consts())
                    is MvFunctionLike -> processor.processAll(scope.allParamsAsBindings)
                    is MvLambdaExpr -> processor.processAll(scope.bindingPatList)
                    is MvForExpr -> {
                        val iterConditionBindingPat = scope.forIterCondition?.bindingPat
                        if (iterConditionBindingPat != null) {
                            processor.process(iterConditionBindingPat)
                        } else {
                            false
                        }
                    }
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> {
                                processor.processAll(
//                                    contextScopeInfo,
                                    item.valueParamsAsBindings,
                                    item.specResultParameters.map { it.bindingPat },
                                )
                            }
                            is MvStruct -> processor.processAll(item.fields)
                            else -> false
                        }
                    }
                    is MvSchema -> processor.processAll(scope.fieldBindings)
                    is MvQuantBindingsOwner -> processor.processAll(scope.bindings)
                    is MvCodeBlock,
                    is MvSpecCodeBlock -> {
                        val visibleLetStmts = when (scope) {
                            is MvCodeBlock -> {
                                scope.letStmts
                                    // drops all let-statements after the current position
                                    .filter { it.cameBefore(cameFrom) }
                                    // drops let-statement that is ancestors of ref (on the same statement, at most one)
                                    .filter {
                                        cameFrom != it
                                                && !PsiTreeUtil.isAncestor(cameFrom, it, true)
                                    }
                            }
                            is MvSpecCodeBlock -> {
                                when (contextScopeInfo.letStmtScope) {
                                    EXPR_STMT -> scope.allLetStmts
                                    LET_STMT, LET_POST_STMT -> {
                                        val letDecls =
                                            if (contextScopeInfo.letStmtScope == LET_POST_STMT) {
                                                scope.allLetStmts
                                            } else {
                                                scope.letStmts(false)
                                            }
                                        letDecls
                                            // drops all let-statements after the current position
                                            .filter { it.cameBefore(cameFrom) }
                                            // drops let-statement that is ancestors of ref (on the same statement, at most one)
                                            .filter {
                                                cameFrom != it
                                                        && !PsiTreeUtil.isAncestor(cameFrom, it, true)
                                            }
                                    }
                                    NONE -> emptyList()
                                }
                            }
                            else -> error("unreachable")
                        }
                        // shadowing support (look at latest first)
                        val namedElements = visibleLetStmts
                            .asReversed()
                            .flatMap { it.pat?.bindings.orEmpty() }

                        // skip shadowed (already visited) elements
                        val visited = mutableSetOf<String>()
                        val shadowingProcessor = processor.wrapWithFilter {
                            val isVisited = it.name in visited
                            if (!isVisited) {
                                visited += it.name
                            }
                            !isVisited
                        }
//                        val processorWithShadowing = MatchingProcessor { entry ->
//                            ((entry.name !in visited)
//                                    && processor.process(entry).also { visited += entry.name })
//                        }
                        var found = shadowingProcessor.processAll(namedElements)
                        if (!found && scope is MvSpecCodeBlock) {
                            // if inside SpecCodeBlock, process also with builtin spec consts and global variables
                            found = shadowingProcessor.processAll(
//                                contextScopeInfo,
                                scope.builtinSpecConsts(),
                                scope.globalVariables()
                            )
                        }
                        found
                    }
                    else -> false
                }
//                if (!found) {
//                    if (scope is MvItemsOwner) {
//                        if (scope.processUseSpeckElements(namespaces, processor)) return true
//                    }
//                }
                found
            }
            Namespace.FUNCTION -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        val specFunctions = if (contextScopeInfo.isMslScope) {
                            listOf(module.specFunctions(), module.builtinSpecFunctions()).flatten()
                        } else {
                            emptyList()
                        }
                        val specInlineFunctions = if (contextScopeInfo.isMslScope) {
                            module.moduleItemSpecs().flatMap { it.specInlineFunctions() }
                        } else {
                            emptyList()
                        }
                        processor.processAll(
//                            contextScopeInfo,
                            module.allNonTestFunctions(),
                            module.builtinFunctions(),
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvModuleSpecBlock -> {
                        val specFunctions = scope.specFunctionList
                        val specInlineFunctions = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                        processor.processAll(
//                            contextScopeInfo,
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvFunctionLike -> processor.processAll(scope.lambdaParamsAsBindings)
                    is MvLambdaExpr -> processor.processAll(scope.bindingPatList)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> processor.processAll(item.lambdaParamsAsBindings)
                            else -> false
                        }
                    }
                    is MvSpecCodeBlock -> {
                        val inlineFunctions = scope.specInlineFunctions().asReversed()
                        return processor.processAll(inlineFunctions)
                    }
                    else -> false
                }
//                if (!found) {
//
//                }
                found
            }

            Namespace.TYPE -> {
                if (scope is MvTypeParametersOwner) {
                    if (processor.processAll(scope.typeParameters)) return true
                }
                val found = when (scope) {
                    is MvItemSpec -> {
                        val funcItem = scope.funcItem
                        if (funcItem != null) {
                            processor.processAll(funcItem.typeParameters)
                        } else {
                            false
                        }
                    }
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.processAll(
//                            contextScopeInfo,
                            scope.allUseItems(),
                            module.structs()
                        )
                    }
                    is MvApplySchemaStmt -> {
                        val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                        val patternTypeParams =
                            toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                        processor.processAll(patternTypeParams)
                    }

                    else -> false
                }
//                if (!found) {
//                    if (scope is MvItemsOwner) {
//                        if (scope.processUseSpeckElements(namespaces, processor)) return true
//                    }
//                }
                found
            }

            Namespace.SCHEMA -> when (scope) {
                is MvModuleBlock -> processor.processAll(
//                    contextScopeInfo,
                    scope.allUseItems(),
                    scope.schemaList
                )
                is MvModuleSpecBlock -> processor.processAll(
//                    contextScopeInfo,
                    scope.allUseItems(),
                    scope.schemaList,
                    scope.specFunctionList
                )
                else -> false
            }

            Namespace.MODULE -> when (scope) {
                is MvItemsOwner ->
                    processor.processAll(scope.moduleUseItems())
                else -> false
            }
        }
        if (!stop && scope is MvItemsOwner) {
            if (scope.processUseSpeckElements(namespaces, processor)) return true
        }
        if (stop) return true
    }

    return false
}

private fun MvItemsOwner.processUseSpeckElements(ns: Set<Namespace>, processor: RsResolveProcessor): Boolean {
    var stop = false
    for (useStmt in this.useStmtList) {
        useStmt.forEachLeafSpeck { path, alias ->
            val name = if (alias != null) {
                alias.name ?: return@forEachLeafSpeck false
            } else {
                var n = path.referenceName ?: return@forEachLeafSpeck false
                // 0x1::m::Self -> 0x1::m
                if (n == "Self") {
                    n = path.qualifier?.referenceName ?: return@forEachLeafSpeck false
                }
                n
            }
            val namedElement = path.reference?.resolve()
            if (namedElement == null) {
                if (alias != null) {
                    // aliased element cannot be resolved, but alias itself is valid, resolve to it
                    if (processor.process(name, alias)) return@forEachLeafSpeck true
                }
                // todo: should it be resolved to import anyway?
                return@forEachLeafSpeck false
            }

            val element = alias ?: namedElement
            val namespace = namedElement.namespace

            if (namespace in ns && processor.process(name, element)) {
                stop = true
                return@forEachLeafSpeck true
            }
            false
        }
        if (stop) return true
    }
    return stop
}
