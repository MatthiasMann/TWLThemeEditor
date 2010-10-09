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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AutoCompletionDataSource;
import de.matthiasmann.twl.model.AutoCompletionResult;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.SimpleAutoCompletionResult;
import de.matthiasmann.twl.utils.NaturalSortComparator;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.NodeReference;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 *
 * @author Matthias Mann
 */
public class NodeReferenceEditorFactory implements PropertyEditorFactory<NodeReference, NodeReferenceProperty> {

    private final Context ctx;

    public NodeReferenceEditorFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Widget create(final PropertyAccessor<NodeReference, NodeReferenceProperty> pa) {
        ThemeTreeNode limit = pa.getProperty().getLimit();
        final Kind kind = pa.getProperty().getKind();
        final NodeReference ref = pa.getValue(null);
        final ListModel<String> refableNodes = ctx.getRefableNodes(limit, kind);
        final Button btnJump = new Button();

        btnJump.setTheme("jumpbutton");
        btnJump.setEnabled(ref != null && !ref.isNone());
        btnJump.addCallback(new Runnable() {
            public void run() {
                NodeReference targetRef = pa.getValue(null);
                if(targetRef != null && !targetRef.isNone()) {
                    if(targetRef.isWildcard()) {
                        ArrayList<ThemeTreeNode> nodes = new ArrayList<ThemeTreeNode>();
                        ctx.resolveReference(ref, nodes);
                        Menu menu = new Menu();
                        if(nodes.isEmpty()) {
                            menu.add("No target found", (Runnable)null);
                        } else {
                            for(final ThemeTreeNode node : nodes) {
                                menu.add(node.getName(), new Runnable() {
                                    public void run() {
                                        ctx.selectTarget(node);
                                    }
                                });
                            }
                        }
                        menu.openPopupMenu(btnJump);
                    } else {
                        ctx.selectTarget(targetRef);
                    }
                }
            }
        });

        DialogLayout l = new DialogLayout();
        l.setTheme("nodereferenceeditor");

        if(pa.getProperty().isSupportsWildcard()) {
            final Button applyBtn = new Button();
            final EditField ef = new EditField();
            final Runnable applyCB = new Runnable() {
                public void run() {
                    try {
                        NodeReference newRef = makeReference(ef.getText(), kind);
                        pa.setValue(newRef);
                        ef.setErrorMessage(null);
                        applyBtn.setEnabled(false);
                    } catch(IllegalArgumentException ex) {
                        ef.setErrorMessage(ex.getMessage());
                    }
                }
            };
            applyBtn.setTheme("applybutton");
            applyBtn.addCallback(applyCB);
            applyBtn.setEnabled(false);
            ef.setText((ref != null) ? ref.toString() : "none");
            ef.addCallback(new EditField.Callback() {
                public void callback(int key) {
                    if(key == Event.KEY_RETURN) {
                        if(applyBtn.isEnabled()) {
                            applyCB.run();
                        }
                    } else {
                        String errMsg = null;
                        boolean canJump = false;
                        boolean canApply = false;
                        if(ef.getTextLength() > 0) {
                            try {
                                NodeReference newRef = makeReference(ef.getText(), kind);
                                canJump = targetExists(newRef);
                                canApply = canJump;
                                if(!canJump) {
                                    if(!newRef.isWildcard()) {
                                        errMsg = "Referenced " + kind.toString().toLowerCase() + " not found";
                                    } else {
                                        canApply = true;
                                    }
                                }
                            } catch(IllegalArgumentException ex) {
                                errMsg = ex.getMessage();
                            }
                        }
                        ef.setErrorMessage(errMsg);
                        btnJump.setEnabled(canJump);
                        applyBtn.setEnabled(canApply);
                    }
                }
            });
            ef.setAutoCompletion(new AutoCompletionDataSource() {
                public AutoCompletionResult collectSuggestions(String text, int cursorPos, AutoCompletionResult prev) {
                    ArrayList<String> results = new ArrayList<String>();
                    HashSet<String> wildcards = new HashSet<String>();
                    for(int i=0,n=refableNodes.getNumEntries() ; i<n ; i++) {
                        String refable = refableNodes.getEntry(i);
                        if(refable.startsWith(text)) {
                            results.add(refable);
                            int idx = refable.indexOf('.', text.length());
                            if(idx >= 0) {
                                wildcards.add(refable.substring(0, idx).concat(".*"));
                            }
                        }
                    }
                    if(text.endsWith(".")) {
                        results.add(text.concat("*"));
                    }
                    if(results.isEmpty() && wildcards.isEmpty()) {
                        return null;
                    }
                    results.addAll(wildcards);
                    Collections.sort(results, NaturalSortComparator.stringComparator);
                    return new SimpleAutoCompletionResult(text, cursorPos, results);
                }
            });
            l.setHorizontalGroup(l.createSequentialGroup(ef, applyBtn, btnJump));
            l.setVerticalGroup(l.createParallelGroup(ef, applyBtn, btnJump));
        } else {
            final ComboBox<String> cb = new ComboBox<String>(refableNodes);
            cb.setSelected((ref != null) ? Utils.find(refableNodes, ref.getName()) : -1);
            cb.addCallback(new Runnable() {
                public void run() {
                    int selected = cb.getSelected();
                    if(selected >= 0) {
                        NodeReference newRef = new NodeReference(refableNodes.getEntry(selected), kind);
                        pa.setValue(newRef);
                        btnJump.setEnabled(!newRef.isNone());
                    }
                }
            });
            l.setHorizontalGroup(l.createSequentialGroup(cb, btnJump));
            l.setVerticalGroup(l.createParallelGroup(cb, btnJump));
        }

        return l;
    }

    NodeReference makeReference(String text, Kind kind) {
        char prevChar = '.';
        for(int i=0 ; i<text.length() ; i++) {
            char ch = text.charAt(i);
            if(ch == '.') {
                if(i == 0) {
                    throw new IllegalArgumentException("reference can't start with a '.'");
                } else if(prevChar == '.') {
                    throw new IllegalArgumentException("reference must not contain \"..\"");
                }
            } else if(ch == '*') {
                if(i != text.length()-1 || i == 0) {
                    throw new IllegalArgumentException("wildcard may only appear at the end");
                } else if(prevChar != '.') {
                    throw new IllegalArgumentException("wildcard must follow a '.'");
                }
            } else if(!Character.isJavaIdentifierPart(ch) && ch != '-') {
                throw new IllegalArgumentException("reference contains invalid character: " + ch);
            }
            prevChar = ch;
        }
        if(prevChar == '.') {
            throw new IllegalArgumentException("reference can't end with '.'");
        }
        return new NodeReference(text, kind);
    }

    boolean targetExists(NodeReference ref) {
        if(ref.isNone()) {
            return true;
        }
        ArrayList<ThemeTreeNode> nodes = new ArrayList<ThemeTreeNode>();
        ctx.resolveReference(ref, nodes);
        return !nodes.isEmpty();
    }
}
