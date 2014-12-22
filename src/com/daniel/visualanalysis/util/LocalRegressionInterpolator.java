package com.daniel.visualanalysis.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import android.location.Location;
import android.util.Log;

public class LocalRegressionInterpolator implements Interpolator {

	public ArrayList<Point> InterPol;
	public ArrayList<Float> y;
	public ArrayList<Float> x;
	public int numberOfPoints;
	public boolean USES_LOCATION;
	public boolean SOLVED=false;
	private PolynomialSplineFunction result;
	
	public LocalRegressionInterpolator(boolean location) {
		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		numberOfPoints = 0;
		USES_LOCATION = location;
	}

	@Override
	public void addPoint(float X_Coord, float Y_Coord) {
		x.add(X_Coord);
		y.add(Y_Coord);
		numberOfPoints++;
		Log.v("PolynomialInterpolator","added point:("+Float.toString(X_Coord)+", "+Float.toString(Y_Coord)+")");
	}

	@Override
	public void addLocation(Location loc) {
		x.add((float) loc.getLongitude());
		y.add((float) loc.getLatitude());
		numberOfPoints++;
	}

	@Override
	public void addListOfLocations(ArrayList<Location> list) {
		for(int i=0;i<list.size();i++){
			this.addLocation(list.get(i));
		}
	}

	@Override
	public void addListOfTimeAndDistances(ArrayList<Float> distances,
			ArrayList<Float> times) {
		x = times;
		y = distances;
		numberOfPoints=x.size();
	}

	@Override
	public boolean isSolved(){
		return SOLVED;
	}
	
	@Override
	public boolean isEmpty() {
		return (x.isEmpty() || y.isEmpty());
	}
	
	@Override
	public int numberOfPoints(){
		return numberOfPoints;
	}

	@Override
	public void clear() {
		x.clear();
		y.clear();
		numberOfPoints = 0;
		InterPol.clear();
	}

	@Override
	public boolean cleanPoints(boolean reduce) {
		int size = x.size();
		if (x.isEmpty())
			return false;
		if (size != y.size())
			return false;
		if (size == 1)
			return false;
		if (!reduce)
			return true;
		if (size < 5)
			return true;
		// Actual point reduction
		int reduction;
		ArrayList<Float> tempX = new ArrayList<Float>();
		ArrayList<Float> tempY = new ArrayList<Float>();
		tempX.add(x.get(0));
		tempY.add(y.get(0));
		System.out.printf("Initial size = %d%n", size);
			reduction = ((size-2)/3);
			for (int i = 1; i < size-1; i++){
				if (i%reduction==0){
					tempX.add(x.get(i));
					tempY.add(y.get(i));
				}
			}
			size = tempX.size();
		System.out.printf("Final size = %d%n", size+1);
		tempX.add(x.get(numberOfPoints-1));
		tempY.add(y.get(numberOfPoints-1));
		x = tempX;
		y = tempY;
		numberOfPoints = x.size();
		return true;
	}

	@Override
	public void interpolate(int screenWidth) {
		double[] xpoints = new double[x.size()];
		double[] ypoints = new double[x.size()];
		for(int i=0;i<x.size();i++){
			xpoints[i] = x.get(i).doubleValue();
			ypoints[i] = y.get(i).doubleValue();
		}
		Arrays.sort(xpoints);
		Arrays.sort(ypoints);
		LoessInterpolator interpolator = new LoessInterpolator();
		result = interpolator.interpolate(xpoints, ypoints);
		Float[] xSoln = new Float[STEP_SIZE];
		Float[] ySoln = new Float[STEP_SIZE];
		for (int i = 0; i < STEP_SIZE; i++){
			xSoln[i] =  (i*((float)screenWidth / STEP_SIZE));
			ySoln[i] = (float) result.value(xSoln[i].doubleValue());
		}
		InterPol = new ArrayList<Point>();
		for (int j = 0; j < STEP_SIZE; j++){
			InterPol.add(new Point(xSoln[j],ySoln[j]));			
		}
		SOLVED = true;
	}

	@Override
	public void interpolateDistance(int screenWidth, int screenHeight,
			long timeElapsed, float distanceCovered) {
		Log.d("SplineInterpolator","time elapsed = "+Long.toString(timeElapsed));
		
		// Converting the points to double[] and sorting (not really necessary)
		double[] xpoints = new double[numberOfPoints];
		double[] ypoints = new double[numberOfPoints];
		for(int i=0;i<x.size();i++){
			xpoints[i] = x.get(i).doubleValue();
			ypoints[i] = y.get(i).doubleValue();
		}
//		Arrays.sort(xpoints);
//		Arrays.sort(ypoints);
		
		if (!cleanPoints(false)){ // With cubic splines we don't need to clean points
			Log.e("SplineInterpolator", "Dirty points, can't interpolate");
			return;
		}
		
		// Generating the interpolating spline polynomial
		LoessInterpolator interpolator = new LoessInterpolator();
		result = interpolator.interpolate(xpoints, ypoints);
		
		// Selecting 200 sample points and Solving the polynomial for sample points
		double[] xTemp = new double[STEP_SIZE];
		float[] xSoln = new float[STEP_SIZE];
		float[] ySoln = new float[STEP_SIZE];
		for (int i = 0; i < STEP_SIZE; i++){
			xTemp[i] = (i*(timeElapsed/ STEP_SIZE));
			xSoln[i] = (float) xTemp[i];
			ySoln[i] = (float) result.value(xTemp[i]);
		}

		
		// Scaling to fit screen
		float width = (float) (screenWidth * 1.0);
		float height = (float) (screenHeight * 1.0); 
		float time = (float) timeElapsed;
		float widthFactor = width/time;
		float heightFactor = height/distanceCovered;
		
		for (int k = 0; k < STEP_SIZE; k++){
			xSoln[k] = xSoln[k]*widthFactor;
			ySoln[k] = height - ySoln[k]*heightFactor;
		}
		
		ArrayList<Point> polynomial = new ArrayList<Point>();
		for (int j = 0; j < STEP_SIZE; j++){
			polynomial.add(new Point(xSoln[j],ySoln[j]));			
		}
		InterPol = polynomial;
		SOLVED=true;

	}

	@Override
	public float f(float x) {
		if(!SOLVED)
			return 0;
		return (float) result.value(x);
	}
	
	@Override
	public Point f_visual(int index){
		return InterPol.get(index);
	}

	@Override
	public float derivativeThirdOrder(float X_coord) {
		float step = 1/((float)STEP_SIZE);
		float y0 = f(X_coord-step);
		float y1 = f(X_coord+step);
		return (y1-y0)/(2*step);
	}

	@Override
	public float integrate(ArrayList<Float> interval) {
		float a = interval.get(0);
		float b = interval.get(1);
		float n = (b-a)/((float)STEP_SIZE);
		float sum = 0;
		for (int k=0;k<STEP_SIZE;k++){
			sum+=f(a+(n*k));
		}
		return sum*(n/2);
	}

}
