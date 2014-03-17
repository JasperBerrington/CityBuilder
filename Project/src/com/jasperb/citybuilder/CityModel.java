/**
 * 
 */
package com.jasperb.citybuilder;

import java.util.Arrays;

import android.util.Log;

import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.Constant.OBJECTS;
import com.jasperb.citybuilder.util.Constant.TERRAIN;
import com.jasperb.citybuilder.util.Constant.TERRAIN_MODS;

/**
 * @author Jasper
 * 
 */
public class CityModel {
    /**
     * String used for identifying this class.
     */
    public static final String TAG = "CityModel";

    private int mWidth, mHeight;
    private byte[][] mTerrainMap;
    private byte[][] mTerrainModMap;
    private boolean[][] mTerrainBlend;
    private short[][] mObjectMap;
    private boolean[] mUsedObjectIDs;
    private ObjectSlice mObjectList = null;
    private short mNumObjects = 0;

    @SuppressWarnings("unused")
    private CityModel() {}// Prevent constructing without a width and height

    public CityModel(int width, int height) {
        Log.v(TAG, "Create City: " + width + "x" + height);
        mWidth = width;
        mHeight = height;

        // Draw thread processes by column, so keep data in a single column close in memory
        mTerrainMap = new byte[mWidth][mHeight];
        mObjectMap = new short[mWidth][mHeight];
        mUsedObjectIDs = new boolean[Constant.OBJECT_LIMIT];
        Arrays.fill(mUsedObjectIDs, false);

        // Try to be clever with memory by keeping all of the terrain mods close in memory
        mTerrainModMap = new byte[mWidth][mHeight * Constant.MAX_NUMBER_OF_TERRAIN_MODS];
        mTerrainBlend = new boolean[mWidth][mHeight];

        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                mTerrainMap[col][row] = TERRAIN.GRASS;
                mTerrainMap[col][row] = (byte) (Math.random() * (TERRAIN.count + 3));
                if (mTerrainMap[col][row] >= TERRAIN.count) {
                    if (row == 0) {
                        mTerrainMap[col][row] = (byte) (Math.random() * TERRAIN.count);
                    } else {
                        mTerrainMap[col][row] = mTerrainMap[col][row - 1];
                    }
                }
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
                mTerrainBlend[col][row] = true;
                mObjectMap[col][row] = OBJECTS.NONE;
            }
        }
        for (int col = 0; col < mWidth; col++) {
            for (int row = 0; row < mHeight; row++) {
                determineTerrainDecorations(row, col);
                determineTerrainMods(row, col);
            }
        }
//        for (int col = 0; col < mWidth; col += OBJECTS.objectNumColumns[OBJECTS.TEST2X4]) {
//            for (int row = 0; row < mHeight; row += OBJECTS.objectNumRows[OBJECTS.TEST2X4]) {
//                addObject(row, col, OBJECTS.TEST2X4);
//            }
//        }
        addObject(0, 0, OBJECTS.TEST2X4);
    }

    /**
     * @return width of the model in tiles
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * @return height of the model in tiles
     */
    public int getHeight() {
        return mHeight;
    }
    
    /**
     * Gets the type of TERRAIN located at the specified row and column.
     * 
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @return the type of tile at the specified location
     */
    public byte getTerrain(int row, int col) {
        return mTerrainMap[col][row];
    }

    /**
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @param index
     *            the mod index
     * @return the type of mod at the specified location and mod index
     */
    public byte getMod(int row, int col, int index) {
        return mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + index];
    }

    /**
     * Gets the id of object located at the specified row and column.
     * 
     * @param row
     *            the row of the tile
     * @param col
     *            the column of the tile
     * @return the id of building at the specified location
     */
    public short getObjectID(int row, int col) {
        return mObjectMap[col][row];
    }
    
    public ObjectSlice getObjectList() {
        return mObjectList;
    }

    /**
     * @return the number of objects in the city model.
     */
    public short getNumberOfObjects() {
        return mNumObjects;
    }

    /**
     * Fill a rectangular region as one terrain type.
     * 
     * @param startRow
     *            minimum row defining the region
     * @param startCol
     *            minimum column defining the region
     * @param endRow
     *            maximum row defining the region
     * @param endCol
     *            maximum column defining the region
     * @param terrain
     *            type of terrain to fill the region with
     * @param blend
     *            true if the terrain at these tiles should use blending mods
     */
    public void setTerrain(int startRow, int startCol, int endRow, int endCol, int terrain, boolean blend) {
        for (int col = startCol; col <= endCol; col++) {
            for (int row = startRow; row <= endRow; row++) {
                mTerrainMap[col][row] = (byte) terrain;
                mTerrainBlend[col][row] = blend;
                determineTerrainDecorations(row, col);
            }
        }
        for (int col = Math.max(startCol - 1, 0); col <= Math.min(endCol + 1, mWidth - 1); col++)
            for (int row = Math.max(startRow - 1, 0); row <= Math.min(endRow + 1, mHeight - 1); row++)
                determineTerrainMods(row, col);
    }

    /**
     * Set a tile as one terrain type.
     * 
     * @param row
     * @param col
     * @param terrain
     * @param blend
     *            true if the terrain at this tile should use blending mods
     */
    public void setTerrain(int row, int col, int terrain, boolean blend) {
        mTerrainMap[col][row] = (byte) terrain;
        mTerrainBlend[col][row] = blend;
        determineTerrainDecorations(row, col);
        for (int c = Math.max(col - 1, 0); c <= Math.min(col + 1, mWidth - 1); c++) {
            for (int r = Math.max(row - 1, 0); r <= Math.min(row + 1, mHeight - 1); r++) {
                determineTerrainMods(r, c);
            }
        }
    }

    /**
     * Determine the terrain mods to be used for a specified tile based off the surrounding 8 tiles
     * 
     * @param row
     * @param col
     * @param overwriteDecorations
     *            if true terrain decorations are overwritten
     */
    private void determineTerrainMods(int row, int col) {
        int terrain = mTerrainMap[col][row];
        int modIndex = 0;
        int blendTerrain;

        if (TERRAIN_MODS.isTerrainDecoration(mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex]))
            modIndex++;

        if (mTerrainBlend[col][row]) {
            if (TERRAIN_MODS.supportsStandardRounding(terrain)) {
                if (col - 1 >= 0) {
                    blendTerrain = TERRAIN.getBaseType(mTerrainMap[col - 1][row]);
                    //Check that the surrounding terrain can be used for blending, and that blending is even needed (not same base type)
                    if (TERRAIN_MODS.hasStandardRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        //Only blend the top left corner if all 3 tiles touching the corner are of the same base type
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col - 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_LEFT);
                            modIndex++;
                        }
                        //Only blend the bottom left corner if all 3 tiles touching the corner are of the same base type
                        if (row + 1 < mHeight && TERRAIN.getBaseType(mTerrainMap[col - 1][row + 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row + 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.BOTTOM_LEFT);
                            modIndex++;
                        }
                    }
                }
                if (col + 1 < mWidth) {
                    blendTerrain = TERRAIN.getBaseType(mTerrainMap[col + 1][row]);
                    //Check that the surrounding terrain can be used for blending, and that blending is even needed (not same base type)
                    if (TERRAIN_MODS.hasStandardRoundingMods(blendTerrain) && blendTerrain != TERRAIN.getBaseType(terrain)) {
                        //Only blend the top right corner if all 3 tiles touching the corner are of the same base type
                        if (row - 1 >= 0 && TERRAIN.getBaseType(mTerrainMap[col + 1][row - 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row - 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.TOP_RIGHT);
                            modIndex++;
                        }
                        //Only blend the bottom right corner if all 3 tiles touching the corner are of the same base type
                        if (row + 1 < mHeight && TERRAIN.getBaseType(mTerrainMap[col + 1][row + 1]) == blendTerrain
                                && TERRAIN.getBaseType(mTerrainMap[col][row + 1]) == blendTerrain) {
                            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] =
                                    (byte) (TERRAIN_MODS.getRoundedType(blendTerrain) + TERRAIN_MODS.BOTTOM_RIGHT);
                            modIndex++;
                        }
                    }
                }
            }
        }

        //Paved line must always have a mod to indicate that it is a paved line        
        if (terrain == TERRAIN.PAVED_LINE) {
            if (setPavedLineMod(row, col, modIndex)) {
                modIndex++;
            } else {//if there is no adjacent paved lines to connect to, just draw a generic one
                //If the limit on terrain mods is exceeded, just drop all of them in favour of the paved line
                if (modIndex == Constant.MAX_NUMBER_OF_TERRAIN_MODS) {
                    modIndex = 0;
                }
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE;
                modIndex++;
            }
        }

        if (modIndex < Constant.MAX_NUMBER_OF_TERRAIN_MODS)
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.NONE;
        else
            assert (modIndex == Constant.MAX_NUMBER_OF_TERRAIN_MODS);
    }

    /**
     * Determine which paved line mods make sense based off adjacent terrain paved line tiles.
     * Note: this method does not supply a default paved line mod if there are no adjacent paved line tiles.
     * 
     * @param row
     * @param col
     * @param modIndex
     *            the current number of mods already assigned to the specified tile
     * @return true if at least one adjacent paved line tile was found
     */
    private boolean setPavedLineMod(int row, int col, int modIndex) {
        if (col - 1 >= 0 && mTerrainMap[col - 1][row] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && col + 1 < mWidth) {
//                if (row - 1 >= 0 && mTerrainMap[col + 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.TOP_RIGHT;
//                    return true;
//                } else if (row + 1 < mHeight && mTerrainMap[col + 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.BOTTOM_RIGHT;
//                    return true;
//                }
//            }
            //Test for the two L shaped scenarios 
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_LEFT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_LEFT;
            } else {//Not L-shaped, so default to straight
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                        + TERRAIN_MODS.HORIZONTAL;
            }
        } else if (col + 1 < mWidth && mTerrainMap[col + 1][row] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && col - 1 >= 0) {
//                if (row - 1 >= 0 && mTerrainMap[col - 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.TOP_LEFT;
//                    return true;
//                } else if (row + 1 < mHeight && mTerrainMap[col - 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.HORIZONTAL * 4 + TERRAIN_MODS.BOTTOM_LEFT;
//                    return true;
//                }
//            }
            //Test for the two L shaped scenarios 
            if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.TOP_RIGHT;
            } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.ROUNDED_PAVED_LINE
                        + TERRAIN_MODS.BOTTOM_RIGHT;
            } else {//Not L-shaped, so default to straight
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                        + TERRAIN_MODS.HORIZONTAL;
            }
        } else if (row - 1 >= 0 && mTerrainMap[col][row - 1] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && row + 1 < mHeight) {
//                if (col - 1 >= 0 && mTerrainMap[col - 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.BOTTOM_LEFT;
//                    return true;
//                } else if (col + 1 < mWidth && mTerrainMap[col + 1][row + 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.BOTTOM_RIGHT;
//                    return true;
//                }
//            }
            //L-shaped scenario would have been caught prior, so default to straight 
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                    + TERRAIN_MODS.VERTICAL;
        } else if (row + 1 < mHeight && mTerrainMap[col][row + 1] == TERRAIN.PAVED_LINE) {
//            if (mBlend[col][row] && row - 1 >= 0) {
//                if (col - 1 >= 0 && mTerrainMap[col - 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.TOP_LEFT;
//                    return true;
//                } else if (col + 1 < mWidth && mTerrainMap[col + 1][row - 1] == TERRAIN.PAVED_LINE) {
//                    mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.SMOOTHED_PAVED_LINE
//                            + TERRAIN_MODS.VERTICAL * 4 + TERRAIN_MODS.TOP_RIGHT;
//                    return true;
//                }
//            }
            //L-shaped scenario would have been caught prior, so default to straight 
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS + modIndex] = TERRAIN_MODS.STRAIGHT_PAVED_LINE
                    + TERRAIN_MODS.VERTICAL;
        } else {
            return false;
        }
        return true;
    }

    private void determineTerrainDecorations(int row, int col) {
        if (mTerrainMap[col][row] == TERRAIN.GRASS) {
            int rand = (int) ((Math.random() * TERRAIN_MODS.GRASS_DECORATION_COUNT) * TERRAIN_MODS.GRASS_DECORATION_CHANCE);
            if (rand < TERRAIN_MODS.GRASS_DECORATION_COUNT) {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = (byte) (TERRAIN_MODS.GRASS_DECORATION + rand);
            } else {
                mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
            }
        } else {
            mTerrainModMap[col][row * Constant.MAX_NUMBER_OF_TERRAIN_MODS] = TERRAIN_MODS.NONE;
        }
    }

    public boolean addObject(int row, int col, int type) {
        int id = createObject(row, col, type);
        if (id != -1) {
            for (int r = row; r < row + OBJECTS.objectNumRows[type]; r++) {
                for (int c = col; c < col + OBJECTS.objectNumColumns[type]; c++) {
                    mObjectMap[c][r] = (short) id;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private int createObject(int row, int col, int type) {
        if (mNumObjects == Constant.OBJECT_LIMIT)
            return -1;
        int i = 0;
        for (; i <= mNumObjects; i++) {
            if (!mUsedObjectIDs[i]) {
                mUsedObjectIDs[i] = true;
                mNumObjects++;
                break;
            }
        }

        if (i == mNumObjects + 1) {
            throw new IllegalStateException("There should be at least one free Object ID"); //This shouldn't be possible
        } else {
            int sliceColumns = OBJECTS.getSliceWidth(type) / (Constant.TILE_WIDTH / 2);
            int lastColumn = col + OBJECTS.objectNumColumns[type] - 1;
            int sliceCount = OBJECTS.getSliceCount(type);
            Log.d(TAG, "OBJ SLICE: " + sliceCount);
            int sliceCol = col;
            int sliceRow = row + OBJECTS.objectNumRows[type] - 1;
            byte sliceIndex = 0;
            ObjectSlice currentSlice = mObjectList;
            while (currentSlice != null) {
                if (currentSlice.col > sliceCol || (currentSlice.col == col && currentSlice.row > row)) {
                    currentSlice = addObjectSlice(currentSlice, new ObjectSlice((short)sliceRow, (short)sliceCol, (short)i, (byte)type, sliceIndex));
                    sliceIndex++;
                    if(sliceIndex == sliceCount) {
                        return i;
                    } else {
                        if(sliceCol == lastColumn) {
                            break;
                        } else {
                            sliceCol += sliceColumns;
                            if(sliceCol > lastColumn) {
                                sliceCol = lastColumn;
                            }
                        }
                    }
                }
                currentSlice = currentSlice.next;
            }
            while(sliceIndex < sliceCount) {
                Log.d(TAG, "APPEND SLICE: " + sliceCount);
                currentSlice = addObjectSlice(currentSlice, new ObjectSlice((short)sliceRow, (short)sliceCol, (short)i, (byte)type, sliceIndex));
                sliceIndex++;
                if(sliceCol != lastColumn) {
                    sliceCol += sliceColumns;
                    if(sliceCol > lastColumn) {
                        sliceCol = lastColumn;
                    }
                }
            }
            
            return i;
        }
    }

    private ObjectSlice addObjectSlice(ObjectSlice node, ObjectSlice newSlice) {
        if (node == null) {
            mObjectList = newSlice;
        } else {
            newSlice.next = node.next;
            node.next = newSlice;
        }
        return newSlice;
    }

    public class ObjectSlice {
        public short row, col;
        public short id;
        public byte type;
        public byte sliceIndex;
        public ObjectSlice next = null;

        public ObjectSlice(short row, short col, short id, byte type, byte sliceIndex) {
            this.row = row;
            this.col = col;
            this.type = type;
            this.sliceIndex = sliceIndex;
        }
        
        public void log(String tag) {
            Log.d(tag,id + ": [R" + row + ",C" + col + "] = "+ type + "[" + sliceIndex + "]");
        }
    }
}
