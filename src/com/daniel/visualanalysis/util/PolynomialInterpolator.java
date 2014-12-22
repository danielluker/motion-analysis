package com.daniel.visualanalysis.util;

import java.util.ArrayList;

import android.location.Location;
import android.util.Log;

/***
 * Class to implement Lagrange Interpolation
 * 
 * @author daniel
 *
 */
public class PolynomialInterpolator implements Interpolator{
	
	/* Instance Variables */
	public static final int STEP_SIZE = 200;
	
	public ArrayList<Float> y;
	public ArrayList<Float> x;
	public boolean USES_LOCATION;
	
	private boolean SOLVED=false;
	private ArrayList<Point> InterPol;
	private int numberOfPoints;
	private float[] NewtonCoefficients;
	private Float[] BasePoints;
	
	/***
	 *  Constructor 
	 *  @param location if this is interpolation with distance
	 */
	public PolynomialInterpolator(boolean location){
		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		numberOfPoints = 0;
		USES_LOCATION = location;
	}
	
	/* Methods */
	public void addPoint(float X_Coord, float Y_Coord) {
		x.add(X_Coord);
		y.add(Y_Coord);
		numberOfPoints++;
		Log.v("PolynomialInterpolator","added point:("+Float.toString(X_Coord)+", "+Float.toString(Y_Coord)+")");
	}
	
	public void addLocation(Location loc){
		x.add((float) loc.getLongitude());
		y.add((float) loc.getLatitude());
		numberOfPoints++;
	}
	
	public void addListOfLocations(ArrayList<Location> list){
		for(int i=0;i<list.size();i++){
			this.addLocation(list.get(i));
		}
	}
	
	public void addListOfTimeAndDistances(ArrayList<Float> distances, ArrayList<Float> times){
		x = times;
		y = distances;
		numberOfPoints = x.size();
	}
	
	@Override
	public int numberOfPoints(){
		return numberOfPoints;
	}
	
	@Override
	public boolean isSolved(){
		return SOLVED;
	}
	
	public boolean isEmpty() {
		if (x.isEmpty() && numberOfPoints == 0)
			return true;
		return false;
	}
	
	public void clear() {
		x.clear();
		y.clear();
		numberOfPoints = 0;
	}
	
	public boolean cleanPoints(boolean reduce){ 
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
	
//=====================================================INTERPOLATION========================================
	
	public void interpolate(int screenWidth) {
		float width = (float) (screenWidth * 1.0);
		float[] c = newtonDiff(x,y,numberOfPoints);
		NewtonCoefficients = c;
		float[] xSoln = new float[STEP_SIZE];
		for (int i = 0; i < STEP_SIZE; i++)
			xSoln[i] = i*(width / STEP_SIZE);
		Float[] xTemp = x.toArray(new Float[x.size()]); 
		float[] ySoln = solve(c,xSoln,xTemp);
		BasePoints = xTemp;
		ArrayList<Point> polynomial = new ArrayList<Point>();
		for (int j = 0; j < STEP_SIZE; j++){
			polynomial.add(new Point(xSoln[j],ySoln[j]));			
		}
		SOLVED = true;
		InterPol = polynomial;
	}
	
	public void interpolateDistance(int screenWidth, int screenHeight, long timeElapsed, float distanceCovered) {
		Log.d("PolynomialInterpolator","time elapsed = "+Long.toString(timeElapsed));
		
		if (!cleanPoints(true)){
			Log.e("PolynomialInterpolator", "Dirty points, can't interpolate");
			return;
		}
		
		// Finding coefficients
		float[] c = newtonDiff(x,y,numberOfPoints);
		NewtonCoefficients = c;
		
		// Selecting 100 sample points
		float[] xSoln = new float[STEP_SIZE];
		for (int i = 0; i < STEP_SIZE; i++)
			xSoln[i] = (float) (i*(timeElapsed/ STEP_SIZE));
		
		// Solving the polynomial for sample points
		Float[] xTemp = x.toArray(new Float[x.size()]); 
		float[] ySoln = solve(c,xSoln,xTemp); 
		BasePoints = xTemp;
		
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

	public void outputPolynomial(){
		String s;
		for(int i=0;i<InterPol.size();i++){
			s = "Point "+i+": ("+Float.toString(InterPol.get(i).x)+", "+Float.toString(InterPol.get(i).y)+")";
			Log.d("PolynomialInterpolator", s);
		}
	}
	
	public void outputPoints(){
		String s;
		for(int i=0;i<x.size();i++){
			s = "Data Point "+i+": ("+Float.toString(x.get(i))+", "+Float.toString(y.get(i))+")";
			Log.d("PolynomialInterpolatorDATA", s);
		}
	}
	
	private float[] newtonDiff(ArrayList<Float> xCoords, ArrayList<Float> yCoords, int n){
		float[][] v = new float[numberOfPoints][numberOfPoints];
		for (int i = 0; i < n; i++){
			v[i][0] = yCoords.get(i);
		}
		for (int j = 1; j < n; j++){
			for (int k = 0; k < (n-j); k++){
				v[k][j] = (v[k+1][j-1] - v[k][j-1])/(xCoords.get(j+k)-xCoords.get(k));
			}
		}
		for (int a = 0; a < numberOfPoints;a++){
			for (int b = 0; b < numberOfPoints;b++){
			}
		}
		float[] result = new float[numberOfPoints];
		for (int l = 0; l < n; l++){
			result[l] = v[0][l];
		}
		return result;
	}
	
	private float[] solve(float[] coefficients, float[] xPts, Float[] xTemp){
		float[] yPts = new float[xPts.length];
		for (int i = 0; i < xPts.length; i++){
			yPts[i] = nest(coefficients.length-1,coefficients,xPts[i],xTemp);
		}
		return yPts;
	}
	
	/***
	 * Evaluation of nested polynomial using Horner's method
	 * @param d Degree of polynomial
	 * @param c Array of d+1 coefficients
	 * @param x X-coordinate at which to evaluate
	 * @param xTemp Array of d base points
	 * @return
	 */
	private float nest(int d,float[] c,float x,Float[] xTemp){
		float y = c[d];
		for (int i = d-1; i >= 0; i--)
			y = y*(x - xTemp[i]) + c[i];
		return y;
	}
	
	public float f(float x){
		if (!SOLVED){
			Log.e("PolnomialInterpolator","Attempted to solve function before interpolation");
			return 0;
		}
		int d = NewtonCoefficients.length - 1;
		float y = NewtonCoefficients[d];
		for (int i=d-1;i>=0;i--)
			y = y*(x-BasePoints[i])+NewtonCoefficients[i];
		return y;
	}
	
	@Override
	public Point f_visual(int index){
		return InterPol.get(index);
	}
	
//=====================================================DIFFERENTIATION=======================================
	/*
	 * THESE FUNCTIONS ARE JUST TO BE USED AFTER AN INTERPOLATION!!!
	 */
	
	public float derivativeThirdOrder(float X_coord){
		float step = 1/((float)STEP_SIZE);
		float y0 = f(X_coord-step);
		float y1 = f(X_coord+step);
		return (y1-y0)/(2*step);
	}
	
//=====================================================INTEGRATION============================================
	
	public float integrate(ArrayList<Float> interval){
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