package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.move.ide.inspections.fixes.RemoveRedundantCastFix
import org.move.lang.core.psi.MvCastExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.endOffsetInParent
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyUnknown

class MvRedundantTypeCastInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitCastExpr(castExpr: MvCastExpr) {
            val msl = castExpr.isMsl()
            // TODO: different rules for msl, no need for any casts at all
            if (msl) return

//            val itemContext = castExpr.itemContext(msl)
//            val inferenceCtx = castExpr.inferenceContext(msl)

            val inference = castExpr.inference(msl) ?: return

//            val objectExpr = castExpr.expr
            val objectExprTy = inference.getExprType(castExpr.expr)
//            val objectExprTy = inferExprTy(objectExpr, inferenceCtx)
            if (objectExprTy is TyUnknown) return

            // cannot be redundant cast for untyped integer
            if (objectExprTy is TyInteger && (objectExprTy.kind == TyInteger.DEFAULT_KIND)) return

            val castTypeTy = castExpr.type.loweredType(msl)
//            val castTypeTy = itemContext.rawType(castExpr.type)
            if (castTypeTy is TyUnknown) return

            if (objectExprTy == castTypeTy) {
                holder.registerProblem(
                    castExpr,
                    "No cast needed",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    TextRange.create(castExpr.`as`.startOffsetInParent, castExpr.type.endOffsetInParent),
                    RemoveRedundantCastFix(castExpr)
                )
            }
        }
    }
}
