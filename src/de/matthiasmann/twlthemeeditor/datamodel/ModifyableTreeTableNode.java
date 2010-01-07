/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.TreeTableNode;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public interface ModifyableTreeTableNode extends TreeTableNode {

    public void appendChild(TreeTableNode ttn);

    public void setLeaf(boolean leaf);

    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz);
    
}
