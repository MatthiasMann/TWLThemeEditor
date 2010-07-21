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

import de.matthiasmann.twl.CallbackWithReason;
import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreeModel extends AbstractTreeTableModel {

    public enum CallbackReason {
        ATTRIBUTE_CHANGED,
        STRUCTURE_CHANGED
    }

    private final Runnable xmlChangedCB;
    private final ThemeFile rootThemeFile;
    private final ThemeTreeRootNode rootNode;
    private ThemeTreeNode curErrorLocation;

    private CallbackWithReason<?>[] callbacks;

    public ThemeTreeModel(MessageLog messageLog, URL url) throws IOException {
        xmlChangedCB = new Runnable() {
            public void run() {
                fireCallbacks(CallbackReason.ATTRIBUTE_CHANGED);
            }
        };
        rootThemeFile = new ThemeFile(messageLog, new TestEnv(), url, xmlChangedCB);
        rootNode = new ThemeTreeRootNode(rootThemeFile, this);

        insertChild(rootNode, 0);
        rootNode.addChildren();
    }

    private static final String COLUMN_HEADER[] = {"Name", "Type"};

    public String getColumnHeaderText(int column) {
        return COLUMN_HEADER[column];
    }

    public int getNumColumns() {
        return COLUMN_HEADER.length;
    }

    public ThemeFile getRootThemeFile() {
        return rootThemeFile;
    }

    public<E extends TreeTableNode> List<E> getTopLevelNodes(Class<E> clazz, TreeTableNode stopAt) {
        TreeTablePath stopAtPath = TreeTablePath.create(stopAt);
        List<E> result = new ArrayList<E>();
        processInclude(rootNode, clazz, result, stopAtPath);
        return result;
    }

    public List<Image> getImages(TreeTableNode stopAt) {
        TreeTablePath stopAtPath = TreeTablePath.create(stopAt);
        ArrayList<Image> result = new ArrayList<Image>();
        outer: for(Images t : getTopLevelNodes(Images.class, null)) {
            for(Image img : t.getChildren(Image.class)) {
                if(stopAtPath != null && stopAtPath.compareTo(img) <= 0) {
                    break outer;
                }
                result.add(img);
            }
        }
        return result;
    }

    public<E extends ThemeTreeNode> E findTopLevelNodes(Class<E> clazz, String name, E exclude) {
        for(E e : getTopLevelNodes(clazz, null)) {
            if(e != exclude && name.equals(e.getName())) {
                return e;
            }
        }
        return null;
    }

    public Image findImage(Kind kind, String name, Image exclude) {
        for(Image img : getImages(null)) {
            if(img != exclude && img.getKind() == kind && name.equals(img.getName())) {
                return img;
            }
        }
        return null;
    }

    public void setErrorLocation(ThemeTreeNode location) {
        if(curErrorLocation != null) {
            curErrorLocation.setError(false);
        }
        curErrorLocation = location;
        if(curErrorLocation != null) {
            curErrorLocation.setError(true);
        }
    }

    private <E extends TreeTableNode> void processInclude(TreeTableNode node, Class<E> clazz, List<E> result, TreeTablePath stopAtPath) {
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            TreeTableNode child = node.getChild(i);
            if(stopAtPath != null && stopAtPath.compareTo(child) <= 0) {
                break;
            }
            if(clazz.isInstance(child)) {
                result.add(clazz.cast(child));
            }
            if(child instanceof Include) {
                processInclude((Include)child, clazz, result, stopAtPath);
            }
        }
    }

    public void handleNodeRenamed(String from, String to, Kind kind) {
        rootNode.handleNodeRenamed(from, to, kind);
    }

    public ThemeTreeNode findNode(String name, Kind kind) {
        return rootNode.findNode(name, kind);
    }

    public void collectNodes(String baseName, Kind kind, Collection<ThemeTreeNode> nodes) {
        rootNode.collectNodes(baseName, kind, nodes);
    }

    public void addCallback(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, CallbackWithReason.class);
    }

    public void removeCallbacks(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    void fireCallbacks(CallbackReason reason) {
        CallbackSupport.fireCallbacks(callbacks, reason);
    }

    @Override
    protected void fireNodesAdded(TreeTableNode parent, int idx, int count) {
        super.fireNodesAdded(parent, idx, count);
        fireCallbacks(CallbackReason.STRUCTURE_CHANGED);
    }

    @Override
    protected void fireNodesRemoved(TreeTableNode parent, int idx, int count) {
        super.fireNodesRemoved(parent, idx, count);
        fireCallbacks(CallbackReason.STRUCTURE_CHANGED);
    }

}
