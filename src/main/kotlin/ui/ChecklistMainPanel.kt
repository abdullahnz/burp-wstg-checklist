package io.github.d0ublew.bapp.starter.ui

import burp.api.montoya.MontoyaApi
import burp.api.montoya.logging.Logging
import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import com.google.gson.GsonBuilder
import io.github.d0ublew.bapp.starter.Storage
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExport
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExportRequest
import io.github.d0ublew.bapp.starter.dataclass.ChecklistExportResponse
import io.github.d0ublew.bapp.starter.dataclass.ChecklistResult
import io.github.d0ublew.bapp.starter.encode
import io.github.d0ublew.bapp.starter.isDarkTheme
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
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
import javax.swing.table.TableRowSorter

class ChecklistMainPanel(
    private val api: MontoyaApi,
    private val storage: Storage,
    private val logging: Logging
) {

    private val rootPanel: JPanel = JPanel(BorderLayout())
    private var checklistResults = mutableListOf<ChecklistResult>()

    private lateinit var requestTable: JTable
    private lateinit var requestEditor: HttpRequestEditor
    private lateinit var responseEditor: HttpResponseEditor

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
            "No.", "ResultId", "ID", "Title", "Name", "Host", "Path", "Passed"
        )

        val tableModel = DefaultTableModel(columnNames, 0)

        fillTable(tableModel)

        requestTable = JTable(tableModel)

        // hide resultid
        requestTable.columnModel.getColumn(1).apply {
            minWidth = 0
            maxWidth = 0
            width = 0
        }

        val sorter = TableRowSorter(requestTable.model)

        sorter.setComparator(0) { a, b ->
            (a as Int).compareTo(b as Int)
        }
        requestTable.rowSorter = sorter

        val headerRenderer = requestTable.tableHeader.defaultRenderer
        if (headerRenderer is JLabel) {
            headerRenderer.horizontalAlignment = SwingConstants.LEFT
        }

        val statusColumnIndex = columnNames.size - 1

        requestTable.columnModel
            .getColumn(statusColumnIndex)
            .cellRenderer = StatusCheckboxRenderer()


        installPopupMenus()
        bindCtrlOAction()

        resizeColumnsToFitContent(requestTable)

        requestTable.selectionModel.addListSelectionListener(
            tableSelectionListener()
        )


        return JScrollPane(requestTable)
    }

    private fun buildBottomPanel(): JSplitPane {
        requestEditor = api.userInterface().createHttpRequestEditor()
        responseEditor = api.userInterface().createHttpResponseEditor()

        val request = createPanel("Request")
        val response = createPanel("Response")

        request.add(requestEditor.uiComponent())
        response.add(responseEditor.uiComponent())

        request.background = requestEditor.uiComponent().background
        response.background = responseEditor.uiComponent().background

        val panel = JSplitPane(SwingConstants.VERTICAL,
            request,
            response)

        panel.resizeWeight = 0.5

        return panel
    }

    private fun createPanel(text: String): JPanel {
        val panel = JPanel(BorderLayout())

        val label = JLabel(text)
        label.font = label.font.deriveFont(Font.BOLD, 13f)
        if (isDarkTheme()) {
            label.foreground = Color.WHITE
        }
        label.horizontalAlignment = SwingConstants.LEFT
        label.border = BorderFactory.createEmptyBorder(
            20,
            10,
            4,
            10
        )

        panel.add(label, BorderLayout.NORTH)

        return panel
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

    private fun tableSelectionListener(): ListSelectionListener =
        ListSelectionListener {

            if (it.valueIsAdjusting) return@ListSelectionListener

            val viewRow = requestTable.selectedRow
            if (viewRow == -1) return@ListSelectionListener

            val modelRow = requestTable.convertRowIndexToModel(viewRow)

            val resultId = requestTable.model
                .getValueAt(modelRow, 1) as? String
                ?: return@ListSelectionListener

            val result = storage.getByResultId(resultId) ?: return@ListSelectionListener

            requestEditor.request = result.httpRequestResponse.request()
            responseEditor.response = result.httpRequestResponse.response()
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

    private fun installPopupMenus() {
        val popupMenu = JPopupMenu()

        val sendToOrganizer = JMenuItem("Send to Organizer")
        sendToOrganizer.addActionListener {
            sendSelectedRowToOrganizer()
        }

        val deleteItem = JMenuItem("Delete item")
        deleteItem.addActionListener {
            deleteSelectedRow()
        }

        popupMenu.add(deleteItem)
        popupMenu.add(sendToOrganizer)

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
        val result = getSelectedResult() ?: return

        val confirm = JOptionPane.showConfirmDialog(
            rootPanel,
            "Delete selected item?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        )

        if (confirm != JOptionPane.YES_OPTION) return

        storage.delete(result.resultId)
        reloadTableFromStorage()
    }


    private fun exportJsonHandler() {
        try {
            val results = storage.get()

            if (results.isEmpty()) {
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
            val exports = ArrayList<ChecklistExport>()

            results.forEach { result ->
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

                exports.add(
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

            file.writeText(gson.toJson(exports), Charsets.UTF_8)

            logging.raiseInfoEvent("Checklist exported to ${file.absolutePath}")

            JOptionPane.showMessageDialog(
                rootPanel,
                "Export saved at ${file.absolutePath}",
                "Export JSON",
                JOptionPane.INFORMATION_MESSAGE
            )

        } catch (e: Exception) {
            logging.raiseErrorEvent("Export failed: ${e.message}")
            e.printStackTrace()
        }
    }


    private fun sendSelectedRowToOrganizer() {
        val result = getSelectedResult() ?: return

        val http = result.httpRequestResponse

        val notes = http.annotations()
            .withNotes("[${result.status}] ${result.checklist.id}: ${result.checklist.title} - ${result.checklist.name}")

        api.organizer()
            .sendToOrganizer(http.withAnnotations(notes))
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

        checklistResults.forEachIndexed { index, result ->
            val row = arrayOf(
                index + 1,
                result.resultId,
                result.checklist.id,
                result.checklist.title,
                result.checklist.name,
                result.httpRequestResponse.request().httpService().host(),
                result.httpRequestResponse.request().pathWithoutQuery(),
                result.status
            )

            model.addRow(row)
        }
    }

    private fun getSelectedResult(): ChecklistResult? {
        val viewRow = requestTable.selectedRow
        if (viewRow == -1) return null

        val modelRow = requestTable.convertRowIndexToModel(viewRow)

        // column 1 = hidden ResultId
        val resultId = requestTable.model
            .getValueAt(modelRow, 1) as? String
            ?: return null

        return storage.getByResultId(resultId)
    }


}
