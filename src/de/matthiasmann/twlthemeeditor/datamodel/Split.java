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

/**
 *
 * @author Matthias Mann
 */
public class Split {

    public static class Point {
        final int pos;
        final boolean edge;

        public Point(int pos, boolean edge) {
            this.pos = pos;
            this.edge = edge;
        }

        public Point(String str) {
            if(str.length() == 0) {
                throw new IllegalArgumentException("empty string");
            }
            switch (str.charAt(0)) {
                case 't':
                case 'T':
                case 'l':
                case 'L':
                    edge = false;
                    str = str.substring(1).trim();
                    break;
                case 'b':
                case 'B':
                case 'r':
                case 'R':
                    edge = true;
                    str = str.substring(1).trim();
                    break;
                default:
                    edge = false;
                    break;
            }
            pos = Integer.parseInt(str);
        }

        public boolean isOtherEdge() {
            return edge;
        }

        public int getPos() {
            return pos;
        }

        public int convertToPX(int size) {
            if(edge) {
                return size - pos;
            } else {
                return pos;
            }
        }

        public Point movePX(int delta) {
            if(edge) {
                return new Point(pos - delta, true);
            } else {
                return new Point(pos + delta, false);
            }
        }

        public Point setOtherEdge(boolean edge, int size) {
            if(this.edge != edge) {
                return new Point(size - pos, edge);
            } else {
                return this;
            }
        }

        public Point setPos(int pos) {
            return new Point(pos, edge);
        }
    }

    public enum Axis {
        HORIZONTAL("LR"),
        VERTICAL("TB");
        
        final String edgeCharacters;
        private Axis(String edgeCharacters) {
            this.edgeCharacters = edgeCharacters;
        }
        char edgeCharacter(boolean edge) {
            return edgeCharacters.charAt(edge ? 1 : 0);
        }
    }

    private final Point split1;
    private final Point split2;

    public Split(Point split1, Point split2) {
        this.split1 = split1;
        this.split2 = split2;
    }

    public Split(String splits) {
        int comma = splits.indexOf(',');
        if(comma < 0) {
            throw new IllegalArgumentException("Need 2 integers");
        }

        this.split1 = new Point(splits.substring(0, comma).trim());
        this.split2 = new Point(splits.substring(comma+1).trim());
    }

    public Point getPoint1() {
        return split1;
    }

    public Point getPoint2() {
        return split2;
    }

    public Point getPoint(int idx) {
        switch (idx) {
            case 0: return split1;
            case 1: return split2;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Split setPoint(int idx, Point point) {
        switch (idx) {
            case 0: return new Split(point, split2);
            case 1: return new Split(split1, point);
            default:
                throw new IndexOutOfBoundsException();
        }
    }
    
    @Override
    public String toString() {
        return toString(Axis.HORIZONTAL);
    }
    
    public String toString(Axis axis) {
        return new StringBuilder()
            .append(axis.edgeCharacter(split1.edge)).append(split1.pos)
            .append(',')
            .append(axis.edgeCharacter(split2.edge)).append(split2.pos)
            .toString();
    }
}
