package burp

import com.google.gson.Gson
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenuItem


class BurpExtender: IBurpExtender {
    companion object {
        lateinit var callbacks: IBurpExtenderCallbacks
    }

    override fun registerExtenderCallbacks(callbacks: IBurpExtenderCallbacks) {
        Companion.callbacks = callbacks
        callbacks.setExtensionName("Repeater cookie switch")
        callbacks.registerContextMenuFactory(ContextMenuFactory())
    }
}


class ContextMenuFactory: IContextMenuFactory {
    override fun createMenuItems(invocation: IContextMenuInvocation): List<JMenuItem> {
        if (invocation.invocationContext == 0.toByte()) {
            val menuItem = JCheckBoxMenuItem("Repeater uses cookies", isRepeaterUsingCookies())
            menuItem.addActionListener {
                setRepeaterUsingCookies(menuItem.isSelected)
            }
            return listOf(menuItem)
        }
        else {
            return emptyList()
        }
    }
}


fun setRepeaterUsingCookies(usingCookies: Boolean) {
    val root = getConfigRoot()
    for(rule in root.project_options.sessions.session_handling_rules.rules) {
        if(rule.actions.any{ it.type == "use_cookies" }) {
            val append = if (usingCookies) { listOf("Repeater") } else { emptyList() }
            rule.tools_scope = rule.tools_scope.filter{ it != "Repeater" } + append
        }
    }
    BurpExtender.callbacks.loadConfigFromJson(Gson().toJson(root))
}


fun isRepeaterUsingCookies(): Boolean {
    for(rule in getConfigRoot().project_options.sessions.session_handling_rules.rules) {
        if(rule.actions.any{ it.type == "use_cookies" } &&
                rule.tools_scope.contains("Repeater")) {
            return true
        }
    }
    return false
}


fun getConfigRoot(): Root {
    val configString = BurpExtender.callbacks.saveConfigAsJson("project_options.sessions.session_handling_rules")
    try {
        return Gson().fromJson(configString, Root::class.java)
    }
    catch(ex: Exception) {
        BurpExtender.callbacks.printError(ex.toString())
        throw ex
    }
}
