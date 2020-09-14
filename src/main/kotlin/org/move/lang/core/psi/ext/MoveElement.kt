package org.move.lang.core.psi.ext

import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.MoveElementTypes.SPEC_BLOCK
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.resolve.walkUpThroughScopes

fun MoveElement.isDescendantOf(elementType: IElementType): Boolean {
    var descendant = false
    walkUpThroughScopes(this, { it is PsiFile }) { _, scope ->
        if (scope.elementType == elementType) descendant = true;
        false
    }
    return descendant
}

//fun MoveElement.isDescendantOf(elementTypes: TokenSet): Boolean {
//    var descendant = false
//    walkUpThroughScopes(this, { it is PsiFile }) { _, scope ->
//        if (scope.elementType in elementTypes) descendant = true;
//        false
//    }
//    return descendant
//}

fun MoveElement.isInsideSpecBlock(): Boolean = isDescendantOf(SPEC_BLOCK)