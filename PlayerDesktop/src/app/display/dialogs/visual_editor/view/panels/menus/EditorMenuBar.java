package app.display.dialogs.visual_editor.view.panels.menus;

import app.display.dialogs.visual_editor.view.panels.menus.viewMenu.ViewMenu;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Menu bar of visual editor
 * @author nic0gin
 */
public class EditorMenuBar extends JMenuBar {

    public EditorMenuBar() {

        JMenu settings = new JMenu("Settings"); // adjust editor settings e.g. font size, colors ect.
        // adding settings menu items
        addJMenuItem(settings, "Open settings...", null);

        JMenu about = new JMenu("About"); // read about the editor: documentation, research report, DLP
        // adding about menu items
        addJMenuItem(about, "Open documentation", null);
        addJMenuItem(about, "Learn more about the editor", null); // opens research paper
        addJMenuItem(about, "Learn more about DLP", null);

        add(new FileMenu(this));
        add(new EditMenu(this));
        add(new ViewMenu(this));
        add(new TreeLayoutMenu(this));
        //add(settings);
        add(new RunMenu(this));
        add(about);
    }

    public void addJMenuItem(JMenu menu, String itemName, ActionListener actionListener)
    {
        JMenuItem jMenuItem = new JMenuItem(itemName);
        jMenuItem.addActionListener(actionListener);
        menu.add(jMenuItem);
    }

    public void addJMenuItem(JMenu menu, String itemName, ActionListener actionListener, KeyStroke keyStroke)
    {
        JMenuItem jMenuItem = new JMenuItem(itemName);
        jMenuItem.addActionListener(actionListener);
        jMenuItem.setAccelerator(keyStroke);
        menu.add(jMenuItem);
    }

    public void addJCheckBoxMenuItem(JMenu menu, String itemName, boolean selected, ActionListener actionListener)
    {
        JCheckBoxMenuItem jMenuItem = new JCheckBoxMenuItem(itemName);
        jMenuItem.addActionListener(actionListener);
        jMenuItem.setSelected(selected);
        menu.add(jMenuItem);
    }

    public void addJCheckBoxMenuItem(JMenu menu, String itemName, boolean selected, ActionListener actionListener, KeyStroke keyStroke)
    {
        JCheckBoxMenuItem jMenuItem = new JCheckBoxMenuItem(itemName);
        jMenuItem.addActionListener(actionListener);
        jMenuItem.setSelected(selected);
        jMenuItem.setAccelerator(keyStroke);
        menu.add(jMenuItem);
    }
}

