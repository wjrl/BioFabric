/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.cmd;

import java.awt.Point;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biofabric.util.ExceptionHandler;

/****************************************************************************
**
** Handles zoom operations
*/

public class ZoomCommandSupport {
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ZoomTarget sup_;
  private JScrollPane jsp_;
  private double[] zoomVals_ = new double[] {0.06, 0.12, 0.20, 0.25, 0.33, 0.38, 
                                             0.44, 0.50, 0.62, 0.67, 0.75, 0.85, 1.0, 
                                             1.25, 1.5, 2.0};
  private int currZoomIndex_ = 5; //0.38
  private static final double SMALL_MODEL_CUSTOM_ZOOM_ = 0.03;
  private double customZoom_ = SMALL_MODEL_CUSTOM_ZOOM_;
  
  private static final int NEW_MODEL_INDEX_ = 8;  // 0.62
  private int newModelIndex_ = NEW_MODEL_INDEX_;
  private Dimension currViewSize_;
  private double currViewXFrac_;
  private double currViewYFrac_;
  private ZoomChangeTracker tracker_;
  private Rectangle2D currClipRect_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ZoomCommandSupport(ZoomChangeTracker tracker) {
    tracker_ = tracker;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Return the current clip rect
  */

  public Rectangle2D getCurrClipRect() {
    return (currClipRect_);
  }

  /***************************************************************************
  **
  ** Set Custom zoom vals (do only at the start)
  */

  public void setCustomZoomPoints(double[] customZoomVals, int startIndex, int newModelIndex) {
    zoomVals_ = customZoomVals;
    currZoomIndex_ = startIndex;
    newModelIndex_ = newModelIndex;
    return;
  }
  
  /***************************************************************************
  **
  ** Register the scroll pane
  */

  public void registerScrollPaneAndZoomTarget(JScrollPane jsp, ZoomTarget sup) {
    jsp_ = jsp;
    sup_ = sup;
    //
    // Need to make view adjustments when the scroll pane size changes:
    //
    jsp.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        try {
          handlePanelResize();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    //
    // Always keep track of where the view center is.  When we resize, we
    // maintain that center:
    //    
    ChangeListener barChange = new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        try {
          trackScrollBars();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    };
    
    //
    // Having scroll bars come and go makes the actual view extent
    // change without any window size change: this happens with
    // zooming, causing bug #16. Track when it comes and goes:
    
    HierarchyListener hChange = new HierarchyListener() {
      public void hierarchyChanged(HierarchyEvent e) {       
        try {
          if ((e.getID() == HierarchyEvent.HIERARCHY_CHANGED) && 
              ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0x00)) {
            checkBarVisible();
          }
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    };
        
    JScrollBar vsb = jsp.getVerticalScrollBar();
    vsb.getModel().addChangeListener(barChange);
    vsb.addHierarchyListener(hChange);
    JScrollBar hsb = jsp.getHorizontalScrollBar();     
    hsb.getModel().addChangeListener(barChange); 
    hsb.addHierarchyListener(hChange);
    return;
  }
  
  /***************************************************************************
  **
  ** Figure out what zoom value to use for a given model
  */ 

  private ZoomResult calcOptimalZoom(Rectangle chosen) {
    Dimension allDim = jsp_.getViewport().getExtentSize();
    
    Dimension dim;
    if (chosen == null) {
      dim = sup_.getBasicSize();
    } else {
      dim = new Dimension(chosen.width, chosen.height);
    }
    
    int vIndex = -1;
    int hIndex = -1;
    for (int i = 0; i < zoomVals_.length; i++) {
      double zoom = zoomVals_[i];
      double modelHeight = dim.height * zoom;
      if (modelHeight <= allDim.height) {
        vIndex++;
      } else {
        break;
      }
    }
    for (int i = 0; i < zoomVals_.length; i++) {
      double zoom = zoomVals_[i];
      double modelWidth = dim.width * zoom;
      if (modelWidth <= allDim.width) {
        hIndex++;
      } else {
        break;
      }
    } 
    int retval = (vIndex < hIndex) ? vIndex : hIndex;
    if (retval == -1) {
      double vZoom = (double)allDim.height / (double)dim.height;
      double hZoom = (double)allDim.width / (double)dim.width;
      return (new ZoomResult((vZoom > hZoom) ? hZoom : vZoom));
    }
   
    retval = (retval > zoomVals_.length - 1) ? zoomVals_.length - 1 : retval;
    return (new ZoomResult(retval));
  }  

  
  /***************************************************************************
  **
  ** do the zoom operations
  */ 

  private void doZoom(double newZoomVal, double oldZoomVal) { 
     
    Dimension vDim = scrollDims();

    JViewport view = jsp_.getViewport();
    Point viewPos = view.getViewPosition();
    Dimension viewDim = view.getExtentSize();

    int vCenter = viewPos.y + (viewDim.height / 2);
    int hCenter = viewPos.x + (viewDim.width / 2);    
    Point2D worldCenter = sup_.viewToWorld(new Point(hCenter, vCenter));    
    sup_.setWideZoomFactor(newZoomVal, vDim);    
    Point newCenter = sup_.pointToViewport(new Point((int)Math.round(worldCenter.getX()), 
                                                     (int)Math.round(worldCenter.getY()))); 
    viewportUpdate(newCenter, vDim);    
    return;
  }

  /***************************************************************************
  **
  ** Calc scroll dims
  */ 
    
  private Dimension scrollDims() {
    // Use this instead??
    // Dimension vDim = jsp.getViewport().getExtentSize();
    JScrollBar vsb = jsp_.getVerticalScrollBar();
    JScrollBar hsb = jsp_.getHorizontalScrollBar();      
    int vAmt = vsb.getVisibleAmount();
    int hAmt = hsb.getVisibleAmount();    
    return (new Dimension(hAmt, vAmt));
  }   
  
  
  /***************************************************************************
  **
  ** Common viewport operations, when we are interested in looking at
  ** the center of the workspace
  */ 
    
  private void viewportToCenter(double zoomVal) {
    Dimension vDim = scrollDims();
    sup_.setWideZoomFactor(zoomVal, vDim);    
    Point pt = sup_.getCenterPoint();
    viewportUpdate(pt, vDim);
    return;
  }

  
  /***************************************************************************
  **
  ** Calculate bounded viewport position (i.e. upper left corner)
  */ 
    
  private Point boundedViewPos(Point center, Dimension vDim, JViewport view) {
    int newV = center.y - (vDim.height / 2);
    int newH = center.x - (vDim.width / 2);
    return (doBounding(newH, newV, view));    
  }
  
  /***************************************************************************
  **
  ** Bound viewport position
  */ 
    
  private Point doBounding(int newH, int newV, JViewport view) {
    //
    // Don't let the value go outide actual dimensions.  Note that with wide views, the
    // preferred size is smaller than the actual view:
    //    
    Dimension allDim = view.getViewSize();
    Dimension viewDim = view.getExtentSize();
    int delH = (allDim.width - viewDim.width);
    int delV = (allDim.height - viewDim.height);
    if (newH < 0) {
      newH = 0;
    } else if ((delH > 0) && (newH > delH)) {
      newH = delH;
    }    
    if (newV < 0) {
      newV = 0;
    } else if ((delV > 0) && (newV > delV)) {
      newV = delV;
    }  
    return (new Point(newH, newV));
  }  

  /***************************************************************************
  **
  ** Common viewport update ops
  */ 
    
  private void viewportUpdate(Point center, Dimension vDim) {
    JViewport view = jsp_.getViewport(); 
    view.setViewSize(sup_.getPreferredSize());
    view.setViewPosition(boundedViewPos(center, vDim, view));
    view.invalidate();
    jsp_.validate();
    return;
  }  

  /***************************************************************************
  **
  ** Get current zoom value
  */ 
    
  private double getCurrentZoom() {
    return ((currZoomIndex_ == -1) ? customZoom_ : zoomVals_[currZoomIndex_]);
  }
 
  /***************************************************************************
  **
  ** Get current zoom value
  */ 
    
  public void bumpZoomWrapper(char direction) {  
    double oldZoomVal = getCurrentZoom();
    bumpZoom(direction);
    double newZoomVal = getCurrentZoom();

    //
    // Enable/disable zoom actions based on zoom limits:
    //
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    doZoom(newZoomVal, oldZoomVal);
    return;
  }
  
  /***************************************************************************
  **
  ** Get current zoom value
  */ 
    
  public void setZoomIndex(int index) {  
    double oldZoomVal = getCurrentZoom();
    setCurrentZoom(new ZoomResult(index));
    double newZoomVal = getCurrentZoom();

    //
    // Enable/disable zoom actions based on zoom limits:
    //
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    doZoom(newZoomVal, oldZoomVal);
    return;
  }
  
  /***************************************************************************
  **
  ** Bump zoom value
  */ 
    
  private void bumpZoom(char direction) {
    // Sanity check:
    if ((currZoomIndex_ < -1) || (currZoomIndex_ >= zoomVals_.length)) {
      return;
    }
    if (direction == '+') {
      if (zoomIsWide()) {
        setCurrentZoom(new ZoomResult(0));
      } else if (zoomIsMax()) {  // Safety net; should not happen
        return;
      } else {
        setCurrentZoom(new ZoomResult(currZoomIndex_ + 1));
      }
    } else {
      if (zoomIsWide()) {  // Safety net; should not happen
        return;
      } else if (currZoomIndex_ == 0) {
        ZoomResult zr = calcOptimalZoom(null);
        //
        // If within bounds, just show full workspace.  If that is smaller than the
        // lowest set zoom, do the small model value:
        //
        if (!zr.doCustom) {
          Dimension vDim = scrollDims();
          double workspaceZoom = sup_.getWorkspaceZoom(vDim, 0.95);
          if (workspaceZoom >= zoomVals_[0]) {
            workspaceZoom = SMALL_MODEL_CUSTOM_ZOOM_;
          }
          setCurrentZoom(new ZoomResult(workspaceZoom));
        } else {
          setCurrentZoom(zr);
        }
      } else {
        setCurrentZoom(new ZoomResult(currZoomIndex_ - 1));
      }      
    }
  }    

  /***************************************************************************
  **
  ** Answer if we are going wide
  */ 
    
  public boolean zoomIsWide() {
    return (currZoomIndex_ == -1);
  }  
  
  /***************************************************************************
  **
  ** May not want to allow a "wide" zoom:
  */ 
    
  public boolean zoomIsFirstDefined() {
    return (currZoomIndex_ == 0);
  }  
  
  /***************************************************************************
  **
  ** Answer if we are max
  */ 
    
  public boolean zoomIsMax() {
    return (currZoomIndex_ == (zoomVals_.length - 1));
  }    

  /***************************************************************************
  **
  ** Set current zoom value for new model
  */ 
    
  public void setCurrentZoomForNewModel() {
    setCurrentZoom(new ZoomResult(newModelIndex_));
    sup_.fixCenterPoint(true, null, false);
    viewportToCenter(getCurrentZoom());
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }  
 
  /***************************************************************************
  **
  ** Set current zoom value
  */ 
    
  private void setCurrentZoom(ZoomResult zres) {
    /*
    int delay = 100;
    Timer zoomTimer = new Timer(delay, new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
      }
    });
    zoomTimer.start();
    */
     
    if (zres.doCustom) {
      customZoom_ = zres.customZoom;
      currZoomIndex_ = -1;
    } else {
      currZoomIndex_ = zres.index;
      customZoom_ = SMALL_MODEL_CUSTOM_ZOOM_;  // Doesn't really matter...
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Zoom to the center of the worksheet
  */ 
    
  public void zoomToWorksheetCenter() {
    ZoomResult zres = calcOptimalZoom(null);    
    setCurrentZoom(zres);    
    viewportToCenter(getCurrentZoom());
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }
 
  
  /***************************************************************************
  **
  ** Zoom to the center of the worksheet, showing full worksheet
  */ 
    
  public void zoomToFullWorksheet() {
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    ZoomResult zres = calcOptimalZoom(wsBounds);    
    setCurrentZoom(zres);    
    viewportToCenter(getCurrentZoom());
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }  
  
  /***************************************************************************
  **
  ** Zoom to rectangle operations
  */ 
    
  public void zoomToRectangle(Rectangle zoomRect) {
    zoomToSelectedGuts(zoomRect);
    return;
  }  
 
  /***************************************************************************
  **
  ** Zoom to selected operations
  */ 
    
  public void zoomToSelected() {
    Rectangle selected = sup_.getSelectedBounds();
    zoomToSelectedGuts(selected);
    return;
  }  
  
  /***************************************************************************
  **
  ** Center on rectangle operations
  */ 
    
  public void centerOnRectangle(Rectangle centerRect) {
    centerOnSelectedGuts(centerRect);
    return;
  }  
 
  /***************************************************************************
  **
  ** Center on selected operations
  */ 
    
  public void centerOnSelected() {
    Rectangle selected = sup_.getSelectedBounds();
    centerOnSelectedGuts(selected);
    return;
  }  
  
  /***************************************************************************
  **
  ** Zoom to selected operations
  */ 
    
  private void zoomToSelectedGuts(Rectangle selected) {
    if (selected == null) {
      zoomToModel();
      return;
    }
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    Rectangle union = wsBounds.union(selected);
    if (!union.equals(wsBounds)) { // i.e. selected is outside of union
      Point2D cent = sup_.getRawCenterPoint();
      selected = centeredUnion(union, cent);
    }
      
    Dimension vDim = scrollDims();
    ZoomResult zres = calcOptimalZoom(selected);  
    setCurrentZoom(zres);
    sup_.setWideZoomFactor(getCurrentZoom(), vDim);
 
    int x = selected.x + (selected.width / 2);
    int y = selected.y + (selected.height / 2);    
    Point pt = new Point(x, y);
    pt = sup_.pointToViewport(pt);
    viewportUpdate(pt, vDim);
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }  
   
  /***************************************************************************
  **
  ** Center on selected operations
  */ 
    
  private void centerOnSelectedGuts(Rectangle selected) {
    if (selected == null) {
      zoomToModel();
      return;
    }
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    Rectangle union = wsBounds.union(selected);
    if (!union.equals(wsBounds)) { // i.e. selected is outside of union
      Point2D cent = sup_.getRawCenterPoint();
      selected = centeredUnion(union, cent);
    }
      
    Dimension vDim = scrollDims();
    //ZoomResult zres = calcOptimalZoom(selected);    
    //setCurrentZoom(zres);
    //sup_.setWideZoomFactor(getCurrentZoom(), vDim);
 
    int x = selected.x + (selected.width / 2);
    int y = selected.y + (selected.height / 2);    
    Point pt = new Point(x, y);
    pt = sup_.pointToViewport(pt);
    viewportUpdate(pt, vDim);
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(true);
    return;
  } 
  
  /***************************************************************************
  **
  ** Zoom to selected operations
  */ 
    
  public void zoomToNextSelected() {
    sup_.incrementToNextSelection();
    Rectangle selected = sup_.getCurrentSelectedBounds();
    zoomToSelectedGuts(selected);
    return;
  } 
  
  /***************************************************************************
  **
  ** Center to selected operations
  */ 
    
  public void centerToNextSelected() {
    sup_.incrementToNextSelection();
    Rectangle selected = sup_.getCurrentSelectedBounds();
    centerOnSelectedGuts(selected);
    return;
  } 
 
  /***************************************************************************
  **
  ** Zoom to selected operations
  */ 
    
  public void zoomToPreviousSelected() {
    sup_.decrementToPreviousSelection();
    Rectangle selected = sup_.getCurrentSelectedBounds();
    zoomToSelectedGuts(selected);
    return;
  } 
  
  /***************************************************************************
  **
  ** Center to selected operations
  */ 
    
  public void centerToPreviousSelected() {
    sup_.decrementToPreviousSelection();
    Rectangle selected = sup_.getCurrentSelectedBounds();
    centerOnSelectedGuts(selected);
    return;
  } 
  
   /***************************************************************************
  **
  ** Zoom to selected operations
  */ 
    
  public void zoomToCurrentSelected() {
    Rectangle selected = sup_.getCurrentSelectedBounds();
    zoomToSelectedGuts(selected);
    return;
  } 
  
  /***************************************************************************
  **
  ** Center to selected operations
  */ 
    
  public void centerToCurrentSelected() {
    Rectangle selected = sup_.getCurrentSelectedBounds();
    centerOnSelectedGuts(selected);
    return;
  } 
  
  /***************************************************************************
  **
  ** Zoom to a rectangle operations
  */ 
    
  public void zoomToRect(Rectangle bounds) {
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    Rectangle union = wsBounds.union(bounds);
    if (!union.equals(wsBounds)) { // i.e. selected is outside of union
      Point2D cent = sup_.getRawCenterPoint();
      bounds = centeredUnion(union, cent);
    }
    Dimension vDim = scrollDims();
    ZoomResult zres = calcOptimalZoom(bounds);    
    setCurrentZoom(zres);
    sup_.setWideZoomFactor(getCurrentZoom(), vDim); 
    int x = bounds.x + (bounds.width / 2);
    int y = bounds.y + (bounds.height / 2);    
    Point pt = new Point(x, y);
    pt = sup_.pointToViewport(pt);
    viewportUpdate(pt, vDim);
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }    
  
  /***************************************************************************
  **
  ** If we need to display a union of model bounds and worksheet (model is
  ** off edges of worksheet), we need to increase the union to be centered
  ** on the worksheet.
  */ 
    
  private Rectangle centeredUnion(Rectangle union, Point2D center) {
    int cenX = (int)Math.round(center.getX());
    int cenY = (int)Math.round(center.getY());
    int delmx = cenX - union.x;
    int delmy = cenY - union.y;
    int delpx = (union.x + union.width) - cenX;
    int delpy = (union.y + union.height) - cenY;    
    int rX = (delpx > delmx) ? cenX - delpx : cenX - delmx;
    int rW = (delpx > delmx) ? (2 * delpx) : (2 * delmx);
    int rY = (delpy > delmy) ? cenY - delpy : cenY - delmy;
    int rH = (delpy > delmy) ? (2 * delpy) : (2 * delmy);    
   
    return (new Rectangle(rX, rY, rW, rH));
  }
  
  /***************************************************************************
  **
  ** Zoom to show current model.  If it is off to one corner of the worksheet, we will scroll
  ** as needed.
  */ 
    
  public void zoomToModel() {
    Rectangle bounds = sup_.getCurrentBasicBounds();
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    Rectangle union = wsBounds.union(bounds);
    if (!union.equals(wsBounds)) { // i.e. part of model outside of workspace: we will center on workspace
      Point2D cent = sup_.getRawCenterPoint();
      bounds = centeredUnion(union, cent);
    }
    ZoomResult zres = calcOptimalZoom(bounds); 
    Dimension vDim = scrollDims();
    setCurrentZoom(zres);    
    sup_.setWideZoomFactor(getCurrentZoom(), vDim);
    
    int x = bounds.x + (bounds.width / 2);
    int y = bounds.y + (bounds.height / 2);    
    Point pt = new Point(x, y);
    pt = sup_.pointToViewport(pt);
    viewportUpdate(pt, vDim);
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }
  
  /***************************************************************************
  **
  ** Zoom to show all models.  If they are off to one corner of the worksheet, we will scroll
  ** as needed.
  */ 
    
  public void zoomToAllModels() {
    Rectangle bounds = sup_.getAllModelBounds();
    Rectangle wsBounds = sup_.getWorkspaceBounds();
    Rectangle union = wsBounds.union(bounds);
    if (!union.equals(wsBounds)) { // i.e. part of all model outside of workspace: we will center on workspace
      Point2D cent = sup_.getRawCenterPoint();
      bounds = centeredUnion(union, cent);
    }
    ZoomResult zres = calcOptimalZoom(bounds); 
    Dimension vDim = scrollDims();
    setCurrentZoom(zres);    
    sup_.setWideZoomFactor(getCurrentZoom(), vDim);
 
    int x = bounds.x + (bounds.width / 2);
    int y = bounds.y + (bounds.height / 2);    
    Point pt = new Point(x, y);
    pt = sup_.pointToViewport(pt);
    
    viewportUpdate(pt, vDim);
    sup_.repaint();
    if (tracker_ != null) tracker_.zoomStateChanged(false);
    return;
  }  

  /***************************************************************************
  **
  ** Changes to panel size result in changes to world->view mappings, so we
  ** do this here:
  */ 
    
  public void handlePanelResize() { 
    JViewport view = jsp_.getViewport();     
    Dimension viewExtent = view.getExtentSize();
    Dimension viewSize = view.getViewSize(); 

    int currX = (int)Math.round((currViewXFrac_ * viewSize.width) - (viewExtent.width / 2.0));
    int currY = (int)Math.round((currViewYFrac_ * viewSize.height) - (viewExtent.height / 2.0));
    view.setViewPosition(doBounding(currX, currY, view));            

    //
    // When the viewport size exceeds the worksheet area, we need to change the 
    // world->viewport transform when viewport size changes:
    // 
    sup_.adjustWideZoomForSize(scrollDims());
    currViewSize_ = viewExtent;
    currClipRect_ = view.getViewRect();
    if (tracker_ != null) tracker_.zoomStateChanged(true);
    return;
  }

  /***************************************************************************
  **
  ** Always track the changes in the scroll bars.  We are NOT interested
  ** in any changes due to resizes, so we toss out results when the extent
  ** size is changing:
  */ 
    
  public void trackScrollBars() { 
    
    JViewport view = jsp_.getViewport();
    Dimension viewExtent = view.getExtentSize();
    if (currViewSize_ == null) {
      currViewSize_ = viewExtent;
    }
    
    if (viewExtent.equals(currViewSize_)) {
      Point viewPos = view.getViewPosition();
      int currX = viewPos.x + (viewExtent.width / 2);
      int currY = viewPos.y + (viewExtent.height / 2);
      Dimension viewSize = view.getViewSize(); 
      currViewXFrac_ = (double)currX / (double)viewSize.width; 
      currViewYFrac_ = (double)currY / (double)viewSize.height;
      currClipRect_ = view.getViewRect();
    }
    if (tracker_ != null) tracker_.zoomStateChanged(true);
    return;
  }
  
  /***************************************************************************
  **
  ** We need to know when scroll bar visibility changes, since we need to
  ** always have an accurate read on extent size (Fixes issue #16)
  */ 
    
  public void checkBarVisible() {    
    JViewport view = jsp_.getViewport();
    currViewSize_ = view.getExtentSize();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Zoom calculation result
  */
  
  private class ZoomResult {
    boolean doCustom;
    int index;
    double customZoom;
    
    ZoomResult(int index) {
      this.doCustom = false;
      this.index = index;
    }
    
    ZoomResult(double customZoom) {
      this.doCustom = true;
      this.customZoom = customZoom;
    }    
   
    public String toString() {
      return ((doCustom) ? ("ZoomResult customZoom = " + customZoom) : ("ZoomResult indexed zoom = " + index));  
    }
  
  }
}
