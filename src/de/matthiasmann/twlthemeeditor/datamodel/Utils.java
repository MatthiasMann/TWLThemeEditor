/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Border;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author MannMat
 */
final class Utils {

    private static final Pattern intListPattern = Pattern.compile("^(\\d+)(?:,(\\d+))*$");

    public static int[] parseInts(String value) {
        Matcher m = intListPattern.matcher(value);
        if(m.matches()) {
            int[] result = new int[m.groupCount()-1];
            for(int i=1 ; i<m.groupCount() ; i++) {
                result[i-1] = Integer.parseInt(m.group(i));
            }
            return result;
        }
        return null;
    }
    
    public static Border parseBorder(String value) {
        if(value == null) {
            return null;
        }
        int[] values = parseInts(value);
        if(values == null) {
            return null;
        }
        switch (values.length) {
            case 1:
                return new Border(values[0]);
            case 2:
                return new Border(values[0], values[1]);
            case 4:
                return new Border(values[0], values[1], values[2], values[3]);
            default:
                return null;
        }
    }

    public static String toString(Border border) {
        if(border == null) {
            return null;
        }
        if(border.getBorderTop() == border.getBorderBottom() && border.getBorderLeft() == border.getBorderRight()) {
            if(border.getBorderTop() == border.getBorderLeft()) {
                if(border.getBorderTop() == 0) {
                    return null;
                }
                return Integer.toString(border.getBorderTop());
            }
            return border.getBorderLeft()+","+border.getBorderTop();
        }
        return border.getBorderTop()+","+border.getBorderLeft()+","+border.getBorderBottom()+","+border.getBorderRight();
    }
}
