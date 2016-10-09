/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biofabric.util;

import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/****************************************************************************
**
** Utility for deriving points from other points
*/

public class AffineCombination {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private static final int MAXIMUM = 0;  
  private static final int AVERAGE = 1;      

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Make an affine combination of the points with even weights (e.g. centroid)
  */
  
  public static Point2D combination(Set points, double gridVal) {
    
    int num = points.size();
    if (num == 0) {
      throw new IllegalArgumentException();
    }
    Iterator pint = points.iterator();
    double xSum = 0.0;
    double ySum = 0.0;
    while (pint.hasNext()) {
      Point2D pt = (Point2D)pint.next();
      xSum += pt.getX();
      ySum += pt.getY();
    }
    double newX = xSum / (double)num;
    double newY = ySum / (double)num;
    
    if (gridVal > 0.0) {
      newX = ((double)Math.round(newX / gridVal)) * gridVal;
      newY = ((double)Math.round(newY / gridVal)) * gridVal;
    }

    return (new Point2D.Double(newX, newY));
  }

  /***************************************************************************
  **
  ** Make an affine combination of the points
  */
  
  public static Point2D combination(List points, double[] weights, double gridVal) {
    
    int num = points.size();
    int wNum = weights.length;
    if (wNum != num) {
      throw new IllegalArgumentException();
    }
    double xSum = 0.0;
    double ySum = 0.0;
    double wSum = 0.0;
    for (int i = 0; i < num; i++) {
      Point2D pt = (Point2D)points.get(i);
      double weight = weights[i];
      xSum += pt.getX() * weight;
      ySum += pt.getY() * weight;
      wSum += weight;
    }
    double newX = xSum / wSum;
    double newY = ySum / wSum;
    
    if (gridVal > 0.0) {
      newX = ((double)Math.round(newX / gridVal)) * gridVal;
      newY = ((double)Math.round(newY / gridVal)) * gridVal;
    }

    return (new Point2D.Double(newX, newY));
  } 
  
  /***************************************************************************
  **
  ** Affine coords in a single dimension
  */
  
  public static boolean getWeights(double val1, double val2, double calcForVal, double[] toFill) {
    if (val1 == val2) {
      return (false);
    }
    toFill[0] = (calcForVal - val2) / (val1 - val2);
    toFill[1] = 1.0 - toFill[0];
    return (true);
  }
  
  /***************************************************************************
  **
  ** Express the given point as a set of weights of the array of 3 points.  Returns
  ** false if the three points do not span the space
  */
  
  public static boolean getWeights(Point2D point, Point2D[] pointSet, double[] toFill) {
    if ((pointSet.length != 3) || (toFill.length != 3)) {
      throw new IllegalArgumentException();
    }
    Frame frame;
    try {
      frame = new Frame(pointSet[0], pointSet[1], pointSet[2]);
    } catch (NoninvertibleTransformException nte) {
      return (false);
    }
    frame.coords(point, toFill);
    toFill[2] = toFill[1];
    toFill[1] = toFill[0];
    toFill[0] = 1.0 - toFill[2] - toFill[1];
    return (true);
  }
  
  /***************************************************************************
  **
  ** Given two points, return a right-handed frame that makes the vector between
  ** the two points the positive x axis.  Returns null if points are identical.
  */
  
  public static Frame buildAFrame(Point2D origin, Point2D positiveX) {

    Vector2D posXVec = new Vector2D(origin, positiveX).normalized();
    Vector2D posYVec = posXVec.normal();

    if (posYVec == null) {
      return (null);
    }
    try {
      return (new Frame(origin, posXVec, posYVec));
    } catch (NoninvertibleTransformException nte) {
      return (null);
    }
  }

  /***************************************************************************
  **
  ** Given a pile of points and a single point, return a list of weights that
  ** represent an affine combination to derive the point.  Note that this combination
  ** is not unique unless the point list has only three points.  List must have
  ** at least three points.  If not, or if they do not span the space, or we
  ** have other problems, we return null.
  */
  
  public static List getWeights(List points, Point2D point) {
    
    //
    // Sort the list of points based on distance.  If two points are at
    // the same distance, break the tie using counterclockwise angle from
    // the vector formed by the given point and the farthest point.  If two
    // points are coincident, they will both get the same weight.
    //
    
    Map indexMap = createIndexMap(points);
    // This ditches duplicates:
    ArrayList working = new ArrayList(indexMap.keySet());
    List ordered = orderPoints(working, point);
    if (ordered == null) {
      return (null);
    }

    int size = ordered.size();
    ArrayList[] weights = new ArrayList[size];
    for (int i = 0; i < size; i++) {
      weights[i] = new ArrayList();  
    }
    int[] indices = new int[3];
    double[] toFill = new double[3];
    double sum = 0.0;
    Point2D[] pointSet = new Point2D[3];
    for (int i = 0; i < size; i++) {
      chooseBasisIndices(ordered, i, indices);
      pointSet[0] = (Point2D)ordered.get(indices[0]);
      pointSet[1] = (Point2D)ordered.get(indices[1]);
      pointSet[2] = (Point2D)ordered.get(indices[2]);
      if (getWeights(point, pointSet, toFill)) {
        weights[indices[0]].add(new Double(toFill[0]));
        weights[indices[1]].add(new Double(toFill[1]));
        weights[indices[2]].add(new Double(toFill[2])); 
        sum += (toFill[0] + toFill[1] + toFill[2]);
      }
    } 
    if (sum == 0.0) {
      return (null);
    }    
        
    //
    // Gotta normalize the weights, and distribute evenly between
    // coincident points:
    //
    return (distributeWeights(indexMap, ordered, weights, sum, points.size()));    
  }
  
  /***************************************************************************
  **
  ** Given a pile of old points, a pile of new points, and a single point, position
  ** the single point wrt the new points using a radius/angle approach.
  */
  
  public static Point2D radialPointPosition(Set oldPoints, Point2D oldPoint, Set newPoints) {

    //
    // Get the centroid and radius of the old point collection.  Same with the new
    // collection.  Find the distance/angle of the old point from this centroid, and
    // position the new point with this information.
    //
    
    Point2D oldCentroid = combination(oldPoints, 0.0);
    Point2D newCentroid = combination(newPoints, 0.0);
    double oldRadius = getCloudDistance(oldPoints, oldCentroid, AVERAGE);
    double newRadius = getCloudDistance(newPoints, newCentroid, AVERAGE);
    double oldDistance = oldCentroid.distance(oldPoint);
    double newDistance = (oldRadius != 0.0) ? oldDistance * (newRadius / oldRadius) : oldDistance;

    double angle = 0.0;
    
    if (oldDistance > 0.01) {
      Vector2D xAxis = new Vector2D(1.0, 0.0);
      Vector2D yAxis = new Vector2D(0.0, 1.0);
      try {
        Frame frame = new Frame(oldCentroid, xAxis, yAxis);
        angle = frame.getAngle(oldPoint);
      } catch (NoninvertibleTransformException ntex) {
        throw new IllegalStateException();
      }
    }
    
    //
    // Get new point:
    //
    
    Point2D newPt = generatePointWithPolarCoords(newCentroid, newDistance, angle);
    return (newPt);    
  }  

  /***************************************************************************
  **
  ** Generate a point at the given polar coords.  Angle in radians.
  */
  
  public static Point2D generatePointWithPolarCoords(Point2D origin, double radius, double angle) { 
    double vecX = Math.cos(angle);
    double vecY = Math.sin(angle);
    double originX = origin.getX();
    double originY = origin.getY();
    double newX = originX + (vecX * radius);
    double newY = originY + (vecY * radius);
    return (new Point2D.Double(newX, newY));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class Frame {

    private AffineTransform toStd_;
    private AffineTransform fromStd_;
    private Point2D origin_;
    private Vector2D xAxis_;
    private Vector2D yAxis_;
    private static Frame stdFrame_;

    public Frame() {        
      toStd_ = new AffineTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
      try { 
        fromStd_ = toStd_.createInverse();
      } catch (NoninvertibleTransformException ntex) {
        throw new IllegalStateException();
      }
      origin_ = new Point2D.Double(0.0, 0.0);
      xAxis_ = new Vector2D(1.0, 0.0);
      yAxis_ = new Vector2D(0.0, 1.0);
    }    
        
    public Frame(Point2D point, Vector2D vec1, Vector2D vec2) throws NoninvertibleTransformException {        
      toStd_ = new AffineTransform(vec1.getX(), vec1.getY(), vec2.getX(), vec2.getY(), point.getX(), point.getY());
      fromStd_ = toStd_.createInverse();
      origin_ = (Point2D)point.clone();
      xAxis_ = new Vector2D(vec1);
      yAxis_ = new Vector2D(vec2);
    }
    
    public Frame(Point2D origin, Point2D ptX, Point2D ptY) throws NoninvertibleTransformException {        
      toStd_ = new AffineTransform(ptX.getX() - origin.getX(), ptX.getY() - origin.getY(), 
                                   ptY.getX() - origin.getX(), ptY.getY() - origin.getY(), 
                                   origin.getX(), origin.getY()); 
      fromStd_ = toStd_.createInverse();
      origin_ = (Point2D)origin.clone();
      xAxis_ = new Vector2D(origin, ptX).normalized();
      yAxis_ = new Vector2D(origin, ptY).normalized();      
    } 

    public static Frame getStdFrame() {  // Not thread safe...
      if (stdFrame_ == null) {
        stdFrame_ = new Frame();
      }
      return (stdFrame_);
    }
 
    private double[] coords(Point2D point, double[] toFill) {
      toFill[0] = point.getX();
      toFill[1] = point.getY();
      fromStd_.transform(toFill, 0, toFill, 0, 1);
      return (toFill);
    }
    
    public double getAngle(Point2D point) {
      Vector2D toPoint = new Vector2D(origin_, point).normalized();
      double dotX = toPoint.dot(xAxis_);
      double dotY = toPoint.dot(yAxis_);
      double retval = Math.acos(dotX);
      return ((dotY >= 0.0) ? retval : (2.0 * Math.PI) - retval);    
    }
  
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  private AffineCombination() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the (max, average) distance of a cloud of points wrt the given point.
  */

  private static double getCloudDistance(Set points, Point2D point, int mode) {
    Iterator pit = points.iterator();
    double sum = 0.0;
    double max = Double.NEGATIVE_INFINITY;
    int count = 0;
    while (pit.hasNext()) {
      Point2D pt = (Point2D)pit.next();
      double dist = point.distance(pt);
      sum += dist;
      if (dist > max) {
        max = dist;
      }
      count++;
    }
    return ((mode == AVERAGE) ? sum / ((double)count) : max);
  }
 
  /***************************************************************************
  **
  ** Given a list of points, some of which may be identical, create a map of
  ** point to indices
  */

  private static Map createIndexMap(List points) {
    HashMap retval = new HashMap();
    int size = points.size();
    for (int i = 0; i < size; i++) {
      Point2D point = (Point2D)points.get(i);
      ArrayList indices = (ArrayList)retval.get(point);
      if (indices == null) {
        indices = new ArrayList();
        retval.put(point, indices);
      }
      indices.add(new Integer(i));
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Distribute the weights:
  */

  private static List distributeWeights(Map indexMap, List ordered, 
                                        ArrayList[] weights, double sum, int origSize) {
  
    double[] preRetval = new double[origSize];
    int size = ordered.size();
    for (int i = 0; i < size; i++) {
      Point2D point = (Point2D)ordered.get(i);
      ArrayList weightList = weights[i];
      int numWeights = weightList.size();
      double wSum = 0.0;
      for (int j = 0; j < numWeights; j++) {
        Double weight = (Double)weightList.get(j);
        wSum += weight.doubleValue();
      }
      double fullWeight = wSum / sum;
      List indices = (List)indexMap.get(point);
      int shareSize = indices.size();
      double finalWeight = fullWeight / (double)shareSize;
      for (int j = 0; j < shareSize; j++) {
        Integer index = (Integer)indices.get(j);   
        preRetval[index.intValue()] = finalWeight;
      }
    }

    ArrayList retval = new ArrayList();    
    for (int i = 0; i < origSize; i++) {
      retval.add(new Double(preRetval[i]));
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Given a list of points, order the list of points.  return null if we have
  ** a problem
  */

  private static List orderPoints(List points, Point2D ref) {
    
    ArrayList working = new ArrayList();
    int size = points.size();
    double maxDist = Double.NEGATIVE_INFINITY;
    Point2D maxPoint = null;
    for (int i = 0; i < size; i++) {
      Point2D point = (Point2D)points.get(i);
      double dist = point.distance(ref);
      working.add(new OrderedPoint(point, dist, 0.0));
      if (dist > maxDist) {
        maxPoint = point;
        maxDist = dist;
      }
    }
    
    if ((maxPoint == null) || (maxDist == 0.0)) {
      return (null);
    }
    
    Frame angleFrame = buildAFrame(ref, maxPoint);
    if (angleFrame == null) {
      return (null);
    }
    
    TreeSet sorted = new TreeSet();
    for (int i = 0; i < size; i++) {
      OrderedPoint oPoint = (OrderedPoint)working.get(i);
      oPoint.angle = angleFrame.getAngle(oPoint.point);
      sorted.add(oPoint);
    }
    
    ArrayList retval = new ArrayList();
    Iterator sit = sorted.iterator();
    while (sit.hasNext()) {
      OrderedPoint oPoint = (OrderedPoint)sit.next();
      retval.add(oPoint.point);
    }
    
    return (retval);
  }

  
  /***************************************************************************
  **
  ** Given a list of points, choose three to form a basis, starting with the
  ** specified point.
  */

  private static void chooseBasisIndices(List points, int startIndex, int[] indices) {
    //
    // Go .33 and .66 around the loop
    //
    
    int size = points.size();
    if (size < 3) {
      throw new IllegalArgumentException();
    }
    double oneThird = (double)size / 3.0;
    int oneThirdDelta = (int)Math.round(oneThird);
    int twoThirdDelta = (int)Math.round(2.0 * oneThird);
    
    indices[0] = startIndex;
    indices[1] = (startIndex + oneThirdDelta) % size;
    indices[2] = (startIndex + twoThirdDelta) % size;
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static class OrderedPoint implements Comparable {

    Point2D point;
    double distance;
    double angle;

    public OrderedPoint(Point2D point, double distance, double angle) {
      this.point = point;
      this.distance = distance;
      this.angle = angle;
    }
    
    public int hashCode() {
      return (point.hashCode());
    }

    public String toString() {
      return ("OrderedPoint: " + point + " distance = " + distance + " angle = " + angle);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof OrderedPoint)) {
        return (false);
      }
      return (this.compareTo(other) == 0);
    } 

    public int compareTo(Object o) {
      OrderedPoint other = (OrderedPoint)o;
      if (this.distance != other.distance) {
        return ((this.distance < other.distance) ? -1 : 1);
      }
      if (this.angle != other.angle) {
        return ((this.angle < other.angle) ? -1 : 1);
      }      
      return (0);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {

    double[] weights = new double[3];
    Point2D[] pointSet = new Point2D[3];
    ArrayList points = new ArrayList();
    pointSet[0] = new Point2D.Double(10.0, 40.0);
    points.add(pointSet[0]);
    pointSet[1] = new Point2D.Double(80.0, 50.0);
    points.add(pointSet[1]);    
    pointSet[2] = new Point2D.Double(60.0, 90.0);
    points.add(pointSet[2]);

    Point2D testPt = new Point2D.Double(100.0, 120.0);
//    System.out.println("Original Point = " + testPt);
    AffineCombination.getWeights(testPt, pointSet, weights);
//    System.out.println("Weights = ");
    for (int i = 0; i < 3; i++) {
//      System.out.println(weights[i]);
    }
    Point2D newPoint = AffineCombination.combination(points, weights, 0.0);
//    System.out.println("Derived point = " + newPoint);

    ArrayList pointList = new ArrayList();
    pointList.add(new Point2D.Double(10.0, 40.0));
    pointList.add(new Point2D.Double(80.0, 50.0));    
    pointList.add(new Point2D.Double(60.0, 90.0));    
    pointList.add(new Point2D.Double(40.0, 200.0));    
    pointList.add(new Point2D.Double(70.0, 150.0));    
    pointList.add(new Point2D.Double(60.0, 90.0));    
    Point2D origPoint = new Point2D.Double(100.0, 120.0);    
    
    List weightList = AffineCombination.getWeights(pointList, origPoint);
            
    int numWeights = weightList.size();
    double[] weightArray = new double[numWeights];
    for (int i = 0; i < numWeights; i++) {
      weightArray[i] = ((Double)weightList.get(i)).doubleValue();
//      System.out.println("wei" + weightArray[i]);
    }    
    Point2D newPoint2 = AffineCombination.combination(pointList, weightArray, 0.0);
//    System.out.println("Derived point = " + newPoint2);
    return;
  }  
 
}
