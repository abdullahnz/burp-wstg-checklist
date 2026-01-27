package io.github.d0ublew.bapp.starter

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import io.github.d0ublew.bapp.starter.ui.ChecklistSelectionList
import io.github.d0ublew.bapp.starter.ui.ChecklistTableTab

class App : BurpExtension {
//    private lateinit var checklistTab: ChecklistTableTab

    override fun initialize(api: MontoyaApi) {
        val logger = api.logging()

        api.extension().setName(EXT_NAME)
        logger.logToOutput("$EXT_NAME v$EXT_VERSION has been loaded")

        val storage = Storage(api, logger)
        storage.debug()

        // register checklist selection
        api.userInterface().registerContextMenuItemsProvider(
            ChecklistSelectionList(
                storage,
                logger
            )
        )

        api.extension().registerUnloadingHandler {
            logger.logToOutput("$EXT_NAME v$EXT_VERSION has been unloaded")
        }
    }
}