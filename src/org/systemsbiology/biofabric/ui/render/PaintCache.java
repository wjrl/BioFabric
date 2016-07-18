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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.util.MinMax;

/****************************************************************************
**
** This is the cache of simple paint objects
*/

public class PaintCache {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private final static float BB_RADIUS_ = 5.0F;
  
  
  private final static float DRAIN_ZONE_ROW_OFFSET_ = 0.5F; // Lifts drain zone text above the node line & boxes
  private final static float NODE_LABEL_X_SHIM_ = 5.0F;
  private final static double LABEL_FONT_HEIGHT_SCALE_ = 2.0 / 3.0;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static int STROKE_SIZE = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  private Font tiny_;
  private Font huge_;
  private Font med_;
  private Font medSmall_;
  private Font small_;
  
  private List<PaintedPath> paintPaths_;
  private FabricColorGenerator colGen_;
  private Color superLightPink_;
  private Color superLightBlue_;
  private Color superLightNegGray_;
  
  private Color[] superLightCycle_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PaintCache(FabricColorGenerator colGen) {
    tiny_ = new Font("SansSerif", Font.PLAIN, 10);
    huge_ = new Font("SansSerif", Font.PLAIN, 200);
    med_ = new Font("SansSerif", Font.PLAIN, 100);
    medSmall_ = new Font("SansSerif", Font.PLAIN, 70);
    small_ = new Font("SansSerif", Font.PLAIN, 30);
    paintPaths_ = new ArrayList<PaintedPath>();
    colGen_ = colGen;
    superLightPink_ = new Color(255, 244, 244);
    superLightBlue_ = new Color(244, 244, 255);
    superLightCycle_ = new Color[6];
    superLightCycle_[0] = new Color(244, 222, 222);
    superLightCycle_[1] = new Color(222, 222, 244);
    superLightCycle_[2] = new Color(222, 244, 222);
    superLightCycle_[3] = new Color(244, 222, 244); 
    superLightCycle_[4] = new Color(244, 244, 222);
    superLightCycle_[5] = new Color(222, 244, 244); 
    superLightNegGray_ = new Color(200, 200, 200);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  **  Answer if we have something to do...
  */
  
  public boolean needToPaint() {
  	System.out.println("paint paths = " + paintPaths_.size());
    return (!paintPaths_.isEmpty());
  }
  
  /***************************************************************************
  **
  **  paint it
  */
  
  public boolean paintIt(Graphics2D g2, boolean doBoxes, Rectangle clip, boolean forSelection) {
    boolean retval = false;
    int numpp = paintPaths_.size();
    for (int i = 0; i < numpp; i++) {
      PaintedPath pp = paintPaths_.get(i);
      int result = pp.paint(g2, true, clip, forSelection);
      retval = retval || (result > 0);
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
                            boolean shadeNodes, boolean showShadows, Map<String, Rectangle2D> nameMap, 
                            Map<String, Rectangle2D> drainMap) {
    paintPaths_.clear();
    FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
   
    int numLinks = links.size();
    
    HashMap<Integer, MinMax> linkExtents = new HashMap<Integer, MinMax>();
    for (int i = 0; i < numLinks; i++) {
      BioFabricNetwork.LinkInfo link = links.get(i);
      int num = link.getUseColumn(showShadows);
      int sRow = link.topRow();
      int eRow = link.bottomRow();
      linkExtents.put(Integer.valueOf(num), new MinMax(sRow, eRow));
    } 
       
   // drawLinkBacks(linkExtents, links, paintPaths_);
    drawNegativeLinkBacks(linkExtents, links, paintPaths_);
  
    ArrayList<PaintedPath> postPaths = new ArrayList<PaintedPath>();
    ArrayList<PaintedPath> postPostPaths = new ArrayList<PaintedPath>();
    Iterator<BioFabricNetwork.NodeInfo> trit = targets.iterator();
    while (trit.hasNext()) {
      BioFabricNetwork.NodeInfo target = trit.next();
      buildALineHorz(target, paintPaths_, postPaths, postPostPaths, frc, 
                     colGen_, linkExtents, shadeNodes, showShadows, nameMap, drainMap);
    }
    paintPaths_.addAll(postPaths);
    for (int i = 0; i < numLinks; i++) {
      BioFabricNetwork.LinkInfo link = links.get(i);
      buildALineVert(link, paintPaths_, colGen_, showShadows);
    }
    paintPaths_.addAll(postPostPaths);
    


    return;
  }
  
  /***************************************************************************
  ** 
  ** Draw link backing rectangles
  */

  public void drawLinkBacks(HashMap<Integer, MinMax> linkExtents, List<BioFabricNetwork.LinkInfo> links, List<PaintedPath> paths) {
  	
  	int numLinks = links.size();
    String currRel = null;
    int currRelStart = 0;
    int backCount = 0;
    for (int i = 0; i < numLinks; i++) {    	
      BioFabricNetwork.LinkInfo link = links.get(i);
      String linkRel = link.getAugRelation().relation;
      if (i == 0) {
      	currRel = linkRel;
      	currRelStart = 0;
      } else {
      	if (!linkRel.equals(currRel)) {
      		MinMax forRel = new MinMax(currRelStart, i - 1);
      		Color col = superLightCycle_[backCount++ % 6];
      		buildABackRect(forRel, linkExtents, null, paths, col);
      		currRel = linkRel;
      		currRelStart = i;
      	}
      }
    }
    MinMax forRel = new MinMax(currRelStart, numLinks - 1);
    Color col = superLightCycle_[backCount++ % 6];
    buildABackRect(forRel, linkExtents, null, paths, col);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Draw link backing rectangles
  */

  public void drawNegativeLinkBacks(HashMap<Integer, MinMax> linkExtents, List<BioFabricNetwork.LinkInfo> links, List<PaintedPath> paths) {
  	
  	int numLinks = links.size();
    int negStart = 0;
    boolean lastRelIsNeg = false;
    for (int i = 0; i <= numLinks; i++) {
    	boolean relIsNeg = false;
    	if (i < numLinks) {
        BioFabricNetwork.LinkInfo link = links.get(i);
        String linkRel = link.getAugRelation().relation;
        relIsNeg = (linkRel.indexOf("-") == 0);
    	} else {
    		relIsNeg = !lastRelIsNeg;
    	}
    	if (relIsNeg) {
    		if ((i == 0) || !lastRelIsNeg) {
    		 	negStart = i;
    		}
    	} else if (lastRelIsNeg) {
    		MinMax forRel = new MinMax(negStart, i - 1);
        buildABackRect(forRel, linkExtents, null, paths, superLightNegGray_);
    	}
    	lastRelIsNeg = relIsNeg;
    }
    return;
  }
  
  
 
  /***************************************************************************
  ** 
  ** Get detail panel
  */

  public Color getColorForLink(BioFabricNetwork.LinkInfo link, FabricColorGenerator colGen) {
    return (colGen.getModifiedColor(link.getColorKey(), FabricColorGenerator.DARKER)); 
  }
  
  /***************************************************************************
  ** 
  ** Get detail panel
  */

  public Color getColorForNode(BioFabricNetwork.NodeInfo node, FabricColorGenerator colGen) {
    return (colGen.getModifiedColor(node.colorKey, FabricColorGenerator.BRIGHTER)); 
  }

  /***************************************************************************
  **
  ** Build a line
  */
  
  private void buildALineVert(BioFabricNetwork.LinkInfo link, List<PaintedPath> objCache, FabricColorGenerator colGen, boolean showShadows) {
 
    int num = link.getUseColumn(showShadows);
    int sRow = link.topRow();
    int eRow = link.bottomRow();
    Color paintCol = getColorForLink(link, colGen);
       
    int yStrt = sRow * BioFabricPanel.GRID_SIZE;
    int yEnd = eRow * BioFabricPanel.GRID_SIZE;
    int x = num * BioFabricPanel.GRID_SIZE;
    
    Line2D line = new Line2D.Double(x, yStrt, x, yEnd);
  
    PaintedPath boxPath;
    if (!link.isDirected()) {
      boxPath = buildABox(paintCol, x, yStrt, yEnd);
    } else {
      int ySrc = link.getStartRow() * BioFabricPanel.GRID_SIZE;
      int yTrg = link.getEndRow() * BioFabricPanel.GRID_SIZE;
      boxPath = buildAnArrow(paintCol, x, ySrc, yTrg);
    }
    objCache.add(new PaintedPath(paintCol, line, x, Integer.MIN_VALUE, new MinMax(yStrt, yEnd)));
    objCache.add(boxPath);
    return;
  }  

  /***************************************************************************
  **
  ** buildABox
  */
  
  private PaintedPath buildABox(Color color, int x, int yStrt, int yEnd) {
    Rectangle2D circ = new Rectangle2D.Double(x - BB_RADIUS_, yStrt - BB_RADIUS_, 2.0 * BB_RADIUS_, 2.0 * BB_RADIUS_);
    Rectangle2D circ2 = new Rectangle2D.Double(x - BB_RADIUS_, yEnd - BB_RADIUS_, 2.0 * BB_RADIUS_, 2.0 * BB_RADIUS_);    
    return (new PaintedPath(color, circ, circ2));
  }
  
  /***************************************************************************
  **
  ** buildABox
  */
  
  private PaintedPath buildAnArrow(Color color, int x, int yStrt, int yEnd) {
    Rectangle2D circ = new Rectangle2D.Double(x - BB_RADIUS_, yStrt - BB_RADIUS_, 2.0 * BB_RADIUS_, 2.0 * BB_RADIUS_);
    GeneralPath gp = new GeneralPath();
    float yoff = (yStrt < yEnd) ? -BB_RADIUS_ : BB_RADIUS_;
    gp.moveTo(x - BB_RADIUS_, yEnd + 2.0F * yoff);
    gp.lineTo(x, (yEnd + yoff));
    gp.lineTo((x - BB_RADIUS_), (yEnd + yoff));
    gp.lineTo((x - BB_RADIUS_), (yEnd - yoff));
    gp.lineTo((x + BB_RADIUS_), (yEnd - yoff));
    gp.lineTo((x + BB_RADIUS_), (yEnd + yoff));
    gp.lineTo(x, (yEnd + yoff));
    gp.lineTo((x + BB_RADIUS_), (yEnd + 2.0F * yoff));
    gp.closePath();
    return (new PaintedPath(color, circ, gp));
  }   
   
  /***************************************************************************
  **
  ** Build a line
  */
  
  private void buildALineHorz(BioFabricNetwork.NodeInfo target, List<PaintedPath> preCache, 
                              List<PaintedPath> objCache, List<PaintedPath> postPostCache, FontRenderContext frc, 
                              FabricColorGenerator colGen, Map<Integer, MinMax> linkExtents, 
                              boolean shadeNodes, boolean showShadows, Map<String, Rectangle2D> nameMap, 
                              Map<String, Rectangle2D> drainMap) {
        
    //
    // Drain zone sizing / rotation:
    //
    
    int diff = 0;
    int useFont = 0;
    Rectangle2D dumpRect = null;
    boolean doRotateName = false;
    MinMax dzmm = target.getDrainZone(showShadows);
    if (dzmm != null) {
      diff = dzmm.max - dzmm.min;      
      Rectangle2D bounds = huge_.getStringBounds(target.nodeName, frc);
      if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * diff)) {
        useFont = 0;
        dumpRect = bounds;
      } else {
        bounds = med_.getStringBounds(target.nodeName, frc);
        if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * diff)) {
          useFont = 1;
          dumpRect = bounds;
        } else {
          bounds = medSmall_.getStringBounds(target.nodeName, frc);
          if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * diff)) {
            useFont = 2;
            dumpRect = bounds;
          } else {
            bounds = small_.getStringBounds(target.nodeName, frc);
            if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * diff)) {
              useFont = 3;
              dumpRect = bounds;
            } else {
              bounds = tiny_.getStringBounds(target.nodeName, frc);
              if (bounds.getWidth() <= (BioFabricPanel.GRID_SIZE * diff)) {
                useFont = 4;
                dumpRect = bounds;
              } else {
                useFont = 4;
                doRotateName = true;
                dumpRect = bounds;
              }
            }
          }
        }
      }
    }
    
    // Drain zone Y: Lifted slightly above node line and link boxes:
    
    float tnamey = (target.nodeRow - DRAIN_ZONE_ROW_OFFSET_) * BioFabricPanel.GRID_SIZE;
    
    MinMax colmm = target.getColRange(showShadows);
    
    //
    // Node label sizing and Y:
    //
    
    Rectangle2D labelBounds = tiny_.getStringBounds(target.nodeName, frc);
    // Easiest font height hack is to scale it by ~.67: 
    double scaleHeight = labelBounds.getHeight() * LABEL_FONT_HEIGHT_SCALE_;
    float namey = (target.nodeRow * BioFabricPanel.GRID_SIZE) + (float)(scaleHeight / 2.0);
    float namex = (colmm.min * BioFabricPanel.GRID_SIZE) - (float)labelBounds.getWidth() - BB_RADIUS_ - NODE_LABEL_X_SHIM_;      
    labelBounds.setRect(namex, namey - scaleHeight, labelBounds.getWidth(), scaleHeight);

    //
    // Bogus non-link safety:
    //
    
    if ((colmm.max == Integer.MIN_VALUE) || (colmm.min == Integer.MAX_VALUE)) {
      objCache.add(new PaintedPath(Color.BLACK, target.nodeName, 100.0F, namey, 100.0F, tnamey, 
                                   doRotateName, useFont, labelBounds, dumpRect));
      nameMap.put(target.nodeName, (Rectangle2D)labelBounds.clone());
      return;
    }

    //
    // Print out drain zone text _if_ there is a drain zone:
    //

    float tnamex = 0;
    if (dumpRect != null) {
      // Easiest font height hack is to scale it by ~.67:
      double dumpScaleHeight = dumpRect.getHeight() * LABEL_FONT_HEIGHT_SCALE_;
      if (doRotateName) {
        tnamex = (dzmm.min * BioFabricPanel.GRID_SIZE) + 
                 ((diff * BioFabricPanel.GRID_SIZE) / 2.0F) + 
                 (float)(dumpScaleHeight / 2.0);
        dumpRect.setRect(tnamex - dumpScaleHeight, tnamey - dumpRect.getWidth(), 
                         dumpScaleHeight, dumpRect.getWidth());
      } else {  
        tnamex = (dzmm.min * BioFabricPanel.GRID_SIZE) + ((diff * BioFabricPanel.GRID_SIZE) / 2.0F)  - ((int)dumpRect.getWidth() / 2);
        dumpRect.setRect(tnamex, tnamey - dumpScaleHeight, dumpRect.getWidth(), dumpScaleHeight);
      }
      if (shadeNodes) {
      	Color col = ((target.nodeRow % 2) == 0) ? superLightBlue_ : superLightPink_;
      	buildABackRect(dzmm, linkExtents, dumpRect, preCache, col);
      }
    }
    
    //
    // Output the node label and (optional) drain zone label.  If we are using a tiny font for the drain
    // zone, it goes out last to get drawn above the links.
    //
    
    if (useFont == 4) {
      
      postPostCache.add(new PaintedPath(Color.BLACK, target.nodeName, namex, namey, tnamex, tnamey, 
                                   doRotateName, useFont, labelBounds, dumpRect));
    } else {
      objCache.add(new PaintedPath(Color.BLACK, target.nodeName, namex, namey, tnamex, tnamey, 
                                   doRotateName, useFont, labelBounds, dumpRect));
      
    }
    //
    // Output the node line:
    //
    int yval = (target.nodeRow * BioFabricPanel.GRID_SIZE);
    int xStrt = (colmm.min * BioFabricPanel.GRID_SIZE);
    int xEnd = (colmm.max * BioFabricPanel.GRID_SIZE);
    Line2D line = new Line2D.Double(xStrt, yval, xEnd, yval);
    Color paintCol = getColorForNode(target, colGen);     
    objCache.add(new PaintedPath(paintCol, line, Integer.MIN_VALUE, yval, new MinMax(xStrt, xEnd)));
    nameMap.put(target.nodeName, (Rectangle2D)labelBounds.clone());
    if (dumpRect != null) {
      drainMap.put(target.nodeName, (Rectangle2D)dumpRect.clone());
    }    
    return;
  } 
  
  /***************************************************************************
  **
  ** Build a backRect
  */
  
  private void buildABackRect(MinMax dzmm, Map<Integer, MinMax> linkExtents, Rectangle2D dumpRect, List<PaintedPath> preCache, Color col) {  
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
    int rectLeft = (int)Math.floor((double)(dzmm.min * BioFabricPanel.GRID_SIZE) - BB_RADIUS_ - (STROKE_SIZE / 2.0));
    int topRow = (int)Math.floor((double)(minRow * BioFabricPanel.GRID_SIZE) - BB_RADIUS_ - (STROKE_SIZE / 2.0));
    int rectTop = (dumpRect == null) ? topRow : Math.min((int)dumpRect.getMinY(), topRow);
    int rectRight = (int)Math.ceil((double)(dzmm.max * BioFabricPanel.GRID_SIZE) + BB_RADIUS_ + (STROKE_SIZE / 2.0));
    int rectWidth = rectRight - rectLeft;
    int rectBot = (int)Math.floor((double)(maxRow * BioFabricPanel.GRID_SIZE) + BB_RADIUS_ + (STROKE_SIZE / 2.0));
    int rectHeight = rectBot - rectTop;
    Rectangle rect = new Rectangle(rectLeft, rectTop, rectWidth, rectHeight);
    preCache.add(new PaintedPath(col, rect));
    return;
  }
  
  /***************************************************************************
  **
  ** Draw Object
  */  
  
  private class PaintedPath {
    Color col;
    Line2D path;
    GeneralPath gp;
    int px;
    int py;
    Rectangle2D circ;
    Rectangle2D circ2;
    String name;
    float nameX;
    float nameY;
    float tnameX;
    float tnameY;
    boolean doRotateName;
    Rectangle2D nameRect;
    Rectangle2D dumpRect;
    MinMax range;
    int font;
    Rectangle rect;
    
    //
    // Used for line drawing:
    //

    PaintedPath(Color col, Line2D path, int x, int y, MinMax range) {
      this.col = col;
      this.path = path;
      this.px = x;
      this.py = y;
      this.range = range;
    }
    
   //
    // Used for shadow drawing:
    //

    PaintedPath(Color col, Rectangle rect) {
      this.col = col;
      this.rect = rect;
    }
 
    //
    // Used for text drawing:
    //
    
    PaintedPath(Color col, String name, float x, float y, 
                float tx, float ty, boolean doRotateName, int font, 
                Rectangle2D nameRect, Rectangle2D dumpRect) {
      this.col = col;
      this.name = name;
      nameX = x;
      nameY = y;
      tnameX = tx;
      tnameY = ty;
      this.doRotateName = doRotateName;
      this.font = font;
      this.nameRect = nameRect;
      this.dumpRect = dumpRect;
    }  
    
    PaintedPath(Color col, Rectangle2D circ, Rectangle2D circ2) {
      this.col = col;
      this.circ = circ;
      this.circ2 = circ2;
    }   
    
     PaintedPath(Color col, Rectangle2D circ, GeneralPath circ2) {
      this.col = col;
      this.circ = circ;
      this.gp = circ2;
    }   
       
          
    private void drawRotString(Graphics2D g2, String name, double x, double y, double ptx, double pty) {
      AffineTransform sav = g2.getTransform();
      AffineTransform forRot = (AffineTransform)sav.clone();
      if ((ptx != 0.0) || (pty != 0.0)) {
        forRot.translate(ptx, pty);
      }
      forRot.rotate(-Math.PI / 2.0, x, y);
      g2.setTransform(forRot);
      g2.drawString(name, (float)x, (float)y);
      g2.setTransform(sav);
      return;
    }  
      
    int paint(Graphics2D g2, boolean doBoxes, Rectangle bounds, boolean forSelection) {
      int didPaint = 0;
      g2.setPaint(forSelection ? Color.black : col);
      if ((name != null) && (path == null)) {
        // NODE LABELS:
        if ((bounds == null) ||
            ((nameRect.getMaxX() > bounds.getMinX()) && 
             (nameRect.getMinX() < bounds.getMaxX()) && 
             (nameRect.getMaxY() > bounds.getMinY()) && 
             (nameRect.getMinY() < bounds.getMaxY()))) { 
          //g2.drawLine((int)nameRect.getMinX(), (int)nameRect.getMinY(), (int)nameRect.getMaxX(), (int)nameRect.getMaxY());     
          g2.setFont(tiny_);
          g2.drawString(name, nameX, nameY);
          didPaint++;
        }
        // DRAIN ZONES:
        if (dumpRect != null) {
          if ((bounds == null) ||
              ((dumpRect.getMaxX() > bounds.getMinX()) && 
               (dumpRect.getMinX() < bounds.getMaxX()) && 
               (dumpRect.getMaxY() > bounds.getMinY()) && 
               (dumpRect.getMinY() < bounds.getMaxY()))) {          
            //g2.drawLine((int)dumpRect.getMinX(), (int)dumpRect.getMinY(), (int)dumpRect.getMaxX(), (int)dumpRect.getMaxY());
            if (doRotateName) {
              g2.setFont(tiny_);
              drawRotString(g2, name, tnameX, tnameY, 0.0, 0.0);
              didPaint++;         
            } else {
              Font useit;
              switch (font) {
                case 0:
                  useit = huge_;
                  break;
                case 1:
                  useit = med_;
                  break;
                case 2:
                  useit = medSmall_;
                  break;
                case 3:
                  useit = small_;
                  break;
                case 4:
                  useit = tiny_;
                  break;
                default:
                  throw new IllegalArgumentException();
              }
              g2.setFont(useit);
              g2.drawString(name, tnameX, tnameY);
              didPaint++;
            }
          }
        }
      } else if (circ != null) {
        g2.setFont(tiny_);
        if ((bounds == null) ||
            ((circ.getMaxX() > bounds.getMinX()) && 
             (circ.getMinX() < bounds.getMaxX()) && 
             (circ.getMaxY() > bounds.getMinY()) && 
             (circ.getMinY() < bounds.getMaxY()))) {          
          g2.fill(circ);     
          g2.setPaint(Color.BLACK);
          g2.draw(circ);
          didPaint++;    
        }
        if (circ2 != null) {
          if ((bounds == null) ||
              ((circ2.getMaxX() > bounds.getMinX()) && 
              (circ2.getMinX() < bounds.getMaxX()) && 
              (circ2.getMaxY() > bounds.getMinY()) && 
              (circ2.getMinY() < bounds.getMaxY()))) {         
            g2.setPaint(forSelection ? Color.black : col);
            g2.fill(circ2);     
            g2.setPaint(Color.BLACK);
            g2.draw(circ2);
            didPaint++;
          }
        } else {
          Rectangle arrow = gp.getBounds();
          if ((bounds == null) ||
              ((arrow.getMaxX() > bounds.getMinX()) && 
               (arrow.getMinX() < bounds.getMaxX()) && 
               (arrow.getMaxY() > bounds.getMinY()) && 
               (arrow.getMinY() < bounds.getMaxY()))) {         
            g2.setPaint(forSelection ? Color.black : col);
            g2.fill(gp);     
            g2.setPaint(Color.BLACK);
            g2.draw(gp);
            didPaint++;
          }
        }
      } else if (rect != null) {
        g2.setPaint(col);
        g2.fill(rect);
      } else {        
        if (px == Integer.MIN_VALUE) { // Horiz line         
          if ((bounds == null) || ((py > bounds.y) && (py < (bounds.y + bounds.height)))) {
            if ((bounds != null) && ((range.max < bounds.x) || (range.min > (bounds.x + bounds.width)))) {
              // do nothing
            } else {
              g2.draw(path);
              didPaint++;
            }
          }
        } else if (py == Integer.MIN_VALUE) { // Vert line
          if ((bounds == null) || ((px > bounds.x) && (px < (bounds.x + bounds.width)))) { 
            if ((bounds != null) && ((range.max < bounds.y) || (range.min > (bounds.y + bounds.height)))) {
              // do nothing
            } else {
              g2.draw(path);     
              didPaint++;        
            }
          }
        }
      }
      return (didPaint);
    } 
  }  
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Floaters
  */  
  
  public static class FloaterSet {
    public Rectangle floater;
    public Rectangle tourRect;
    public Rectangle currSelRect;
    
    public FloaterSet(Rectangle floater, Rectangle tourRect, Rectangle currSelRect) {
      this.floater = floater;
      this.tourRect = tourRect;
      this.currSelRect = currSelRect;    
    }
    
    public void clear() {
      floater = null;
      tourRect = null;
      currSelRect = null;
      return;
    }
    
    public boolean isEmpty() {
      return ((floater == null) &&
              (tourRect == null) &&
              (currSelRect == null));
    }   
  } 
}
