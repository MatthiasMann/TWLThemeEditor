/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreeModel extends AbstractTreeTableModel implements ModifyableTreeTableNode {

    private final ThemeFile rootThemeFile;

    public ThemeTreeModel(ThemeFile rootThemeFile) throws IOException {
        this.rootThemeFile = rootThemeFile;

        rootThemeFile.addChildren(this);
    }

    private static final String COLUMN_HEADER[] = {"Name", "Type"};

    public String getColumnHeaderText(int column) {
        return COLUMN_HEADER[column];
    }

    public int getNumColumns() {
        return COLUMN_HEADER.length;
    }

    public void appendChild(TreeTableNode ttn) {
        insertChild(ttn, getNumChildren());
    }

    public void setLeaf(boolean leaf) {
    }

    public ThemeFile getRootThemeFile() {
        return rootThemeFile;
    }

    public List<Textures> getTextures() {
        List<Textures> result = new ArrayList<Textures>();
        processInclude(this, Textures.class, result);
        return result;
    }

    private <E extends TreeTableNode> void processInclude(TreeTableNode node, Class<E> clazz, List<E> result) {
        Utils.getChildren(node, clazz, result);
        for(Include include : Utils.getChildren(node, Include.class)) {
            processInclude(include, clazz, result);
        }
    }
    
    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz) {
        return Utils.getChildren(this, clazz);
    }
}
