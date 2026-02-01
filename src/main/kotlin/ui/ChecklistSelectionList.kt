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
            val selected = event.selectedRequestResponses()

            for (select in selected) {
                logger.logToOutput("PASSED → ${checklist.id} - ${select.request().url()}")
                storage.add(checklist.id, select, "PASSED")
            }

            // handle req res from repeater
            val editor = event.messageEditorRequestResponse()

            if (editor.isPresent) {
                val current = editor.get().requestResponse()
                logger.logToOutput("PASSED → ${checklist.id} - ${current.request().url()}")
                storage.add(checklist.id, current, "PASSED")
            }
        }

        val issue = JMenuItem("ISSUE")
        issue.addActionListener {
            val selected = event.selectedRequestResponses()
            for (select in selected) {
                logger.logToOutput("ISSUE → ${checklist.id} - ${select.request().url()}")
                storage.add(checklist.id, select, "ISSUE")
            }

            // handle req res from repeater
            val editor = event.messageEditorRequestResponse()

            if (editor.isPresent) {
                val current = editor.get().requestResponse()
                logger.logToOutput("ISSUE → ${checklist.id} - ${current.request().url()}")
                storage.add(checklist.id, current, "ISSUE")
            }
        }

        selection.add(passed)
        selection.add(issue)

        return selection
    }


}