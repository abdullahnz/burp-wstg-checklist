package io.github.d0ublew.bapp.starter.ui

import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class StatusCheckboxRenderer : JCheckBox(), TableCellRenderer {

    init {
        horizontalAlignment = CENTER
        isOpaque = true
        isEnabled = false
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        rowSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {

        this.isSelected = when (value) {
            is Boolean -> value
            is String -> value.equals("PASSED", ignoreCase = true)
            else -> false
        }

        val base = table.getDefaultRenderer(Any::class.java)
            .getTableCellRendererComponent(
                table, value, rowSelected, hasFocus, row, column
            )

        background = base.background
        foreground = base.foreground

        return this
    }
}
