/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.datamodel;

import java.util.List;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class NodeWrapper {

    protected final ThemeFile themeFile;
    protected final Element node;

    protected NodeWrapper(ThemeFile themeFile, Element node) {
        this.themeFile = themeFile;
        this.node = node;
    }

    protected ThemeFile getThemeFile() {
        return themeFile;
    }

    protected String getAttribute(String name) {
        Attribute attr = node.getAttribute(name);
        return (attr != null) ? attr.getValue() : null;
    }

    @SuppressWarnings("unchecked")
    public List<Attribute> getAttributes() {
        return (List<Attribute>)node.getAttributes();
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
        if(!Utils.equals(value, oldValue)) {
            if(value == null) {
                node.removeAttribute(name);
            } else {
                node.setAttribute(name, value);
            }
            getThemeFile().fireCallbacks();
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

}
