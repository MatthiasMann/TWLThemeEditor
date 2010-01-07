/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TreeTableNode;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public abstract class ThemeTreeNode extends AbstractTreeTableNode implements ModifyableTreeTableNode {

    protected ThemeTreeNode(TreeTableNode parent) {
        super(parent);
    }

    public ThemeTreeModel getThemeTreeModel() {
        return (ThemeTreeModel)getTreeTableModel();
    }
    
    public void appendChild(TreeTableNode ttn) {
        insertChild(ttn, getNumChildren());
    }

    @Override
    public void setLeaf(boolean leaf) {
        super.setLeaf(leaf);
    }

    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz) {
        return Utils.getChildren(this, clazz);
    }

}
