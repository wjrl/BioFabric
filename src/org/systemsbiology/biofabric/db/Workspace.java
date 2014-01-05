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

package org.systemsbiology.biofabric.db;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.FactoryUtilWhiteboard;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.UiUtil;


/****************************************************************************
**
** Definition of Workspace
*/

public class Workspace implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final int DEFAULT_WIDTH_ = 12000;
  private static final int DEFAULT_HEIGHT_ = 9000;
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final double ASPECT_RATIO = (double)DEFAULT_WIDTH_ / (double)DEFAULT_HEIGHT_;    
  public static final int MIN_DIMENSION = 1000;  
  public static final int MAX_DIMENSION = 1000000;    
  
  public static final int PADDING = 100;   
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Rectangle workspace_;
  private boolean needsCenter_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public Workspace() {
    workspace_ = new Rectangle(0, 0, DEFAULT_WIDTH_, DEFAULT_HEIGHT_);
    needsCenter_ = false;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public Workspace(Rectangle rect) {
    workspace_ = (Rectangle)rect.clone();
    needsCenter_ = false;
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public Workspace(Workspace oldSpace, Point2D newCenter) {
    workspace_ = (Rectangle)oldSpace.workspace_.clone();
    setCenter(newCenter);
    needsCenter_ = false;
  }    
  
  /***************************************************************************
  **
  ** Used for IO only
  */
  
  public Workspace(String x, String y, String width, String height) throws IOException {  
    
    int xNum;
    int yNum;
    int wNum;
    int hNum;
    
    try {
      xNum = Integer.parseInt(x);
      yNum = Integer.parseInt(y);
      wNum = Integer.parseInt(width);
      hNum = Integer.parseInt(height);
    } catch (NumberFormatException ex) {
      throw new IOException();
    }
    
    workspace_ = new Rectangle(xNum, yNum, wNum, hNum);
    needsCenter_ = false;
  }      
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      Workspace retval = (Workspace)super.clone();
      retval.workspace_ = (Rectangle)this.workspace_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Set it
  */
  
  public void setWorkspace(int x, int y, int width, int height) {
    workspace_ = new Rectangle(x, y, width, height);
    needsCenter_ = false;
    return;
  } 
  
  /***************************************************************************
  **
  ** Set it
  */
  
  public void setWorkspace(Rectangle rect) {
    workspace_ = (Rectangle)rect.clone();
    needsCenter_ = false;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get it
  */
  
  public Rectangle getWorkspace() {
    return ((Rectangle)workspace_.clone());
  } 
  
  /***************************************************************************
  **
  ** Get height
  */
  
  public int getHeight() {
    return (workspace_.height);
  }
  
  /***************************************************************************
  **
  ** Get width
  */
  
  public int getWidth() {
    return (workspace_.width);
  }
  
  /***************************************************************************
  **
  ** Get center
  */
  
  public Point2D getCenter() {
    double x = workspace_.getX() + (workspace_.getWidth() / 2.0);
    double y = workspace_.getY() + (workspace_.getHeight() / 2.0);    
    return (new Point2D.Double(x, y));
  }  
  
  /***************************************************************************
  **
  ** Get origin
  */
  
  public Point2D getOrigin() {
    return (new Point2D.Double(workspace_.getX(), workspace_.getY()));
  }    

  /***************************************************************************
  **
  ** Answer if we need a center
  */
  
  public boolean needsCenter() {
    return (needsCenter_);
  } 
  
  /***************************************************************************
  **
  ** Set if we need a center
  */
  
  public void setNeedsCenter(boolean needsCenter) {
    needsCenter_ = needsCenter;
    return;
  }   

  /***************************************************************************
  **
  ** Set the origin
  */
  
  public void setOrigin(Point2D origin) {
    workspace_.x = (int)Math.round(origin.getX());
    workspace_.y = (int)Math.round(origin.getY());
    needsCenter_ = false;
    return;
  }   
  
  /***************************************************************************
  **
  ** Set the center
  */
  
  public void setCenter(Point2D center) {
    workspace_.x = (int)Math.round(center.getX() - (workspace_.getWidth() / 2.0));
    workspace_.y = (int)Math.round(center.getY() - (workspace_.getHeight() / 2.0));
    needsCenter_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we contain the given rectangle
  */
  
  public boolean contains(Rectangle rect) {
    if ((rect.x < workspace_.x) || (rect.y < workspace_.y)) {
      return (false);
    }
    if (rect.getMaxX() > workspace_.getMaxX()) {
      return (false);
    } 
    if (rect.getMaxY() > workspace_.getMaxY()) {
      return (false);
    }     
    
    return (true);
  }
 
  /***************************************************************************
  **
  ** Write the workspace definition to XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<workspace x=\"");
    out.print(workspace_.x);
    out.print("\" y=\"");
    out.print(workspace_.y);  
    out.print("\" w=\"");
    out.print(workspace_.width);  
    out.print("\" h=\"");
    out.print(workspace_.height);    
    out.println("\" />");
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get a workspace that fits model bounds
  */
  
  public static Workspace setToModelBounds(Rectangle modelSize) {        
    FixedAspectDim modelFad = calcBoundedFit(modelSize, PADDING, ASPECT_RATIO);
    Point2D centerPt = getAllModelCenter(modelSize);
    int x = (int)Math.round(centerPt.getX() - ((double)modelFad.getWidth() / 2.0));
    int y = (int)Math.round(centerPt.getY() - ((double)modelFad.getHeight() / 2.0));    
    return (new Workspace(new Rectangle(x, y, modelFad.getWidth(), modelFad.getHeight())));
  }
  
  /***************************************************************************
  **
  ** Get the all model center
  */
  
  public static Point2D getAllModelCenter(Rectangle allModelSize) {
    double x = allModelSize.getX() + (allModelSize.getWidth() / 2.0);
    double y = allModelSize.getY() + (allModelSize.getHeight() / 2.0);    
    return (new Point2D.Double(x, y));
  }  

  /***************************************************************************
  **
  ** Figure out the bounded allowed size to fit the model.
  */
  
  public static FixedAspectDim calcBoundedFit(Rectangle modelSize, int padding, double fixedAspect) {
    
    //
    // Figure out acceptable min/max bounds for fixed aspect ratio:
    
    Dimension minDim;
    Dimension maxDim;
    if (fixedAspect >= 1.0) {  // wide...
      int minHeight = MIN_DIMENSION;
      int minWidth = (int)Math.round((double)MIN_DIMENSION * fixedAspect);
      minDim = new Dimension(minWidth, minHeight);
      int maxWidth = MAX_DIMENSION;
      int maxHeight = (int)Math.round((double)MAX_DIMENSION / fixedAspect);
      // For huge/tiny aspect ratios, these may be inconsistent.  Break the max if necessary:
      // since fixedAspect >= 1.0, only maxHeight can be too small:
      if (maxHeight < MIN_DIMENSION) {
        maxHeight = MIN_DIMENSION;
        maxWidth = (int)Math.round((double)MIN_DIMENSION * fixedAspect);
      }
      maxDim = new Dimension(maxWidth, maxHeight);
    } else { // tall;
      int minWidth = MIN_DIMENSION;
      int minHeight = (int)Math.round((double)MIN_DIMENSION / fixedAspect);
      minDim = new Dimension(minWidth, minHeight);      
      int maxHeight = MAX_DIMENSION;
      int maxWidth = (int)Math.round((double)MAX_DIMENSION * fixedAspect);
      // For huge/tiny aspect ratios, these may be inconsistent.  Break the max if necessary:
      // since fixedAspect < 1.0, only maxWidth can be too small:
      if (maxWidth < MIN_DIMENSION) {
        maxWidth = MIN_DIMENSION;
        maxHeight = (int)Math.round((double)MIN_DIMENSION / fixedAspect);
      }
      maxDim = new Dimension(maxWidth, maxHeight);      
    }
       
    int paddedWidth = (int)UiUtil.forceToGridValueMax((double)(modelSize.width + padding), UiUtil.GRID_SIZE);
    int paddedHeight = (int)UiUtil.forceToGridValueMax((double)(modelSize.height + padding), UiUtil.GRID_SIZE);
    double modelAspect = (double)paddedWidth / (double)paddedHeight;
    FixedAspectDim retval = new FixedAspectDim(new Dimension(paddedWidth, paddedHeight), fixedAspect);    
 
    if (modelAspect < fixedAspect) {  // model is taller than desired
      retval.changeHeight(paddedHeight, true);  // true, because orig aspect is wrong   
    } else { // if (modelAspect >= fixedAspect)
      retval.changeWidth(paddedWidth, true);
    }
    
    if ((retval.getHeight() < minDim.height) || (retval.getWidth() < minDim.width))  {
      return (new FixedAspectDim(minDim, fixedAspect));
    } else if ((retval.getHeight() > maxDim.height) || (retval.getWidth() > maxDim.width)) {
      return (new FixedAspectDim(maxDim, fixedAspect));
    } else {
      return (retval);
    }
  }     
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class WorkspaceWorker extends AbstractFactoryClient {
    
    public WorkspaceWorker(FactoryUtilWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("workspace");
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("workspace")) {
        FactoryUtilWhiteboard board = (FactoryUtilWhiteboard)this.sharedWhiteboard_;
        board.workspace = buildFromXML(elemName, attrs);
        retval = board.workspace;
      }
      return (retval);     
    }
    
    private Workspace buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String x = AttributeExtractor.extractAttribute(elemName, attrs, "workspace", "x", true);
      String y = AttributeExtractor.extractAttribute(elemName, attrs, "workspace", "y", true);      
      String width = AttributeExtractor.extractAttribute(elemName, attrs, "workspace", "w", true);      
      String height = AttributeExtractor.extractAttribute(elemName, attrs, "workspace", "h", true);      

      return (new Workspace(x, y, width, height));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Fixed Aspect ratio dimension
  ** 
  */
  
  public static class FixedAspectDim {
    private Dimension dim_;
    private double aspect_; 

    public FixedAspectDim(Dimension dim, double aspect) {    
      dim_ = (Dimension)dim.clone();
      aspect_ = aspect;
    }
    
    public FixedAspectDim(FixedAspectDim other) {
      this.dim_ = (Dimension)other.dim_.clone();
      this.aspect_ = other.aspect_;
    }
    
    public void mergeAll(FixedAspectDim other) {
      this.dim_ = (Dimension)other.dim_.clone();
      this.aspect_ = other.aspect_;
    }    
    
    public int getHeight() {
      return (dim_.height);
    }
    
    public int getWidth() {
      return (dim_.width);
    } 
    
    public double getAspect() {
      return (aspect_);
    }    
    
    public boolean heightChanged(FixedAspectDim old) {
      return (dim_.height != old.dim_.height);
    }
    
    public boolean widthChanged(FixedAspectDim old) {
      return (dim_.width != old.dim_.width);
    }    

    public void mergeHeight(FixedAspectDim src) {
      dim_.height = src.dim_.height;      
      return;      
    }
    
    public void mergeWidth(FixedAspectDim src) {
      dim_.width = src.dim_.width;
      return;      
    } 
    
    public void changeHeight(int newHeight, boolean force) {    
      if ((dim_.height != newHeight) || force) {
        dim_.height = newHeight;
        dim_.width = (int)Math.round((double)dim_.height * aspect_);
      }
      return;
    }
    
    public void changeWidth(int newWidth, boolean force) {        
      if ((dim_.width != newWidth) || force) {
        dim_.width = newWidth;
        dim_.height = (int)Math.round((double)dim_.width / aspect_);
      }
      return;
    }    
  }    

}
