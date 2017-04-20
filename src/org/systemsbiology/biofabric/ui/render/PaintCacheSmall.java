/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.ui.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.MinMax;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.QuadTree;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This is the cache of simple paint objects
*/

public class PaintCacheSmall implements PaintCache {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private final static double BB_HALF_WIDTH_ = 5.0F;
  
  private final static double PERCENT_PAD_ = 0.10;
  private final static int MINIMUM_PAD_ = 10;
  
  private final static double DRAIN_ZONE_ROW_OFFSET_ = 0.5F; // Lifts drain zone text above the node line & boxes
  private final static double NODE_LABEL_X_SHIM_ = 5.0F;
  private final static double LABEL_FONT_HEIGHT_SCALE_ = 2.0 / 3.0;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static int STROKE_SIZE = 3;
  
  public final static int TINY = 0;
  public final static int HUGE = 1;
  public final static int MED = 2;
  public final static int MED_SMALL = 3;
  public final static int SMALL = 4;
  private final static int NUM_FONTS_ = 5;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  private Font[] fonts_; 
 
  private LinePath[] nodes_;
  private List<BioFabricNetwork.LinkInfo> linkRefs_;
  private int[] linkIndex_;
  private QuadTree names_;
  private HashMap<String, PaintedPath> nameKeyToPaintFirst_;
  private HashMap<String, PaintedPath> nameKeyToPaintSecond_;
  private HashMap<String, PaintedPath> nameKeyToPaintThird_;
  private int nameKeyCount_;
  
  private FabricColorGenerator colGen_;
  private Color superLightPink_;
  private Color superLightBlue_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PaintCacheSmall(FabricColorGenerator colGen) {
  	fonts_ = new Font[NUM_FONTS_];
    fonts_[TINY] = new Font("SansSerif", Font.PLAIN, 10);
    fonts_[HUGE] = new Font("SansSerif", Font.PLAIN, 200);
    fonts_[MED] = new Font("SansSerif", Font.PLAIN, 100);
    fonts_[MED_SMALL] = new Font("SansSerif", Font.PLAIN, 70);
    fonts_[SMALL] = new Font("SansSerif", Font.PLAIN, 30);
    
    nameKeyToPaintFirst_ = new HashMap<String, PaintedPath>();
    nameKeyToPaintSecond_ = new HashMap<String, PaintedPath>();
    nameKeyToPaintThird_ = new HashMap<String, PaintedPath>();

    colGen_ = colGen;
    superLightPink_ = new Color(255, 244, 244);
    superLightBlue_ = new Color(244, 244, 255);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  **  Dump used memory
  */
  
  public void clear() {	
  	nameKeyToPaintFirst_.clear();
    nameKeyToPaintSecond_.clear();
    nameKeyToPaintThird_.clear();
    nodes_ = null;
    linkRefs_ = null;
    linkIndex_ = null;
    if (names_ != null) {
    	names_.clear();
    }
  	return;
  }
 
  /***************************************************************************
  **
  **  Answer if we have something to do...
  */
  
  public boolean needToPaint() {
  	UiUtil.fixMePrintout("NO! Need to check array lengths");
  	return (true);
 //   return (!(paintPathsFirstPass_.isEmpty() && paintPathsSecondPass_.isEmpty() && paintPathsThirdPass_.isEmpty()
    //		non-null arrays!
   // 		));
  }
  
  /***************************************************************************
  **
  **  paint it
  */
  
  public boolean paintIt(Graphics2D g2, Rectangle clip, boolean forSelection, Reduction reduce) {
    boolean retval = false;
    HashSet<String> pKeys = new HashSet<String>();
    if (names_ != null) {
      names_.getPayloadKeys(clip, pKeys);
    }
    
    //
    // First pass is background rectangles, which are not drawn for selections:
    //
    if (reduce == null) {
	    Iterator<String> pkit = pKeys.iterator();
	    while (pkit.hasNext()) {
	    	String pkey = pkit.next();
	      PaintedPath pp = nameKeyToPaintFirst_.get(pkey);
	    	if (pp != null) {
	        int result = pp.paint(g2, clip, forSelection, fonts_);
	        retval = retval || (result > 0);
	    	}
	    }
    }
    //
    // These have to be done differently:
    //
    if (reduce == null) {
	    Iterator<String> pkit = pKeys.iterator();
	    while (pkit.hasNext()) {
	    	String pkey = pkit.next();
	      PaintedPath pp = nameKeyToPaintSecond_.get(pkey);
	    	if (pp != null) {
	        int result = pp.paint(g2, clip, forSelection, fonts_);
	        retval = retval || (result > 0);
	    	}
	    }
    }

    if (nodes_ != null) {  	
    	double minY = clip.getMinY() / BioFabricPanel.GRID_SIZE;
    	double maxY = clip.getMaxY() / BioFabricPanel.GRID_SIZE;  
    	//
    	// Want a padding on either side based on percentage, though for tiny
    	// clips (e.g. for magnifier), we want that to not be smaller than a minimum:
    	//
    	int extraRows = (int)Math.round((maxY - minY) * PERCENT_PAD_ * 0.5);
    	if (extraRows < MINIMUM_PAD_) {
    		extraRows = MINIMUM_PAD_;
    	}
	    int startRow = (int)Math.floor(minY) - extraRows;
	    int endRow = (int)Math.floor(maxY) + extraRows;
	    
	    startRow = (startRow < 0) ? 0 : startRow;
	    endRow = (endRow >= nodes_.length) ? nodes_.length - 1 : endRow;
	    
	    for (int i = startRow; i < endRow; i++) {
	    	if ((reduce == null) || reduce.paintRows.contains(Integer.valueOf(i))) {
	       	if (nodes_[i] != null) {
		        int result = nodes_[i].paint(g2, clip, forSelection);
		        retval = retval || (result > 0);
	    	  }
	    	}
	    } 
    }

    if (linkRefs_ != null) {  	
    	double minX = clip.getMinX() / BioFabricPanel.GRID_SIZE;
    	double maxX = clip.getMaxX() / BioFabricPanel.GRID_SIZE;   	
    	//
    	// Want a padding on either side based on percentage, though for tiny
    	// clips (e.g. for magnifier), we want that to not be smaller than a minimum:
    	//
    	int extraCols = (int)Math.round((maxX - minX) * PERCENT_PAD_ * 0.5);
    	if (extraCols < MINIMUM_PAD_) {
    		extraCols = MINIMUM_PAD_;
    	}
    	
	    int startCol = (int)Math.floor(minX) - extraCols;
	    int endCol = (int)Math.floor(maxX) + extraCols;
	    
	    startCol = (startCol < 0) ? 0 : startCol;
	    endCol = (endCol >= linkIndex_.length) ? linkIndex_.length - 1 : endCol;

	    Line2D line = new Line2D.Double();
      LinePath lp = new LinePath();
      MinMax mm = new MinMax();
      GlyphPath gp = new GlyphPath();

	    for (int i = startCol; i < endCol; i++) {
	    	if ((reduce == null) || reduce.paintCols.contains(Integer.valueOf(i))) {
	    		BioFabricNetwork.LinkInfo li = linkRefs_.get(linkIndex_[i]);
          int sRow = li.topRow();
          int eRow = li.bottomRow();
          Color paintCol = getColorForLink(li, colGen_);
     
          int yStrt = sRow * BioFabricPanel.GRID_SIZE;
          int yEnd = eRow * BioFabricPanel.GRID_SIZE;
          int x = i * BioFabricPanel.GRID_SIZE;
  
          line.setLine(x, yStrt, x, yEnd);
          int result = lp.reset(paintCol, line, x, Integer.MIN_VALUE, mm.reset(yStrt, yEnd)).paint(g2, clip, forSelection);
		      retval = retval || (result > 0);
         
          if (!li.isDirected()) {
          	gp.reuse(paintCol, x, yStrt, yEnd, BB_HALF_WIDTH_, false);
          } else {
            int ySrc = li.getStartRow() * BioFabricPanel.GRID_SIZE;
            int yTrg = li.getEndRow() * BioFabricPanel.GRID_SIZE;
            result = gp.reuse(paintCol, x, ySrc, yTrg, BB_HALF_WIDTH_, true).paint(g2, clip, forSelection);
            retval = retval || (result > 0);
          }
	    	}
	    }
    }
    
    if (reduce == null) {
	    Iterator<String> pkit = pKeys.iterator();
	    while (pkit.hasNext()) {
	    	String pkey = pkit.next();
	      PaintedPath pp = nameKeyToPaintThird_.get(pkey);
	    	if (pp != null) {
	        int result = pp.paint(g2, clip, forSelection, fonts_);
	        retval = retval || (result > 0);
	    	}
	    }
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drawing core
  */
  
  public void drawFloater(Graphics2D g2, FloaterSet floaters) {
    if ((floaters.floater == null) && (floaters.tourRect == null) && (floaters.currSelRect == null)) {
      return;
    }       
    BasicStroke selectedStroke = new BasicStroke(6, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);    
    g2.setStroke(selectedStroke);    
    g2.setPaint(Color.BLACK);
    if (floaters.floater != null) {
      g2.drawRect(floaters.floater.x, floaters.floater.y, floaters.floater.width, floaters.floater.height); 
    }  
    if (floaters.tourRect != null) {
      g2.setPaint(new Color(0, 0, 255, 125));
      g2.drawArc(floaters.tourRect.x, floaters.tourRect.y, floaters.tourRect.width, floaters.tourRect.height, 0, 360); 
    }
    if (floaters.currSelRect != null) {
      g2.setPaint(new Color(Color.orange.getRed(), Color.orange.getGreen(), Color.orange.getBlue(), 125));
      g2.drawArc(floaters.currSelRect.x, floaters.currSelRect.y, floaters.currSelRect.width, floaters.currSelRect.height, 0, 360); 
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Build objcache
  */
  
  public void buildObjCache(List<BioFabricNetwork.NodeInfo> targets, List<BioFabricNetwork.LinkInfo> links, 
                            boolean shadeNodes, boolean showShadows, Map<NID.WithName, Rectangle2D> nameMap, 
                            Map<NID.WithName, List<Rectangle2D>> drainMap, Rectangle2D worldRect,
                            BTProgressMonitor monitor, double startFrac, double endFrac) throws AsynchExitRequestException {
  	
  	
  	
    nameKeyToPaintFirst_.clear();
    nameKeyToPaintSecond_.clear();
    nameKeyToPaintThird_.clear();
    names_ = new QuadTree(worldRect, 5);
    nameKeyCount_ = 0;
    
    //
    // Need to find the maximum column in use for the links, depending on shadow display or not:
    //
     
   // nodes_ = new PaintedPath[targets.size()];

    FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
   
    int numLinks = links.size();
    int maxCol = 0;
    
     LoopReporter lr0 = new LoopReporter(links.size(), 20, monitor, 0.0, 1.0, "progress.calcLinkExtents");
    
    HashMap<Integer, MinMax> linkExtents = new HashMap<Integer, MinMax>();
    for (int i = 0; i < numLinks; i++) {
      BioFabricNetwork.LinkInfo link = links.get(i);
      lr0.report();    
      int num = link.getUseColumn(showShadows);
      int sRow = link.topRow();
      int eRow = link.bottomRow();
      linkExtents.put(Integer.valueOf(num), new MinMax(sRow, eRow));
      if (num > maxCol) {
      	maxCol = num;
      }
    }
    
    nodes_ = new LinePath[targets.size()];
    linkIndex_ = new int[maxCol + 1];
    linkRefs_ = links;
     
    LoopReporter lr = new LoopReporter(targets.size(), 20, monitor, 0.0, 1.0, "progress.buildNodeGraphics");
    Iterator<BioFabricNetwork.NodeInfo> trit = targets.iterator();
    while (trit.hasNext()) {
      BioFabricNetwork.NodeInfo target = trit.next();
      lr.report();
      buildALineHorz(target, frc, colGen_, linkExtents, shadeNodes, 
      		           showShadows, nameMap, drainMap);
    }
     
    //
    // We do not build e.g. 10^6 link paths, but stock the drawing primitives while painting:
    //
    
    LoopReporter lr2 = new LoopReporter(numLinks, 20, monitor, 0.0, 1.0, "progress.buildLinkGraphics");
    for (int i = 0; i < numLinks; i++) {
      BioFabricNetwork.LinkInfo link = links.get(i);
      lr2.report();
      linkIndex_[link.getUseColumn(showShadows)] = i;
    }

    return;
  }
 
  /***************************************************************************
  ** 
  ** Get a link color
  */

  public Color getColorForLink(BioFabricNetwork.LinkInfo link, FabricColorGenerator colGen) {
    return (colGen.getModifiedColor(link.getColorKey(), FabricColorGenerator.DARKER)); 
  }
  
  /***************************************************************************
  ** 
  ** Get a node color
  */

  public Color getColorForNode(BioFabricNetwork.NodeInfo node, FabricColorGenerator colGen) {
    return (colGen.getModifiedColor(node.colorKey, FabricColorGenerator.BRIGHTER)); 
  }

  /***************************************************************************
  **
  ** Build a line
  */
  
  private void buildALineHorz(BioFabricNetwork.NodeInfo target,
                              FontRenderContext frc, 
                              FabricColorGenerator colGen, Map<Integer, MinMax> linkExtents, 
                              boolean shadeNodes, boolean showShadows, Map<NID.WithName, Rectangle2D> nameMap, 
                              Map<NID.WithName, List<Rectangle2D>> drainMap) {
        
    //
    // Drain zone sizing / rotation:
    //
    
    List<BioFabricNetwork.DrainZone> zones = target.getDrainZones(showShadows);
    
    DrainZoneInfo[] dzis = new DrainZoneInfo[zones.size()];
    for (int i = 0; i < dzis.length; i++) {    // initialize each entry in array
      dzis[i] = new DrainZoneInfo(zones.get(i));
    }
    
    for (int i = 0; i < zones.size(); i++) {
      
      DrainZoneInfo curr = dzis[i];
      curr.doRotateName = false;
      
      if (curr.dzmm == null) {
        continue;
      }
      
      curr.diff = curr.dzmm.max - curr.dzmm.min;
      
      Rectangle2D bounds = fonts_[HUGE].getStringBounds(target.getNodeName(), frc);
      if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * curr.diff)) {
        curr.font = 0;
        curr.dumpRect = bounds;
      } else {
        bounds = fonts_[MED].getStringBounds(target.getNodeName(), frc);
        if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * curr.diff)) {
          curr.font = 1;
          curr.dumpRect = bounds;
        } else {
          bounds = fonts_[MED_SMALL].getStringBounds(target.getNodeName(), frc);
          if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * curr.diff)) {
            curr.font = 2;
            curr.dumpRect = bounds;
          } else {
            bounds = fonts_[SMALL].getStringBounds(target.getNodeName(), frc);
            if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * curr.diff)) {
              curr.font = 3;
              curr.dumpRect = bounds;
            } else {
              bounds = fonts_[TINY].getStringBounds(target.getNodeName(), frc);
              if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * curr.diff)) {
                curr.font = 4;
                curr.dumpRect = bounds;
              } else {
                curr.font = 4;
                curr.dumpRect = bounds;
                curr.doRotateName = true;
              }
            }
          }
        }
      }
    }
    
    // Drain zone Y: Lifted slightly above node line and link boxes:
    
    double tnamey = (target.nodeRow - DRAIN_ZONE_ROW_OFFSET_) * BioFabricPanel.GRID_SIZE;
    
    MinMax colmm = target.getColRange(showShadows);
    
    //
    // Node label sizing and Y:
    //
    
    Rectangle2D labelBounds = fonts_[TINY].getStringBounds(target.getNodeName(), frc);
    // Easiest font height hack is to scale it by ~.67: 
    double scaleHeight = labelBounds.getHeight() * LABEL_FONT_HEIGHT_SCALE_;
    double namey = (target.nodeRow * BioFabricPanel.GRID_SIZE) + (scaleHeight / 2.0);
    double namex = (colmm.min * BioFabricPanel.GRID_SIZE) - labelBounds.getWidth() - BB_HALF_WIDTH_ - NODE_LABEL_X_SHIM_;      
    labelBounds.setRect(namex, namey - scaleHeight, labelBounds.getWidth(), scaleHeight);

    //
    // Bogus non-link safety:
    //
    
    if ((colmm.max == Integer.MIN_VALUE) || (colmm.min == Integer.MAX_VALUE)) {
    	UiUtil.fixMePrintout("Does this still need to exist??");
   //   objCache.add(new PaintedPath(Color.BLACK, target.getNodeName(), 100.0F, namey, 100.0F, tnamey, 
    //                               doRotateName, useFont, labelBounds, dumpRect));
   //   nameMap.put(target.getNodeIDWithName(), (Rectangle2D)labelBounds.clone());
      return;
    }
    
    //
    // Create the node line and process it
    //
    
    int yval = (target.nodeRow * BioFabricPanel.GRID_SIZE);
    int xStrt = (colmm.min * BioFabricPanel.GRID_SIZE);
    int xEnd = (colmm.max * BioFabricPanel.GRID_SIZE);
    Line2D line = new Line2D.Double(xStrt, yval, xEnd, yval);
    Color paintCol = getColorForNode(target, colGen);
    nodes_[target.nodeRow] = new LinePath(paintCol, line, Integer.MIN_VALUE, yval, new MinMax(xStrt, xEnd));
    nameMap.put(target.getNodeIDWithName(), (Rectangle2D) labelBounds.clone());
	
    PaintedPath npp = new PaintedPath(Color.BLACK, target.getNodeName(), namex, namey, labelBounds);
    String nkk = Integer.toString(nameKeyCount_++);
    QuadTree.Payload pay = new QuadTree.Payload(labelBounds, nkk);
    names_.insertPayload(pay);
    nameKeyToPaintSecond_.put(nkk, npp);    
    
    List<Rectangle2D> rectList = new ArrayList<Rectangle2D>();
     
    for (int i = 0; i < zones.size(); i++) {
      
      DrainZoneInfo curr = dzis[i];
      
      if (curr.dzmm == null) {
        continue;  
      }

	    //
	    // Print out drain zone text _if_ there is a drain zone:
	    //
	
	    float tnamex = 0;
	      if (curr.dumpRect != null) {
	      // Easiest font height hack is to scale it by ~.67:
	        double dumpScaleHeight = curr.dumpRect.getHeight() * LABEL_FONT_HEIGHT_SCALE_;
	        if (curr.doRotateName) {
	          tnamex = (curr.dzmm.min * BioFabricPanel.GRID_SIZE) +
	                  ((curr.diff * BioFabricPanel.GRID_SIZE) / 2.0F) +
	                 (float)(dumpScaleHeight / 2.0);
	          curr.dumpRect.setRect(tnamex - dumpScaleHeight, tnamey - curr.dumpRect.getWidth(),
	                  dumpScaleHeight, curr.dumpRect.getWidth());
	      } else {  
	          tnamex = (curr.dzmm.min * BioFabricPanel.GRID_SIZE) +
	                  ((curr.diff * BioFabricPanel.GRID_SIZE) / 2.0F) - ((int) curr.dumpRect.getWidth() / 2);
	          curr.dumpRect.setRect(tnamex, tnamey - dumpScaleHeight, curr.dumpRect.getWidth(), dumpScaleHeight);
	      }
	      if (shadeNodes) {
	      	Color col = ((target.nodeRow % 2) == 0) ? superLightBlue_ : superLightPink_;
	        buildANodeShadeRect(curr.dzmm, linkExtents, curr.dumpRect, col);
	      }
	    }
    
	    //
	    // Output the node label and (optional) drain zone label.  If we are using a tiny font for the drain
	    // zone, it goes out last to get drawn above the links.
	    //
	    PaintedPath drain = new PaintedPath(Color.BLACK, target.getNodeName(), namex, namey, tnamex, tnamey, 
	                                        curr.doRotateName, curr.font, labelBounds, curr.dumpRect);
	    nkk = Integer.toString(nameKeyCount_++);
	    pay = new QuadTree.Payload(curr.dumpRect, nkk);
	    names_.insertPayload(pay);
	    if (curr.font == 4) {  
	    	nameKeyToPaintThird_.put(nkk, drain);
	    } else {
	    	nameKeyToPaintSecond_.put(nkk, drain);
	    }
      
      if (curr.dumpRect != null) {
        rectList.add((Rectangle2D) curr.dumpRect.clone());
      }
    }
    drainMap.put(target.getNodeIDWithName(), rectList);
    
    return;
  } 
  
  /***************************************************************************
   ** Contains the properties required to draw a drain zone
   */
  
  private static class DrainZoneInfo {
    
    private int diff, font;
    private boolean doRotateName;
    private Rectangle2D dumpRect;
    private MinMax dzmm;
    

    DrainZoneInfo(BioFabricNetwork.DrainZone dz) {
      this.dzmm = dz.getMinMax().clone();
    }
  }
  
  /***************************************************************************
  **
  ** Build a backRect
  */
  
  private void buildANodeShadeRect(MinMax dzmm, Map<Integer, MinMax> linkExtents, 
  		                             Rectangle2D dumpRect, Color col) {  
    int minRow = Integer.MAX_VALUE;
    int maxRow = Integer.MIN_VALUE;       
    for (int i = dzmm.min; i <= dzmm.max; i++) {
      MinMax range = linkExtents.get(Integer.valueOf(i));
      if (range != null) {
        if (minRow > range.min) {
          minRow = range.min;
        }
        if (maxRow < range.max) {
          maxRow = range.max;
        }
      }
    }
    int rectLeft = (int)Math.floor((double)(dzmm.min * BioFabricPanel.GRID_SIZE) - BB_HALF_WIDTH_ - (STROKE_SIZE / 2.0));
    int topRow = (int)Math.floor((double)(minRow * BioFabricPanel.GRID_SIZE) - BB_HALF_WIDTH_ - (STROKE_SIZE / 2.0));
    int rectTop = (dumpRect == null) ? topRow : Math.min((int)dumpRect.getMinY(), topRow);
    int rectRight = (int)Math.ceil((double)(dzmm.max * BioFabricPanel.GRID_SIZE) + BB_HALF_WIDTH_ + (STROKE_SIZE / 2.0));
    int rectWidth = rectRight - rectLeft;
    int rectBot = (int)Math.floor((double)(maxRow * BioFabricPanel.GRID_SIZE) + BB_HALF_WIDTH_ + (STROKE_SIZE / 2.0));
    int rectHeight = rectBot - rectTop;
    Rectangle rect = new Rectangle(rectLeft, rectTop, rectWidth, rectHeight);
    PaintedPath npp = new PaintedPath(col, rect);
    String nkk = Integer.toString(nameKeyCount_++);
    QuadTree.Payload pay = new QuadTree.Payload(rect, nkk);
    names_.insertPayload(pay);
    nameKeyToPaintFirst_.put(nkk, npp);
    return;
  }
}
