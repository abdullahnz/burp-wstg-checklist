package io.github.d0ublew.bapp.starter

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import io.github.d0ublew.bapp.starter.ui.ChecklistMainPanel
import io.github.d0ublew.bapp.starter.ui.ChecklistSelectionList

class App : BurpExtension {
    override fun initialize(api: MontoyaApi) {
        val logger = api.logging()
        val storage = Storage(api, logger)

        api.extension().setName(EXT_NAME)
        logger.logToOutput("$EXT_NAME v$EXT_VERSION has been loaded")

        // register checklist selection
        api.userInterface().registerContextMenuItemsProvider(
            ChecklistSelectionList(
                storage,
                logger
            )
        )

        // checklist tab
        api.userInterface().registerSuiteTab(
            EXT_NAME, ChecklistMainPanel(
                api,
                storage,
                logger
            ).getPanel()
        )

        api.extension().registerUnloadingHandler {
            logger.logToOutput("$EXT_NAME v$EXT_VERSION has been unloaded")
        }
    }
}