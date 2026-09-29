package dev.chungjungsoo.gptmobile.navigation

import dev.chungjungsoo.gptmobile.presentation.common.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeSetupNavigationTest {
    @Test fun setupServerEditRouteRequestsReturnToChats() {
        val route = Route.OPEN_CODE_SERVER_EDIT
            .replace("{serverId}", "new")
            .replace("{returnToChats}", "true")
        assertEquals("opencode_server_edit?serverId=new&returnToChats=true", route)
    }

    @Test fun settingsServerEditRouteKeepsSettingsBackStack() {
        val route = Route.OPEN_CODE_SERVER_EDIT
            .replace("{serverId}", "server")
            .replace("{returnToChats}", "false")
        assertEquals("opencode_server_edit?serverId=server&returnToChats=false", route)
    }
}
