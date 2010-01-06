/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 *
 * @author MannMat
 */
public abstract class NodeWrapper {

    protected final ThemeFile themeFile;
    protected final Element root;

    protected NodeWrapper(ThemeFile themeFile, Element root) {
        this.themeFile = themeFile;
        this.root = root;
    }

    protected String getAttribute(String name) {
        Attribute attr = root.getAttribute(name);
        return (attr != null) ? attr.getValue() : null;
    }

    protected int parseIntFromAttribute(String name) throws NumberFormatException {
        String value = getAttribute(name);
        if(value == null) {
            throw new NumberFormatException("missing attribute: " + name);
        }
        return Integer.parseInt(value);
    }

    protected boolean parseBoolFromAttribute(String name, boolean defaultValue) {
        String value = getAttribute(name);
        if(value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    protected void setAttribute(String name, String value) {
        String oldValue = getAttribute(name);
        if(!equals(value, oldValue)) {
            if(value == null) {
                root.removeAttribute(name);
            } else {
                root.setAttribute(name, value);
            }
            themeFile.fireCallbacks();
        }
    }

    protected void setAttribute(String name, int value) {
        setAttribute(name, Integer.toString(value));
    }

    protected void setAttribute(String name, boolean value, boolean defaultValue) {
        if(value == defaultValue) {
            setAttribute(name, null);
        } else {
            setAttribute(name, value ? "true" : "false");
        }
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
