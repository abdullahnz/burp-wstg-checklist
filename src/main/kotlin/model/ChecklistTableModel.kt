package io.github.d0ublew.bapp.starter.model

import burp.api.montoya.MontoyaApi
import javax.swing.table.DefaultTableModel

class ChecklistTableModel(
    api: MontoyaApi
) : DefaultTableModel() {

    private val log = api.logging()

    init {
        log.logToOutput("Initializing ChecklistTableModel")

        addColumn("#")
        addColumn("ID")
        addColumn("Name")
        addColumn("Host")
        addColumn("Path")
        addColumn("Time")
        addColumn("Result")

        log.logToOutput("Table columns created")

        
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }

    fun addFinding(
        id: String,
        name: String,
        host: String,
        path: String,
        time: String,
        result: String
    ) {
        log.logToOutput("Adding row to table model (ID=$id)")
        addRow(arrayOf(this.rowCount + 1, id, name, host, path, time, result))
    }
}
