package com.daniel.visualanalysis.util;

import java.util.ArrayList;

import android.location.Location;

public interface Interpolator {

	/* Constants */
	public static final int STEP_SIZE = 200;
	
	/* Methods */
	public void addPoint(float X_Coord, float Y_Coord);

	public void addLocation(Location loc);

	public void addListOfLocations(ArrayList<Location> list);

	public void addListOfTimeAndDistances(ArrayList<Float> distances, ArrayList<Float> times);

	public boolean isSolved();
	
	public boolean isEmpty();

	public int numberOfPoints();
	
	public void clear();

	/***
	 * Function used to check the validity of the points collected so 
	 * far, and also used to clean up the number of points. Also, can
	 * reduce the number of points to be interpolated to 4, evenly spread.
	 * @param reduce Indicates whether to reduce the number of points to
	 * reduce Runge phenomenon
	 * @return Indicating whether points in list are valid.
	 */
	public boolean cleanPoints(boolean reduce);
	//=====================================================INTERPOLATION========================================

	/***
	 * Function which interpolates the contents of ArrayLists x and y, to produce a list 
	 * of points to be plotted on the screen. 
	 * @param screenWidth Destination width. To know initial and final values.
	 */
	public void interpolate(int screenWidth);

	/***
	 * Function which interpolates the contents of ArrayLists x and y, to produce a list 
	 * of points to be plotted on the screen. To be used with distance, because the method should produce a
	 * list of scaled points, where the first input point will be at the bottom left corner, and the
	 * last one at the top right.
	 * @param screenWidth Destination width. To know initial and final values.
	 * 
	 */
	public void interpolateDistance(int screenWidth, int screenHeight, long timeElapsed, float distanceCovered);

	/***
	 * Function used to evaluate the Interpolating function
	 * @param x-Coordinate at which to evaluate.
	 * @return
	 */
	public float f(float x);
	
	/***
	 * Method to retrieve point to plot on ImageView
	 * @param index of point on line to draw (0<i<STEP_SIZE)
	 * @return Point to draw on ImageView
	 */
	public Point f_visual(int index);

	//=====================================================DIFFERENTIATION=======================================
	/*
	 * THESE FUNCTIONS ARE JUST TO BE USED AFTER AN INTERPOLATION!!!
	 */

	public float derivativeThirdOrder(float X_coord);

	//=====================================================INTEGRATION============================================

	public float integrate(ArrayList<Float> interval);

}
