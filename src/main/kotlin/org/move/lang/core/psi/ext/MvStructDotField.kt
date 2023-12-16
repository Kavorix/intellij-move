package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructDotField
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.MvStructDotFieldReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveLocalItem

class MvStructDotFieldReferenceImpl(
    element: MvStructDotFieldReferenceElement
) : MvPolyVariantReferenceCached<MvStructDotFieldReferenceElement>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        return resolveLocalItem(element, setOf(Namespace.DOT_FIELD))
    }
}

abstract class MvStructDotFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructDotField {
    override fun getReference(): MvPolyVariantReference {
        return MvStructDotFieldReferenceImpl(this)
    }
}
