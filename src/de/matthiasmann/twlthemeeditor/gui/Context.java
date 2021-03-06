/*
 * Copyright (c) 2008-2012, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twlthemeeditor.gui.editors.ConditionEditorFactory;
import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AutoCompletionDataSource;
import de.matthiasmann.twl.model.AutoCompletionResult;
import de.matthiasmann.twlthemeeditor.gui.editors.NodeReferenceEditorFactory;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.SimpleAutoCompletionResult;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twlthemeeditor.datamodel.Condition;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.NodeReference;
import de.matthiasmann.twlthemeeditor.datamodel.Theme;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.gui.editors.AnimStateEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.WidgetThemeEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.WidgetThemeProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author Matthias Mann
 */
public class Context extends PropertyFactories {

    private static final MessageLog.Category CAT_JUMP  = new MessageLog.Category("Jump to element", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
    private static final MessageLog.Category CAT_ERROR = new MessageLog.Category("Exception", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    
    private final MessageLog messageLog;
    private final ThemeTreeModel model;
    private final PreviewDebugHook debugHook;

    private ThemeTreePane themeTreePane;

    public Context(MessageLog messageLog, ThemeTreeModel model) {
        this.messageLog = messageLog;
        this.model = model;
        this.debugHook = new PreviewDebugHook();
        
        factoriesP.put(NodeReferenceProperty.class, new NodeReferenceEditorFactory(this));
        factoriesP.put(WidgetThemeProperty.class, new WidgetThemeEditorFactory(this));
        
        factoriesT.put(Condition.class, new ConditionEditorFactory(this));
        factoriesT.put(AnimationState.class, new AnimStateEditorFactory(this));
    }

    public void logMessage(MessageLog.Entry entry) {
        messageLog.add(entry);
    }

    private void logException(String action, String detail, Throwable ex) {
        logMessage(new MessageLog.Entry(CAT_ERROR, "Exception while "+action, detail, ex));
    }

    public ListModel<String> getRefableNodes(ThemeTreeNode stopAt, Kind kind) {
        SimpleChangableListModel<String> result = new SimpleChangableListModel<String>();
        switch (kind) {
            case IMAGE:
                result.addElement("none");
                // fall through
            case CURSOR:
                for(Image img : model.getImages(stopAt)) {
                    if(img.getKind() == kind) {
                        result.addElement(img.getName());
                    }
                }
                break;
            default:
                for(ThemeTreeNode node : model.getTopLevelNodes(ThemeTreeNode.class, stopAt)) {
                    if(node.getKind() == kind) {
                        result.addElement(node.getName());
                    }
                }
        }
        return result;
    }

    public Theme findTheme(String[] themePath) {
        if(themePath != null && themePath.length > 0) {
            ThemeData td = findThemeData(themePath);
            if(td != null) {
                return td.themeNode;
            }
        }
        return null;
    }
    
    public ListModel<String> getRefableThemes(String[] themePath) {
        if(themePath.length == 0) {
            return getRefableNodes(null, Kind.THEME);
        }
        SimpleChangableListModel<String> result = new SimpleChangableListModel<String>();
        ThemeData td = findThemeData(themePath);
        if(td != null) {
            result.addElements(td.children.keySet());
            if(td.hasWildcard) {
                TreeSet<String> wildcardThemes = new TreeSet<String>();
                for(ThemeTreeNode node : model.getTopLevelNodes(ThemeTreeNode.class, null)) {
                    if(node instanceof Theme) {
                        Theme toplevelTheme = (Theme)node;
                        if(toplevelTheme.isAllowWildcard()) {
                            String name = toplevelTheme.getName();
                            // don't add themes which are hidden by local themes
                            if(!td.children.containsKey(name)) {
                                wildcardThemes.add(name);
                            }
                        }
                    }
                }
                result.addElements(wildcardThemes);
            }
            // add absolute themes
            TreeSet<String> toplevelThemes = new TreeSet<String>();
            for(ThemeTreeNode node : model.getTopLevelNodes(ThemeTreeNode.class, null)) {
                if(node instanceof Theme) {
                    toplevelThemes.add("/".concat(((Theme)node).getName()));
                }
            }
            result.addElements(toplevelThemes);
        }
        return result;
    }

    private ThemeData findThemeData(String[] themePath) {
        HashMap<String, ThemeData> toplevelThemes = new HashMap<String, ThemeData>();
        ThemeData td = parseTheme(themePath[0], toplevelThemes);
        for(int idx = 1; idx < themePath.length && td != null; ++idx) {
            td = getChildTheme(td, themePath[idx], toplevelThemes);
        }
        return td;
    }

    private ThemeData getChildTheme(ThemeData td, String name, HashMap<String, ThemeData> toplevelThemes) {
        ThemeData tdChild = td.children.get(name);
        if(tdChild == null && td.hasWildcard) {
            return parseTheme(name, toplevelThemes);
        }
        return tdChild;
    }

    private ThemeData parseTheme(String name, HashMap<String, ThemeData> toplevelThemes) {
        ThemeData td = toplevelThemes.get(name);
        if(td == null && model != null) {
            TreeTableNode refNode = model.findNode(name, Kind.THEME);
            if(refNode instanceof Theme) {
                Theme refTheme = (Theme)refNode;
                td = parseTheme(null, refTheme, toplevelThemes);
            }
        }
        return td;
    }

    private ThemeData parseTheme(ThemeData parent, Theme themeNode, HashMap<String, ThemeData> toplevelThemes) {
        ThemeData td = new ThemeData(themeNode);
        if(parent == null) {
            toplevelThemes.put(td.name, td);
        }

        // this logic should match ThemeManager !!
        if(themeNode.isMerge() && parent != null) {
            ThemeData tdPrev = parent.children.get(td.name);
            if(tdPrev != null) {
                td.copy(tdPrev);
            }
        }
        NodeReference ref = themeNode.getRef();
        if(ref != null) {
            ThemeData tdRef = parseTheme(ref.getName(), toplevelThemes);
            if(tdRef != null) {
                td.copy(tdRef);
            }
        }
        
        for(int i=0,n=themeNode.getNumChildren() ; i<n ; i++) {
            TreeTableNode childNode = themeNode.getChild(i);
            if(childNode instanceof Theme) {
                Theme childTheme = (Theme)childNode;
                if(childTheme.isWildcard()) {
                    td.hasWildcard = true;
                } else {
                    ThemeData tdChild = parseTheme(td, childTheme, toplevelThemes);
                    td.children.put(tdChild.name, tdChild);
                }
            }
        }

        return td;
    }

    public AutoCompletionDataSource collectAllStates() {
        Set<String> statesSet = new TreeSet<String>();
        for(Image img : model.getImages(null)) {
            collectStates(img, statesSet);
        }

        for(int i=0,n=StateKey.getNumStateKeys() ; i<n ; i++) {
            statesSet.add(StateKey.get(i).getName());
        }

        final String[] states = statesSet.toArray(new String[statesSet.size()]);
        return new AutoCompletionDataSource() {
            public AutoCompletionResult collectSuggestions(String text, int cursorPos, AutoCompletionResult prev) {
                int prefixLength = cursorPos;
                while(prefixLength > 0 && Character.isJavaIdentifierPart(text.charAt(prefixLength-1))) {
                    prefixLength--;
                }
                if(prefixLength == cursorPos) {
                    return null;
                }
                int postfixStart = cursorPos;
                while(postfixStart < text.length() && Character.isJavaIdentifierPart(text.charAt(postfixStart))) {
                    ++postfixStart;
                }
                String searchText = text.substring(prefixLength, cursorPos).toLowerCase();
                String prefixText = text.substring(0, prefixLength);
                String postfixText = text.substring(postfixStart);
                ArrayList<String> answers = new ArrayList<String>();
                ArrayList<String> answers2 = new ArrayList<String>();
                for(String state : states) {
                    int idx = state.toLowerCase().indexOf(searchText);
                    if(idx >= 0) {
                        String completion = prefixText + state + postfixText;
                        if(idx == 0) {
                            answers.add(completion);
                        } else {
                            answers2.add(completion);
                        }
                    }
                }
                answers.addAll(answers2);
                final int postfixLength = postfixText.length();
                return new SimpleAutoCompletionResult(text, prefixLength, answers) {
                    @Override
                    public int getCursorPosForResult(int idx) {
                        return super.getResult(idx).length() - postfixLength;
                    }
                };
            }
        };
    }

    public void selectTarget(NodeReference ref) {
        if(themeTreePane != null && ref != null) {
            try {
                ThemeTreeNode node = resolveReference(ref);
                if(node != null) {
                    themeTreePane.selectNode(node);
                } else {
                    logMessage(new MessageLog.Entry(CAT_JUMP, "Could not locate theme element", ref.getKind() + " " + ref.getName(), null));
                }
            } catch(Throwable ex) {
                logException("jump to theme element", ref.getKind() + " " + ref.getName(), ex);
            }
        }
    }

    public void selectTarget(ThemeTreeNode node) {
        if(themeTreePane != null && node != null) {
            try {
                themeTreePane.selectNode(node);
            } catch(Throwable ex) {
                logException("jump to theme element", node.getKind() + " " + node.getName(), ex);
            }
        }
    }

    public void resolveReference(NodeReference ref, Collection<ThemeTreeNode> nodes) {
        if(ref != null && !ref.isNone()) {
            if(ref.isWildcard()) {
                model.collectNodes(ref.getBaseName(), ref.getKind(), nodes);
            } else {
                ThemeTreeNode node = model.findNode(ref.getName(), ref.getKind());
                if(node != null) {
                    nodes.add(node);
                }
            }
        }
    }

    private ThemeTreeNode resolveReference(NodeReference ref) {
        if(ref != null && !ref.isNone()) {
            return model.findNode(ref.getName(), ref.getKind());
        }
        return null;
    }

    public void selectTheme(String[] themePath) {
        if(themeTreePane != null) {
            try {
                Theme theme = findTheme(themePath);
                if(theme != null) {
                    themeTreePane.selectNode(theme);
                } else {
                    logMessage(new MessageLog.Entry(CAT_JUMP, "Could not locate theme element", Arrays.toString(themePath), null));
                }
            } catch(Throwable ex) {
                logException("jump to theme element", Arrays.toString(themePath), ex);
            }
        }
    }

    public ThemeTreeModel getThemeTreeModel() {
        return model;
    }

    public ThemeTreePane getThemeTreePane() {
        return themeTreePane;
    }

    public void setThemeTreePane(ThemeTreePane themeTreePane) {
        this.themeTreePane = themeTreePane;
    }

    private void collectStates(Image img, Set<String> states) {
        String cond = img.getCondition().getCondition();
        for(int i=0,n=cond.length() ; i<n ;) {
            while(i<n && !Character.isJavaIdentifierStart(cond.charAt(i))) {
                i++;
            }
            int start = i;
            while(i<n && Character.isJavaIdentifierPart(cond.charAt(i))) {
                i++;
            }
            String state = cond.substring(start, i);
            states.add(state);
        }

        for(Image child : img.getChildren(Image.class)) {
            collectStates(child, states);
        }
    }

    public void uninstallDebugHook() {
        debugHook.uninstall();
    }

    public void installDebugHook() {
        debugHook.install();
    }

    public void clearWidgetMessages() {
        debugHook.clear();
    }
    
    public void clearWidgetMessages(Widget startingFromWidget) {
        debugHook.clear(startingFromWidget);
    }    

    public boolean checkLayoutValidated() {
        return debugHook.checkLayoutValidated();
    }

    public Object getTooltipForWidget(Widget widget) {
        PreviewDebugHook.Entry entry = debugHook.getEntry(widget);
        if(entry != null) {
            StringBuilder sb = new StringBuilder();
            appendMsgs(sb, entry.errorMsg, "Error");
            appendMsgs(sb, entry.warningMsg, "Warning");
            stripTrailingNewline(sb);
            return sb.toString();
        }
        return null;
    }

    public int getWidgetFlags(Widget widget) {
        PreviewDebugHook.Entry entry = debugHook.getEntry(widget);
        if(entry != null) {
            return (entry.errorMsg.isEmpty() ? 0 : DecoratedText.ERROR) |
                    (entry.warningMsg.isEmpty() ? 0 : DecoratedText.WARNING);
        }
        return 0;
    }

    private void appendMsgs(StringBuilder sb, Collection<String> msgs, String title) {
        if(!msgs.isEmpty()) {
            sb.append(title).append(":\n");
            for(String m : msgs) {
                sb.append(m).append('\n');
            }
        }
    }

    private void stripTrailingNewline(StringBuilder sb) {
        int length = sb.length();
        if(length > 0 && sb.charAt(length-1) == '\n') {
            sb.deleteCharAt(length-1);
        }
    }

    static class ThemeData {
        final Theme themeNode;
        final String name;
        final TreeMap<String, ThemeData> children;
        boolean hasWildcard;

        ThemeData(Theme themeNode) {
            this.themeNode = themeNode;
            this.name = themeNode.getName();
            this.children = new TreeMap<String, ThemeData>();
        }

        void copy(ThemeData src) {
            hasWildcard |= src.hasWildcard;
            children.putAll(src.children);
        }
    }
}
