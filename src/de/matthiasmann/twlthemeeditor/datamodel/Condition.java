/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

/**
 *
 * @author MannMat
 */
public final class Condition {

    public static enum Type {
        IF,
        UNLESS,
        NONE
    }
    
    private final Type type;
    private final String condition;

    public static final Condition NONE = new Condition(Type.NONE, "");
    
    public Condition(Type type, String condition) {
        this.type = type;
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public Type getType() {
        return type;
    }
    
}
