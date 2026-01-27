package io.github.d0ublew.bapp.starter.ui

import burp.api.montoya.MontoyaApi
import io.github.d0ublew.bapp.starter.model.ChecklistTableModel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel

class ChecklistTableTab(
    private val api: MontoyaApi
) {

    private val log = api.logging()

    val panel: JPanel = JPanel(BorderLayout())

    private val tableModel = ChecklistTableModel(api)
    private val table = JTable(tableModel)

    init {
        log.logToOutput("Initializing ChecklistTableTab UI")

        setupTable()
        buildLayout()

        log.logToOutput("ChecklistTableTab UI ready")
    }

    private fun setupTable() {
        log.logToOutput("Setting up JTable")

        table.autoCreateRowSorter = true

        table.columnModel.getColumn(0).preferredWidth = 10
        // table.columnModel.getColumn(1).preferredWidth = 200
        // table.columnModel.getColumn(2).preferredWidth = 300
        // table.columnModel.getColumn(3).preferredWidth = 80
    }

    private fun buildLayout() {
        log.logToOutput("Building table layout")

        val scrollPane = JScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)
    }

    fun addFinding(
        id: String,
        name: String,
        host: String,
        path: String,
        time: String,
        result: String
    ) {
        log.logToOutput("Adding finding: $id | $name | $host | $path | $time | $result")
        tableModel.addFinding(id, name, host, path, time, result)
    }
}

