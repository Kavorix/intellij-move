package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

val MvUseAlias.parentUseSpeck: MvUseSpeck get() = this.parent as MvUseSpeck

abstract class MvUseAliasMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                               MvUseAlias
