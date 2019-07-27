/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.cache;

import java.util.Arrays;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.N;

/**
 * <br/>
 * Array a = new Object[x][y]; y is a big number. The format is <br/>
 * [1][][][][][][][][][][][][][]...[y] <br/>
 * . [][][][][][][][][][][][][]...[y] <br/>
 * . [][][][][][][][][][][][][]...[y] <br/>
 * . [][][][][][][][][][][][][]...[y] <br/>
 * [x][][][][][][][][][][][][][]...[y]
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class DataGrid<E> {
    private static final Logger logger = LoggerFactory.getLogger(DataGrid.class);

    private static final Object NULL_MASK = NullMask.INSTANCE;
    private static final int INIT_INDEX = -1;
    private static final int PIECE_BITS = 10;
    private static final int PIECE_SIZE = 1 << PIECE_BITS;
    private static final int BIT_INDEX_MASK = PIECE_SIZE - 1;

    private final boolean isBigY;
    private int x;
    private final int y;
    private Object[][] xyArray;
    private int[] pieceIndex;

    public DataGrid(int x, int y) {
        if ((x < 0) || (y < 0)) {
            throw new IllegalArgumentException("both x and y must be bigger than 0");
        }

        this.isBigY = y > PIECE_SIZE;

        this.x = x;
        this.y = y;

        if (isBigY) {
            xyArray = new Object[x][0];
            pieceIndex = new int[((y % PIECE_SIZE) == 0) ? (y / PIECE_SIZE) : ((y / PIECE_SIZE) + 1)];
            Arrays.fill(pieceIndex, INIT_INDEX);
        } else {
            xyArray = new Object[x][y];

            for (int i = 0; i < x; i++) {
                Arrays.fill(xyArray[i], NULL_MASK);
            }

            pieceIndex = new int[] { 0 };
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void put(int x, int y, E e) {
        y = initY(y);

        xyArray[x][y] = e;
    }

    private int initY(int y) {
        if (isBigY) {
            int indexOfPiece = y >> PIECE_BITS;

            if (pieceIndex[indexOfPiece] == INIT_INDEX) {
                int len = xyArray[0].length;
                pieceIndex[indexOfPiece] = len;
                int newLen = len + PIECE_SIZE;

                for (int i = 0; i < x; i++) {
                    xyArray[i] = Arrays.copyOf(xyArray[i], newLen);
                    Arrays.fill(xyArray[i], len, newLen, NULL_MASK);
                }
            }

            return pieceIndex[indexOfPiece] + (y & BIT_INDEX_MASK);
        } else {
            return y;
        }
    }

    @SuppressWarnings("unchecked")
    public E get(int x, int y) {
        y = indexOfY(y);

        if ((y == INIT_INDEX) || (xyArray[x][y] == NULL_MASK)) {
            return null;
        } else {
            return (E) xyArray[x][y];
        }
    }

    private int indexOfY(int y) {
        if (isBigY) {
            int index = pieceIndex[y >> PIECE_BITS];

            return (index == INIT_INDEX) ? INIT_INDEX : (index + (y & BIT_INDEX_MASK));
        } else {
            return y;
        }
    }

    public void clear(int x, int y) {
        y = indexOfY(y);

        if (y != INIT_INDEX) {
            xyArray[x][y] = NULL_MASK;
        }
    }

    public boolean isClean(int x, int y) {
        y = indexOfY(y);

        return (y == INIT_INDEX) || (xyArray[x][y] == NULL_MASK);
    }

    public void clearX(int x) {
        if (xyArray[x].length > 0) {
            Arrays.fill(xyArray[x], NULL_MASK);
        }
    }

    public boolean isXFull(int x, int fromY, int toY) {
        for (int y = 0, i = fromY; i < toY; i++) {
            y = indexOfY(i);

            if ((y == INIT_INDEX) || (xyArray[x][y] == NULL_MASK)) {
                return false;
            }
        }

        return true;
    }

    public Object[] getX(int x, int fromY, int toY) {
        Object[] objects = new Object[toY - fromY];

        for (int y = 0, i = fromY; i < toY; i++) {
            y = indexOfY(i);

            if ((y != INIT_INDEX) && (xyArray[x][y] != NULL_MASK)) {
                objects[i - fromY] = xyArray[x][y];
            }
        }

        return objects;
    }

    public void clear() {
        for (int i = 0; i < x; i++) {
            Arrays.fill(xyArray[i], NULL_MASK);
        }
    }

    public void extendX(int newX) {
        if (x >= newX) {
            return;
        }

        xyArray = Arrays.copyOf(xyArray, newX);

        for (int i = x, len = xyArray[0].length; i < newX; i++) {
            xyArray[i] = new Object[len];

            Arrays.fill(xyArray[i], NULL_MASK);
        }

        x = newX;
    }

    public void zip() {
        if ((xyArray.length == 0) || (xyArray[0].length == 0)) {

            if (logger.isInfoEnabled()) {
                logger.info("XY Array is empty");
            }

            return;
        }

        // Runtime.getRuntime().gc();

        int hitNum = 0;
        Object[] hashArray = null;

        try {
            hashArray = new Object[xyArray[0].length];
        } catch (Exception e) {

            if (logger.isWarnEnabled()) {
                logger.warn("Failed to create object array with length: " + xyArray[0].length);
            }

            return;
        }

        final int BIT_INDEX = hashArray.length - 1;

        Object value = null;
        int index = -1;
        int hashCode = -1;

        for (int i = 0; i < x; i++) {
            for (int j = 0, len = xyArray[0].length; j < len; j++) {
                value = xyArray[i][j];

                if (value != null) {
                    if (value instanceof Boolean || value instanceof Byte || value instanceof Character) {
                        break;
                    }

                    hashCode = value.hashCode();

                    index = BIT_INDEX & hashCode;

                    if (hashArray[index] == null) {
                        hashArray[index] = value;
                    } else if (value.equals(hashArray[index])) {
                        xyArray[i][j] = hashArray[index];

                        hitNum++;
                    } else {
                        hashArray[index] = value;
                    }
                }
            }
        }

        if (logger.isWarnEnabled()) {
            logger.warn(hitNum + " objects have been zipped.");
        }

        // Runtime.getRuntime().gc();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DataGrid && N.deepEquals(((DataGrid<E>) obj).xyArray, xyArray));
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(xyArray);
    }

    @Override
    public String toString() {
        return Arrays.deepToString(xyArray);
    }

    private static class NullMask implements java.io.Serializable {
        private static final long serialVersionUID = 6343178353389511572L;

        private static final NullMask INSTANCE = new NullMask();

        private NullMask() {
        }

        @Override
        public int hashCode() {
            return 17;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public String toString() {
            return NullMask.class.getCanonicalName();
        }
    }
}
