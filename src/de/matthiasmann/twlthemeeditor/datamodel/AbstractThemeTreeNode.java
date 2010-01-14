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

import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.DeleteNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.MoveNodeOperations;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class AbstractThemeTreeNode extends AbstractTreeTableNode implements ThemeTreeNode {

    protected boolean error;

    protected AbstractThemeTreeNode(TreeTableNode parent) {
        super(parent);
    }

    public ThemeTreeModel getThemeTreeModel() {
        return (ThemeTreeModel)getTreeTableModel();
    }
    
    @Override
    public void insertChild(TreeTableNode ttn, int idx) {
        super.insertChild(ttn, idx);
    }

    public void removeChild(TreeTableNode ttn) {
        int childIndex = super.getChildIndex(ttn);
        if(childIndex >= 0) {
            super.removeChild(childIndex);
        }
    }

    @Override
    public void setLeaf(boolean leaf) {
        super.setLeaf(leaf);
    }

    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz) {
        return Utils.getChildren(this, clazz);
    }

    public void setError(boolean hasError) {
        this.error = hasError;
    }

    protected static List<ThemeTreeOperation> getDefaultOperations(Element element, ThemeTreeNode node) {
        List<ThemeTreeOperation> result = new ArrayList<ThemeTreeOperation>();
        result.add(new DeleteNodeOperation(element, node));
        result.add(new MoveNodeOperations("opMoveNodeUp", element, node, -1));
        result.add(new MoveNodeOperations("opMoveNodeDown", element, node, +1));
        return result;
    }

    public Object getData(int column) {
        switch (column) {
            case 0: {
                String displayName = getDisplayName();
                return error ? new NodeNameWithError(displayName) : displayName;
            }
            case 1:
                return getType();
            default:
                return "";
        }
    }

    public String getDisplayName() {
        String name = getName();
        if(name == null && (getParent() instanceof NameGenerator)) {
            name = ((NameGenerator)getParent()).generateName(this);
        }
        if(name == null) {
            name = "Unnamed #" + (1+getParent().getChildIndex(this));
        }
        return name;
    }

    public abstract String getName();

    protected abstract String getType();
}
