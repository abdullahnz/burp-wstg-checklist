package io.github.d0ublew.bapp.starter.ui

import burp.api.montoya.logging.Logging
import burp.api.montoya.organizer.Organizer
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import io.github.d0ublew.bapp.starter.Storage
import io.github.d0ublew.bapp.starter.dataclass.CHECKLIST
import io.github.d0ublew.bapp.starter.dataclass.Checklist
import java.awt.Component
import javax.swing.JMenu
import javax.swing.JMenuItem


class ChecklistSelectionList(
    private val storage: Storage,
    private val logger: Logging
) : ContextMenuItemsProvider  {
    private final val checklists = CHECKLIST

    override fun provideMenuItems(event: ContextMenuEvent): List<Component> {
        val menus = arrayListOf<Component>()
        val grouped = checklists.groupBy { it.title }

        for ((title, items) in grouped) {
            val parent = JMenu(title)

            for (checklist in items) {
                val item = addChecklistResultSelection(checklist, event)
                parent.add(item)
            }
            menus.add(parent)
        }

        return menus
    }

    private fun addChecklistResultSelection(
        checklist: Checklist,
        event: ContextMenuEvent,
    ): JMenu {
        val selection = JMenu("${checklist.id} - ${checklist.name}")

        val passed = JMenuItem("PASSED")
        passed.addActionListener {
            logger.logToOutput("PASSED → ${checklist.id}")

            val selected = event.selectedRequestResponses()
            for (selected in selected) {
                storage.add(checklist.id, selected, "PASSED")
            }

        }

        val issue = JMenuItem("ISSUE")
        issue.addActionListener {
            logger.logToOutput("ISSUE → ${checklist.id}")

            val selected = event.selectedRequestResponses()
            for (selected in selected) {
                storage.add(checklist.id, selected, "ISSUE")
            }
        }

        selection.add(passed)
        selection.add(issue)

        return selection
    }


}