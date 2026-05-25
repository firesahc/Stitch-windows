package soko.ekibun.stitch.ui

import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * Manages keyboard shortcut bindings for the editor.
 *
 * @param rootPane The [JRootPane] to bind shortcuts to.
 * @param actions A map of action names to callback lambdas.
 *   Keys: "undo", "selectAll", "selectClear", "save", "stitch", "delete",
 *         "selHandleB", "selHandleA", "decValue", "incValue".
 */
class ShortcutManager(rootPane: JRootPane, actions: Map<String, () -> Unit>) {

    init {
        // ── Simple key bindings (WHEN_FOCUSED) ──
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().menuShortcutKeyMask),
            "undo", actions["undo"]
        )
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().menuShortcutKeyMask),
            "selectAll", actions["selectAll"]
        )
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "selectClear", actions["selectClear"]
        )
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().menuShortcutKeyMask),
            "save", actions["save"]
        )
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().menuShortcutKeyMask),
            "stitch", actions["stitch"]
        )
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "delete", actions["delete"]
        )
        // Backspace also maps to "delete"
        putBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
            "delete", actions["delete"]
        )

        // ── Ancestor-focused bindings (Up/Down/Left/Right) ──
        // These use WHEN_ANCESTOR_OF_FOCUSED_COMPONENT so child components
        // (e.g. JTextField) can also trigger the action.
        putAncestorBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            "selHandleB", actions["selHandleB"]
        )
        putAncestorBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            "selHandleA", actions["selHandleA"]
        )
        putAncestorBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
            "decValue", actions["decValue"]
        )
        putAncestorBinding(
            rootPane,
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
            "incValue", actions["incValue"]
        )
    }

    /**
     * Registers a key binding in the default (WHEN_FOCUSED) input map.
     */
    private fun putBinding(rootPane: JRootPane, keyStroke: KeyStroke, actionName: String, action: (() -> Unit)?) {
        rootPane.inputMap.put(keyStroke, actionName)
        if (action != null) {
            rootPane.actionMap.put(actionName, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    action()
                }
            })
        }
    }

    /**
     * Registers a key binding using [JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT],
     * allowing child components to trigger the action.
     */
    private fun putAncestorBinding(rootPane: JRootPane, keyStroke: KeyStroke, actionName: String, action: (() -> Unit)?) {
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionName)
        if (action != null) {
            rootPane.actionMap.put(actionName, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    action()
                }
            })
        }
    }
}
