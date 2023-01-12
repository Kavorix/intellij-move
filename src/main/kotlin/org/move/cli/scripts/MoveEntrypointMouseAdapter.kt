package org.move.cli.scripts

import org.move.cli.AptosCommandLine
import org.move.cli.toolwindow.MoveProjectsTree
import org.move.cli.toolwindow.MoveProjectsTreeStructure
import org.move.lang.core.psi.ext.toAddress
import org.move.lang.core.psi.module
import org.move.lang.core.psi.typeParameters
import org.move.lang.moveProject
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class MoveEntrypointMouseAdapter : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return

        val tree = e.source as? MoveProjectsTree ?: return
        val node = tree.selectionModel.selectionPath
            ?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val scriptFunction =
            (node.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Entrypoint)?.function
                ?: return

        val moveProject = scriptFunction.moveProject ?: return
        val profiles = moveProject.currentPackage.aptosConfigYaml?.profiles.orEmpty().toList()
        val paramsDialog = TransactionParametersDialog(scriptFunction, profiles)
        val isOk = paramsDialog.showAndGet()
        if (!isOk) return

        val address = scriptFunction.module?.addressRef?.toAddress(moveProject)?.value ?: return
        val module = scriptFunction.module?.name ?: return
        val name = scriptFunction.name ?: return

        val functionTypeParamNames = scriptFunction.typeParameters.mapNotNull { it.name }
        val sortedTypeParams = paramsDialog.typeParams
            .entries
            .sortedBy { (name, _) ->
                functionTypeParamNames.indexOfFirst { it == name }
            }.flatMap { (_, value) -> listOf("--type-args", value) }

        val functionParamNames = scriptFunction.parameterBindings().mapNotNull { it.name }
        val sortedParams = paramsDialog.params.entries
            .sortedBy { (name, _) ->
                functionParamNames.indexOfFirst { it == name }
            }.flatMap { (_, value) -> listOf("--args", value) }

        val profile = paramsDialog.selectedProfile
        val profileArgs =
            if (profile != null) listOf("--profile", profile) else listOf()
        val commandArgs = listOf(
            profileArgs,
            listOf("--function-id", "${address}::${module}::${name}"),
            sortedTypeParams,
            sortedParams,
        ).flatten()
        AptosCommandLine("move run", moveProject.contentRootPath, commandArgs)
            .run(moveProject, paramsDialog.configurationName)
    }
}