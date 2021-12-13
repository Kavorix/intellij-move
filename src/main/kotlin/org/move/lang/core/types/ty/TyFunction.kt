package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvFunctionSignature
import org.move.lang.core.psi.typeParameters

class TyFunction(
    val item: MvFunctionSignature,
    val typeVars: List<TyInfer.TyVar>,
    val paramTypes: List<Ty>,
    val retType: Ty
) : Ty {
    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)
}