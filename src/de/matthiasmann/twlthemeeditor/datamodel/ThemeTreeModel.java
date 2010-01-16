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

import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile.CallbackReason;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreeModel extends AbstractTreeTableModel {

    private final ThemeFile rootThemeFile;
    private final ThemeTreeRootNode rootNode;
    private ThemeTreeNode curErrorLocation;

    public ThemeTreeModel(TestEnv env, URL url) throws IOException {
        rootThemeFile = new ThemeFile(env, url);
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

    public<E extends TreeTableNode> List<E> getTopLevelNodes(Class<E> clazz) {
        List<E> result = new ArrayList<E>();
        processInclude(rootNode, clazz, result);
        return result;
    }

    public List<Image> getImages() {
        ArrayList<Image> result = new ArrayList<Image>();
        for(Textures t : getTopLevelNodes(Textures.class)) {
            result.addAll(t.getChildren(Image.class));
        }
        return result;
    }

    public<E extends ThemeTreeNode> E findTopLevelNodes(Class<E> clazz, String name, E exclude) {
        for(E e : getTopLevelNodes(clazz)) {
            if(e != exclude && name.equals(e.getName())) {
                return e;
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

    private <E extends TreeTableNode> void processInclude(TreeTableNode node, Class<E> clazz, List<E> result) {
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            TreeTableNode child = node.getChild(i);
            if(child instanceof Include) {
                processInclude((Include)child, clazz, result);
            } else if(clazz.isInstance(child)) {
                result.add(clazz.cast(child));
            }
        }
    }

    public List<ThemeTreeOperation> getOperations() {
        return Collections.<ThemeTreeOperation>emptyList();
    }

    public void handleNodeRenamed(String from, String to, Kind kind) {
        rootNode.handleNodeRenamed(from, to, kind);
    }

    void fireCallbacks(CallbackReason reason) {
        rootThemeFile.fireCallbacks(reason);
    }

    private void structureModified() {
        fireCallbacks(CallbackReason.STRUCTURE_CHANGED);
    }

    @Override
    protected void fireNodesAdded(TreeTableNode parent, int idx, int count) {
        super.fireNodesAdded(parent, idx, count);
        structureModified();
    }

    @Override
    protected void fireNodesChanged(TreeTableNode parent, int idx, int count) {
        super.fireNodesChanged(parent, idx, count);
    }

    @Override
    protected void fireNodesRemoved(TreeTableNode parent, int idx, int count) {
        super.fireNodesRemoved(parent, idx, count);
        structureModified();
    }

}
