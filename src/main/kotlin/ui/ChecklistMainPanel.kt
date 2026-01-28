package io.github.d0ublew.bapp.starter.ui

import burp.api.montoya.logging.Logging
import burp.api.montoya.organizer.Organizer
import com.google.gson.GsonBuilder
import io.github.d0ublew.bapp.starter.Storage
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExport
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExportRequest
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExportResponse
import io.github.d0ublew.bapp.starter.dataclass.ChecklistResult
import io.github.d0ublew.bapp.starter.encode
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumnModel
import kotlin.math.log

class ChecklistMainPanel(
    private val organizer: Organizer,
    private val storage: Storage,
    private val logging: Logging
) {

    private val rootPanel: JPanel = JPanel(BorderLayout())
    private var checklistResults = mutableListOf<ChecklistResult>()

    private lateinit var requestTable: JTable
    private lateinit var requestTextArea: JTextArea
    private lateinit var responseTextArea: JTextArea

    init {
        logging.raiseInfoEvent("Initializing ChecklistMainPanel")
        checklistResults = storage.get()

        buildUI()

        rootPanel.addAncestorListener(object : AncestorListener {
            override fun ancestorAdded(event: AncestorEvent) {
                reloadTableFromStorage()
            }

            override fun ancestorRemoved(event: AncestorEvent) {}
            override fun ancestorMoved(event: AncestorEvent) {}
        })

    }

    fun getPanel(): JPanel = rootPanel

    private fun buildUI() {
        val mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        mainSplitPane.resizeWeight = 0.4

        mainSplitPane.topComponent = buildTopPanel()
        mainSplitPane.bottomComponent = buildBottomPanel()

        rootPanel.add(mainSplitPane, BorderLayout.CENTER)
    }

    private fun buildTopPanel(): JPanel {
        val topPanel = JPanel(BorderLayout())

        topPanel.add(buildHeader(), BorderLayout.NORTH)
        topPanel.add(buildTable(), BorderLayout.CENTER)

        return topPanel
    }

    private fun buildHeader(): JPanel {
        val headerPanel = JPanel(BorderLayout())
        headerPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val exportButton = JButton("Export JSON")
        exportButton.addActionListener {
            exportJsonHandler()
        }

        headerPanel.add(exportButton, BorderLayout.EAST)
        return headerPanel
    }

    private fun buildTable(): JScrollPane {
        val columnNames = arrayOf(
            "#", "ID", "Title", "Name", "Host", "Path", "Status"
        )
        val tableModel = DefaultTableModel(columnNames, 0)

        fillTable(tableModel)

        requestTable = JTable(tableModel)

        val headerRenderer = requestTable.tableHeader.defaultRenderer
        if (headerRenderer is JLabel) {
            headerRenderer.horizontalAlignment = SwingConstants.LEFT
        }

        requestTable.autoCreateRowSorter = true

        installTablePopupMenu()
        bindCtrlOAction()

        resizeColumnsToFitContent(requestTable)

        requestTable.selectionModel.addListSelectionListener(
            tableSelectionListener(checklistResults as ArrayList<ChecklistResult>)
        )


        return JScrollPane(requestTable)
    }

    private fun buildBottomPanel(): JSplitPane {
        val bottomSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        bottomSplitPane.resizeWeight = 0.5

        requestTextArea = createTextArea("Request")
        responseTextArea = createTextArea("Response")

        bottomSplitPane.leftComponent = JScrollPane(requestTextArea)
            .apply { border = BorderFactory.createTitledBorder("Request") }

        bottomSplitPane.rightComponent = JScrollPane(responseTextArea)
            .apply { border = BorderFactory.createTitledBorder("Response") }

        return bottomSplitPane
    }

    private fun createTextArea(defaultText: String): JTextArea =
        JTextArea("$defaultText details will appear here...").apply {
            font = Font("Monospaced", Font.PLAIN, 11)
            isEditable = false
            lineWrap = true
        }

    private fun getSelectedRowIndex(): Int {
        val selectedRow = requestTable.selectedRow
        if (selectedRow == -1) {
            logging.raiseInfoEvent("Ctrl+O pressed but no row selected")
            return -1
        }

        val modelRow = requestTable.convertRowIndexToModel(selectedRow)
        val index =  requestTable.model.getValueAt(modelRow, 0) as Int

        return index - 1
    }

    private fun tableSelectionListener(checklistResults: ArrayList<ChecklistResult>): ListSelectionListener =
        ListSelectionListener {
            if (it.valueIsAdjusting) return@ListSelectionListener

            val index = getSelectedRowIndex()
            if (index == -1) return@ListSelectionListener

            val result = checklistResults.getOrNull(index) ?: return@ListSelectionListener

            val req = result.httpRequestResponse.request()
            val res = result.httpRequestResponse.response()

            requestTextArea.text = req.toString().trimIndent()
            responseTextArea.text = res.toString().trimIndent()

            // scroll to top
            requestTextArea.caretPosition = 0
            responseTextArea.caretPosition = 0
    }

    private fun bindCtrlOAction() {
        val inputMap = requestTable.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = requestTable.actionMap

        inputMap.put(
            KeyStroke.getKeyStroke("control O"),
            "send-to-organizer"
        )

        actionMap.put("send-to-organizer", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                sendSelectedRowToOrganizer()
            }
        })

        logging.raiseInfoEvent("Ctrl+O key binding registered")
    }

    private fun installTablePopupMenu() {
        val popupMenu = JPopupMenu()

        val deleteItem = JMenuItem("Delete item")
        deleteItem.addActionListener {
            deleteSelectedRow()
        }

        popupMenu.add(deleteItem)

        requestTable.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                showPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                showPopup(e)
            }

            private fun showPopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return

                val row = requestTable.rowAtPoint(e.point)
                if (row != -1) {
                    requestTable.setRowSelectionInterval(row, row)
                }

                popupMenu.show(e.component, e.x, e.y)
            }
        })
    }

    private fun deleteSelectedRow() {
        val confirm = JOptionPane.showConfirmDialog(
            rootPanel,
            "Delete selected item?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        )

        if (confirm != JOptionPane.YES_OPTION) return

        val index = getSelectedRowIndex()
        val result = checklistResults[index]

        storage.delete(result.checklist.id, result.resultId, result.status)

        reloadTableFromStorage()
    }

    private fun exportJsonHandler() {
        try {
            if (checklistResults.isEmpty()) {
                JOptionPane.showMessageDialog(
                    rootPanel,
                    "No data to export",
                    "Export JSON",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            val chooser = JFileChooser().apply {
                dialogTitle = "Export Checklist Result as JSON"
                selectedFile = File("checklist-export.json")
            }

            val choice = chooser.showSaveDialog(null)
            if (choice != JFileChooser.APPROVE_OPTION) return

            val file = chooser.selectedFile
            val results = ArrayList<ChecklistExport>()

            checklistResults.forEach { result ->
                val http = result.httpRequestResponse
                val request = http.request()
                val response = http.response()

                val req = ChecklistExportRequest(
                    method = request.method(),
                    url = request.url(),
                    headers = request.headers().map { it.toString() },
                    body = request.bodyToString(),
                    raw = encode(request.toByteArray().bytes)
                )

                val res = response?.let {
                    ChecklistExportResponse(
                        statusCode = it.statusCode(),
                        headers = it.headers().map { h -> h.toString() },
                        body = it.bodyToString(),
                        raw = encode(it.toByteArray().bytes)
                    )
                }

                results.add(
                    ChecklistExport(
                        id = result.checklist.id,
                        title = result.checklist.title,
                        name = result.checklist.name,
                        request = req,
                        response = res,
                        status = result.status
                    )
                )
            }

            val gson = GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

            file.writeText(gson.toJson(results), Charsets.UTF_8)

            logging.raiseInfoEvent(
                "Checklist exported to ${file.absolutePath}"
            )

            JOptionPane.showMessageDialog(
                rootPanel,
                "Export completed:\n${file.absolutePath}",
                "Export JSON",
                JOptionPane.INFORMATION_MESSAGE
            )

        } catch (e: Exception) {
            logging.raiseErrorEvent("Export failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendSelectedRowToOrganizer() {
        val index = getSelectedRowIndex()

        if (index == -1) {
            return
        }

        val result = checklistResults[index]
        val httpRequestResponse = result.httpRequestResponse

        val annotations = httpRequestResponse.annotations()
        val notes = annotations.withNotes("[${result.status}] ${result.checklist.id}: ${result.checklist.title} - ${result.checklist.name}")

        organizer.sendToOrganizer(httpRequestResponse.withAnnotations(notes))
    }

    private fun resizeColumnsToFitContent(table: JTable) {
        val columnModel: TableColumnModel = table.columnModel
        val model = table.model

        SwingUtilities.invokeLater {
            for (col in 0 until table.columnCount) {
                var maxWidth = 0
                val column = columnModel.getColumn(col)

                val headerRenderer = table.tableHeader.defaultRenderer
                val headerComp = headerRenderer.getTableCellRendererComponent(
                    table, column.headerValue, false, false, 0, col
                )
                maxWidth = headerComp.preferredSize.width

                for (row in 0 until model.rowCount) {
                    val renderer = table.getCellRenderer(row, col)
                    val comp = renderer.getTableCellRendererComponent(
                        table, model.getValueAt(row, col), false, false, row, col
                    )
                    maxWidth = maxOf(maxWidth, comp.preferredSize.width)
                }

                column.preferredWidth = maxWidth
            }
        }
    }

    private fun reloadTableFromStorage() {
        val model = requestTable.model as DefaultTableModel

        fillTable(model)
        resizeColumnsToFitContent(requestTable)
    }

    private fun fillTable(model: DefaultTableModel) {
        checklistResults = storage.get()
        model.rowCount = 0

        checklistResults.forEachIndexed { index, (_, checklist, rr, status) ->
            val row = arrayOf(
                index + 1,
                checklist.id,
                checklist.title,
                checklist.name,
                rr.request().httpService().host(),
                rr.request().pathWithoutQuery(),
                status
            )

            model.addRow(row)
        }
    }
}
