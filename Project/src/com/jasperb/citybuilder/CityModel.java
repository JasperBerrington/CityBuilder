/**
 * 
 */
package com.jasperb.citybuilder;

import com.jasperb.citybuilder.util.Constant.TERRAIN;

import android.util.Log;

/**
 * @author Jasper
 * 
 */
public class CityModel {
    /**
     * Identifier string for debug messages originating from this class
     */
    public static final String TAG = "CityModel";

    private int mWidth, mHeight;
    private TERRAIN[][] mTerrainMap;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @SuppressWarnings("unused")
    private CityModel() {}

    public CityModel(int width, int height) {
        Log.d(TAG, "Create City: " + width + "x" + height);
        mWidth = width;
        mHeight = height;

        mTerrainMap = new TERRAIN[mHeight][mWidth];
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                if ((i + j) % 2 == 0 || i % 2 == 0) {
                    mTerrainMap[i][j] = TERRAIN.GRASS;
                } else {
                    mTerrainMap[i][j] = TERRAIN.DIRT;
                }
            }
        }
    }

    /**
     * Gets the type of TERRAIN located at the specified row and column
     * 
     * @param row
     * @param col
     * @return the type of tile at the specified location
     */
    public TERRAIN getTerrain(int row, int col) {
        return mTerrainMap[row][col];
    }
}
