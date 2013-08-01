/*
 * Copyright (c) 2008-2013, Matthias Mann
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
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AutoCompletionDataSource;
import de.matthiasmann.twl.model.AutoCompletionResult;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleAutoCompletionResult;
import de.matthiasmann.twl.utils.NaturalSortComparator;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.NodeReference;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 *
 * @author Matthias Mann
 */
public class NodeReferenceEditorFactory implements PropertyEditorFactory<NodeReference> {

    private final Context ctx;

    public NodeReferenceEditorFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Widget create(Property<NodeReference> prop, ExternalFetaures externalFetaures) {
        return new NodeReferenceEditor(ctx, (NodeReferenceProperty)prop);
    }

    static final class NodeReferenceEditor extends DialogLayout implements EditField.Callback {
        private final Context ctx;
        private final NodeReferenceProperty property;
        private final Runnable propertyCB;
        private final Button jumpBtn;
        private final Button applyBtn;
        private final EditField ef;
        private final ComboBox<String> cb;
        private ListModel<String> refableNodes;

        boolean lastKeyWasEscape;
        boolean inUpdateComboBox;
                    
        @SuppressWarnings("LeakingThisInConstructor")
        NodeReferenceEditor(Context ctx, NodeReferenceProperty property) {
            this.ctx = ctx;
            this.property = property;
            this.propertyCB = new Runnable() {
                public void run() {
                    propertyChanged();
                }
            };
            
            jumpBtn = new Button();
            jumpBtn.setTheme("jumpbutton");
            jumpBtn.addCallback(new Runnable() {
                public void run() {
                    jump();
                }
            });

            if(property.isSupportsWildcard()) {
                cb = null;
                
                applyBtn = new Button();
                applyBtn.setTheme("applybutton");
                applyBtn.setEnabled(false);
                applyBtn.addCallback(new Runnable() {
                    public void run() {
                        apply();
                    }
                });
                
                ef = new EditField() {
                    @Override
                    protected void keyboardFocusLost() {
                        super.keyboardFocusLost();
                        focusLost();
                    }
                };
                ef.setAutoCompletionOnSetText(false);
                ef.addCallback(this);
                ef.setAutoCompletion(new ACDS());
                
                setHorizontalGroup(createSequentialGroup(ef, applyBtn, jumpBtn));
                setVerticalGroup(createParallelGroup(ef, applyBtn, jumpBtn));
            } else {
                applyBtn = null;
                ef = null;
                
                cb = new ComboBox<String>();
                cb.addCallback(new Runnable() {
                    public void run() {
                        selectedChanged();
                    }
                });
                
                setHorizontalGroup(createSequentialGroup(cb, jumpBtn));
                setVerticalGroup(createParallelGroup(cb, jumpBtn));
            }
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            property.addValueChangedCallback(propertyCB);
            propertyChanged();
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            property.removeValueChangedCallback(propertyCB);
            super.beforeRemoveFromGUI(gui);
        }
        
        void propertyChanged() {
            NodeReference ref = property.getPropertyValue();
            jumpBtn.setEnabled(ref != null && !ref.isNone());
            if(ef != null) {
                ef.setText((ref != null) ? ref.toString() : "none");
            }
            if(cb != null) {
                getGUI().invokeLater(new Runnable() {
                    public void run() {
                        updateComboBox();
                    }
                });
            }
        }
        
        void updateComboBox() {
            inUpdateComboBox = true;
            try {
                NodeReference ref = property.getPropertyValue();
                if(refableNodes == null || !cb.hasOpenPopups()) {
                    refableNodes = getRefableNodes();
                    cb.setModel(refableNodes);
                }
                int selected = -1;
                if(ref != null) {
                    selected = Utils.find(refableNodes, ref.getName());
                    if(selected < 0) {
                        if(ref.getName() == null) {
                            cb.setDisplayTextNoSelection("");
                            cb.setNoSelectionIsError(false);
                        } else {
                            cb.setDisplayTextNoSelection(ref.getName());
                            cb.setNoSelectionIsError(true);
                        }
                    }
                }
                cb.setSelected(selected);
            } finally {
                inUpdateComboBox = false;
            }
        }
        
        void jump() {
            NodeReference targetRef = property.getPropertyValue();
            if(targetRef != null && !targetRef.isNone()) {
                if(targetRef.isWildcard()) {
                    ArrayList<ThemeTreeNode> nodes = new ArrayList<ThemeTreeNode>();
                    ctx.resolveReference(targetRef, nodes);
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
                    menu.openPopupMenu(jumpBtn);
                } else {
                    ctx.selectTarget(targetRef);
                }
            }
        }
        
        void apply() {
            try {
                NodeReference newRef = makeReference(ef.getText(), property.getKind());
                property.setPropertyValue(newRef);
                ef.setErrorMessage(null);
                applyBtn.setEnabled(false);
            } catch(IllegalArgumentException ex) {
                ef.setErrorMessage(ex.getMessage());
            }
        }
        
        void focusLost() {
            if(applyBtn.isEnabled()) {
                apply();
            }
        }
        
        void selectedChanged() {
            int selected = cb.getSelected();
            if(selected >= 0 && !inUpdateComboBox) {
                NodeReference newRef = new NodeReference(
                        cb.getModel().getEntry(selected), property.getKind());
                property.setPropertyValue(newRef);
                jumpBtn.setEnabled(!newRef.isNone());
            }
        }
        
        public void callback(int key) {
            if(key == Event.KEY_RETURN) {
                if(applyBtn.isEnabled()) {
                    apply();
                }
            } else if(key == Event.KEY_ESCAPE) {
                if(lastKeyWasEscape) {
                    NodeReference curRef = property.getPropertyValue();
                    ef.setText((curRef != null) ? curRef.toString() : "none");
                    ef.setErrorMessage(null);
                    jumpBtn.setEnabled(targetExists(curRef));
                    applyBtn.setEnabled(false);
                }
            } else {
                String errMsg = null;
                boolean canJump = false;
                boolean canApply = false;
                if(ef.getTextLength() > 0) {
                    Kind kind = property.getKind();
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
                jumpBtn.setEnabled(canJump);
                applyBtn.setEnabled(canApply);
            }
            lastKeyWasEscape = (key == Event.KEY_ESCAPE);
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
        
        ListModel<String> getRefableNodes() {
            return ctx.getRefableNodes(property.getLimit(), property.getKind());
        }
        
        class ACDS implements AutoCompletionDataSource {
            public AutoCompletionResult collectSuggestions(String text, int cursorPos, AutoCompletionResult prev) {
                ListModel<String> refableNodes = getRefableNodes();
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
        }
    }
    
}
