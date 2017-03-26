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

package org.systemsbiology.biofabric.ui.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.BioFabricNetwork.LinkInfo;
import org.systemsbiology.biofabric.model.BioFabricNetwork.NodeInfo;
import org.systemsbiology.biofabric.ui.BasicZoomTargetSupport;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.render.BufBuildDrawer;
import org.systemsbiology.biofabric.util.MinMax;

/****************************************************************************
**
** This is a fast network image builder for big networks.
*/

public class BucketRenderer implements BufBuildDrawer {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private List<BioFabricNetwork.NodeInfo> targetList_;
  private List<BioFabricNetwork.LinkInfo> linkList_;
  
  private Dimension screenDim_;
  private Dimension worldDim_;  
  private boolean showShadows_;

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BucketRenderer(FabricColorGenerator colGen) {

    targetList_ = new ArrayList<NodeInfo>();
    linkList_ = new ArrayList<LinkInfo>();
    worldDim_ = new Dimension(100, 100);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get the dimensions for the buffer
  */
  
   public void dimsForBuf(Dimension screenDim, Dimension worldDim) {
     screenDim.setSize(screenDim_);
     worldDim.setSize(worldDim_);    
     return;
   }

  /***************************************************************************
  ** 
  ** Install a model
  */
   
  public void buildBucketCache(List<BioFabricNetwork.NodeInfo> targets, 
  		                         List<BioFabricNetwork.LinkInfo> links, boolean showShadows, 
  		                         Dimension screenDim, Dimension worldDim) { 
 
  	targetList_ = targets;
    linkList_ = links;
    showShadows_ = showShadows;
    screenDim_ = screenDim;
    worldDim_ = worldDim;
    return;
  }
 
  /***************************************************************************
  **
  ** Drawing core
  */
  
  public boolean drawForBuffer(BufferedImage bi, Rectangle clip, Dimension screenDim, 
  		                         Rectangle worldRec, int heightPad, double linksPerPixel) {
  	
  	int imgHeight = bi.getHeight();
    int imgWidth = bi.getWidth();
  	Graphics2D g2 = bi.createGraphics();
  	g2.setPaint(Color.WHITE);
    g2.fillRect(0, 0, imgWidth, imgHeight);
 
    BufferedImage bin = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    drawNodesForBuffer(bin, clip, screenDim, worldRec, heightPad, linksPerPixel);
    g2.drawImage(bin, 0, 0, null);
  	BufferedImage bil = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
  	drawLinksForBuffer(bil, clip, screenDim, worldRec, heightPad, linksPerPixel);
  	g2.drawImage(bil, 0, 0, null);
  	g2.dispose();
  	
  	return (true);
  }
  
 
  /***************************************************************************
  **
  ** Drawing core
  */
  
  private boolean drawLinksForBuffer(BufferedImage bi, Rectangle clip, Dimension screenDim, Rectangle worldRec, int heightPad, double lpp) {
  	
  	int imgHeight = bi.getHeight();
    int imgWidth = bi.getWidth();
	
  	System.out.println("Clip " + clip);
    double zoomH = screenDim.getWidth() / worldRec.getWidth();
    double zoomV = screenDim.getHeight() / worldRec.getHeight();
    double zoom = Math.max(zoomH, zoomV);
    Point2D centerW = new Point2D.Double(worldRec.getX() + (worldRec.getWidth() / 2.0), worldRec.getY() + (worldRec.getHeight() / 2.0));
    AffineTransform transform = new AffineTransform();
    transform.translate(screenDim.getWidth() / 2.0, screenDim.getHeight() / 2.0);
    transform.scale(zoom, zoom);
    transform.translate(-centerW.getX(), -centerW.getY());
      
    int scrnHeight = screenDim.height;
    int scrnWidth = screenDim.width;
     
    int bufLen = scrnHeight * scrnWidth;
    
    Point2D newPoint = new Point2D.Double();
    int[] mybuf = new int[bufLen];
    System.out.println("mbl " + mybuf.length + " " + screenDim + " " + imgHeight + " " + imgWidth);
    
    Point pts = new Point();
    Point pte = new Point();    
    Point startPoint = new Point();
	  Point endPoint = new Point();
	  
	  startPoint.setLocation(worldRec.getX(), worldRec.getY());
	  Point ulInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	  int bufOffset = (ulInV.x * scrnHeight) + ulInV.y;	  
	  
    for (BioFabricNetwork.LinkInfo lif : linkList_) {

	    int yStrt = lif.topRow() * BioFabricPanel.GRID_SIZE;
	    int yEnd = lif.bottomRow() * BioFabricPanel.GRID_SIZE;
	    int x = lif.getUseColumn(showShadows_) * BioFabricPanel.GRID_SIZE;
	    if ((x < clip.x) || (x > (clip.x + clip.width))) {
	    	continue;
	    }
	    if (yStrt < clip.y) {
	    	yStrt = clip.y;
	    }
	    if (yStrt > (clip.y + clip.height)) {
	    	yStrt = clip.y + clip.height;
	    }
	    if (yEnd < clip.y) {
	    	yEnd = clip.y;
	    }
	    if (yEnd > (clip.y + clip.height)) {
	    	yEnd = clip.y + clip.height;
	    }
	
	    startPoint.setLocation(x, yStrt);
	    endPoint.setLocation(x, yEnd);
	    
	    Point startPtInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	    Point endPtInV = BasicZoomTargetSupport.pointToViewport(endPoint, transform, newPoint, pte);
	
	    int bufStart = (startPtInV.x * scrnHeight) + startPtInV.y - bufOffset;
	    int bufEnd = (endPtInV.x * scrnHeight) + endPtInV.y - bufOffset;
	       
	    for (int i = bufStart; i < bufEnd; i++) {
	    	int off = i;
	    	if (off < 0) {
	    		continue;
	    	}
	    	if (off >= mybuf.length) {
          continue;	
	    	}
	    	mybuf[off] += 1;    	
	    }
    }
    
     // link color: 130 122 128
    
    for (int i = 0; i < bufLen; i++) {
    	double val = mybuf[i] / lpp;
    	int red = 255 - Math.min(125, (int)Math.round(val * 125.0));
    	int green = 255 - Math.min(133, (int)Math.round(val * 133.0));
    	int blue = 255 - Math.min(127, (int)Math.round(val * 127.0));
    	int grey = 255 - Math.min(255, (int)Math.round(val * 255.0));
      int rgb = 128 << 24 | red << 16 | green << 8 | blue;
      
    	int xval = i / scrnHeight;
    	
    	if (xval >= imgWidth) {
    		continue;
    	}
    	int yval = i % scrnHeight;
    	if (yval >= imgHeight) {
    		continue;
    	}
      bi.setRGB(xval, yval, rgb);
	  }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Drawing core
  */
  
  private boolean drawNodesForBuffer(BufferedImage bi, Rectangle clip, Dimension screenDim, Rectangle worldRec, int heightPad, double lpp) {
  
  	int imgHeight = bi.getHeight();
    int imgWidth = bi.getWidth();
  	
    double zoomH = screenDim.getWidth() / worldRec.getWidth();
    double zoomV = screenDim.getHeight() / worldRec.getHeight();
    double zoom = Math.max(zoomH, zoomV);
    Point2D centerW = new Point2D.Double(worldRec.getX() + (worldRec.getWidth() / 2.0), worldRec.getY() + (worldRec.getHeight() / 2.0));
    AffineTransform transform = new AffineTransform();
    transform.translate(screenDim.getWidth() / 2.0, screenDim.getHeight() / 2.0);
    transform.scale(zoom, zoom);
    transform.translate(-centerW.getX(), -centerW.getY());
    
    double linksPerPix = 1.0 / (BioFabricPanel.GRID_SIZE * zoom);
    System.out.println("lpp " + linksPerPix);
     
      
    int scrnHeight = screenDim.height;
    int scrnWidth = screenDim.width;
     
    int bufLen = scrnHeight * scrnWidth;
    
    Point2D newPoint = new Point2D.Double();
    int[] mybuf = new int[bufLen];
    System.out.println("mbl " + mybuf.length + " " + screenDim + " " + imgHeight + " " + imgWidth);
    
    Point pts = new Point();
    Point pte = new Point();    
    Point startPoint = new Point();
	  Point endPoint = new Point();
	  
	  startPoint.setLocation(worldRec.getX(), worldRec.getY());
	  Point ulInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	  int bufOffset = (ulInV.y * scrnWidth) + ulInV.x;	  
	  
    for (BioFabricNetwork.NodeInfo nif : targetList_) {

	    MinMax colRange = nif.getColRange(showShadows_);
	    				
	    int xStrt = colRange.min * BioFabricPanel.GRID_SIZE;
	    int xEnd = colRange.max * BioFabricPanel.GRID_SIZE;
	    int y = nif.nodeRow * BioFabricPanel.GRID_SIZE;
	    if ((y < clip.y) || (y > (clip.y + clip.height))) {
	    	continue;
	    }
	    if (xStrt < clip.x) {
	    	xStrt = clip.x;
	    }
	    if (xStrt > (clip.x + clip.width)) {
	    	xStrt = clip.x + clip.width;
	    }
	    if (xEnd < clip.x) {
	    	xEnd = clip.x;
	    }
	    if (xEnd > (clip.x + clip.width)) {
	    	xEnd = clip.x + clip.width;
	    }
	
	    startPoint.setLocation(xStrt, y);
	    endPoint.setLocation(xEnd, y);
	    
	    Point startPtInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	    Point endPtInV = BasicZoomTargetSupport.pointToViewport(endPoint, transform, newPoint, pte);
	
	    int bufStart = (startPtInV.y * scrnWidth) + startPtInV.x - bufOffset;
	    int bufEnd = (endPtInV.y * scrnWidth) + endPtInV.x - bufOffset;
	       
	    for (int i = bufStart; i < bufEnd; i++) {
	    	int off = i;
	    	if (off < 0) {
	    		continue;
	    	}
	    	if (off >= mybuf.length) {
          continue;	
	    	}
	    	mybuf[off] += 1;    	
	    }
    }
    
    // Node color: 187 177 175
    
    for (int i = 0; i < bufLen; i++) {
    	double val = mybuf[i] / lpp;
    	int red = 255 - Math.min(68, (int)Math.round(val * 68.0));
    	int green = 255 - Math.min(78, (int)Math.round(val * 78.0));
    	int blue = 255 - Math.min(80, (int)Math.round(val * 80.0));
    	
    	
    	int grey = 255 - Math.min(255, (int)Math.round(val * 255.0));
      int rgb = 128 << 24 | red << 16 | green << 8 | blue;
    //  mybuf[i] = rgb;
    	int yval = i / scrnWidth;
    	
    	if (yval >= imgHeight) {
    		continue;
    	}
    	int xval = i % scrnWidth;
    	if (xval >= imgWidth) {
    		continue;
    	}
      bi.setRGB(xval, yval, rgb);
	  }
    // Use this instead:
    // int[] a = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
    // System.arraycopy(mybuf, 0, a, 0, Math.min(mybuf.length, a.length));
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

}
