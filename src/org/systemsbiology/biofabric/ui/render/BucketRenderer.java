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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.BioFabricNetwork.LinkInfo;
import org.systemsbiology.biofabric.model.BioFabricNetwork.NodeInfo;
import org.systemsbiology.biofabric.ui.BasicZoomTargetSupport;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
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
  private BioFabricNetwork.Extents ext_;
  
  private Dimension screenDim_;
  private boolean showShadows_;
  private Rectangle2D worldRect_;
  private ImgAndBufPool bis_;

  
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
    ext_ = new BioFabricNetwork.Extents();
    worldRect_ = new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0);
    bis_ = null;
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
  
   public void dimsForBuf(Dimension screenDim, Rectangle2D worldRect) {
     screenDim.setSize(screenDim_);
     worldRect.setRect(worldRect_);    
     return;
   }

  /***************************************************************************
  ** 
  ** Install a model
  */
   
  public void buildBucketCache(List<BioFabricNetwork.NodeInfo> targets, 
  		                         List<BioFabricNetwork.LinkInfo> links, 
  		                         BioFabricNetwork.Extents ext, boolean showShadows) { 
 
  	targetList_ = targets;
    linkList_ = links;
    ext_ = ext;
    showShadows_ = showShadows;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Install a model size
  */
   
  public void setModelDims(Dimension screenDim, Rectangle2D worldRect, ImgAndBufPool bis) { 
    screenDim_ = screenDim;
    worldRect_.setRect(worldRect);
    bis_ = bis;
    return;
  }

  /***************************************************************************
  **
  ** Drawing core
  */
  
  public boolean drawForBuffer(BufferedImage bi, Rectangle2D clip, Dimension screenDim, 
  		                         Rectangle2D worldRec, int heightPad, double linksPerPixel) {
  	
  	int imgHeight = bi.getHeight();
    int imgWidth = bi.getWidth();
  	Graphics2D g2 = bi.createGraphics();
  	g2.setPaint(Color.WHITE);
    g2.fillRect(0, 0, imgWidth, imgHeight);
 
    BufferedImage bin = bis_.fetchImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    drawNodesForBuffer(bin, clip, screenDim, worldRec, heightPad, linksPerPixel);
    g2.drawImage(bin, 0, 0, null);
  	BufferedImage bil = bis_.fetchImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
  	drawLinksForBuffer(bil, clip, screenDim, worldRec, heightPad, linksPerPixel);
  	g2.drawImage(bil, 0, 0, null);
  	g2.dispose();
  	bis_.returnImage(bin);
  	bis_.returnImage(bil);
  	
  	return (true);
  }

  /***************************************************************************
  **
  ** Drawing core
  */
  
  private boolean drawLinksForBuffer(BufferedImage bi, Rectangle2D clip, Dimension screenDim, Rectangle2D worldRec, int heightPad, double lpp) {
  	
    BufAndMeta bam = new BufAndMeta(bi, clip, screenDim, worldRec, heightPad, lpp, bis_);
    
	  bam.startPoint.setLocation(worldRec.getX(), worldRec.getY());
	  int bufOffset = (bam.ulInV.x * bam.scrnHeight) + bam.ulInV.y;	  
	  
    for (BioFabricNetwork.LinkInfo lif : linkList_) {

	    bam.yStrt = lif.topRow() * BioFabricPanel.GRID_SIZE;
	    bam.yEnd = lif.bottomRow() * BioFabricPanel.GRID_SIZE;
	    bam.x = lif.getUseColumn(showShadows_) * BioFabricPanel.GRID_SIZE;
	   	if (!bam.clipForLinks(clip)) {
	    	continue;
	    }
	    int bufStart = (bam.startPtInV.x * bam.scrnHeight) + bam.startPtInV.y - bufOffset;
	    int bufEnd = (bam.endPtInV.x * bam.scrnHeight) + bam.endPtInV.y - bufOffset;
	    
	    bam.transToBuf(bufStart, bufEnd);
    }
    
     // link color: 130 122 128
    
    for (int i = 0; i < bam.bufLen; i++) {
    	double val = bam.mybuf[i] / lpp;
    	int red = 255 - Math.min(125, (int)Math.round(val * 125.0));
    	int green = 255 - Math.min(133, (int)Math.round(val * 133.0));
    	int blue = 255 - Math.min(127, (int)Math.round(val * 127.0));
    	int grey = 255 - Math.min(255, (int)Math.round(val * 255.0));
      int rgb = 128 << 24 | red << 16 | green << 8 | blue;
      
    	int xval = i / bam.scrnHeight;
    	
    	if (xval >= bam.imgWidth) {
    		continue;
    	}
    	int yval = i % bam.scrnHeight;
    	if (yval >= bam.imgHeight) {
    		continue;
    	}
      bi.setRGB(xval, yval, rgb);
	  }
    
    bis_.returnBuf(bam.mybuf);
    return (true);
  }
 
  /***************************************************************************
  **
  ** Drawing core
  */
  
  private boolean drawNodesForBuffer(BufferedImage bi, Rectangle2D clip, Dimension screenDim, Rectangle2D worldRec, int heightPad, double lpp) {
  
  	BufAndMeta bam = new BufAndMeta(bi, clip, screenDim, worldRec, heightPad, lpp, bis_);

	  int bufOffset = (bam.ulInV.y * bam.scrnWidth) + bam.ulInV.x;	  
	  
    for (BioFabricNetwork.NodeInfo nif : targetList_) {

	    MinMax colRange = nif.getColRange(showShadows_);	    				
	    bam.xStrt = colRange.min * BioFabricPanel.GRID_SIZE;
	    bam.xEnd = colRange.max * BioFabricPanel.GRID_SIZE;
	    bam.y = nif.nodeRow * BioFabricPanel.GRID_SIZE;
	    if (!bam.clipForNodes(clip)) {
	    	continue;
	    }
	    int bufStart = (bam.startPtInV.y * bam.scrnWidth) + bam.startPtInV.x - bufOffset;
	    int bufEnd = (bam.endPtInV.y * bam.scrnWidth) + bam.endPtInV.x - bufOffset;
	       
	    bam.transToBuf(bufStart, bufEnd);
    }
    
    // Node color: 187 177 175
    
    for (int i = 0; i < bam.bufLen; i++) {
    	double val = bam.mybuf[i] / lpp;
    	int red = 255 - Math.min(68, (int)Math.round(val * 68.0));
    	int green = 255 - Math.min(78, (int)Math.round(val * 78.0));
    	int blue = 255 - Math.min(80, (int)Math.round(val * 80.0));
    	
    	
    	int grey = 255 - Math.min(255, (int)Math.round(val * 255.0));
      int rgb = 128 << 24 | red << 16 | green << 8 | blue;
    //  mybuf[i] = rgb;
    	int yval = i / bam.scrnWidth;
    	
    	if (yval >= bam.imgHeight) {
    		continue;
    	}
    	int xval = i % bam.scrnWidth;
    	if (xval >= bam.imgWidth) {
    		continue;
    	}
      bi.setRGB(xval, yval, rgb);
	  }
    // Use this instead:
    // int[] a = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
    // System.arraycopy(mybuf, 0, a, 0, Math.min(mybuf.length, a.length));
    bis_.returnBuf(bam.mybuf);
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  private static class BufAndMeta {
  	int imgHeight;
    int imgWidth;

    double zoomH;
    double zoomV;
    double zoom;
    AffineTransform transform;
     
    int scrnHeight;
    int scrnWidth;
     
    int bufLen;
    
    Point2D newPoint;
  	int[] mybuf;
	
  	Point pts;
    Point pte;    
    Point startPoint;
	  Point endPoint;
	  Point ulInV;
	  
	  Point startPtInV;
	  Point endPtInV;
	  
	  // For link drawing:
	  double yStrt;
	  double yEnd;
	  double x;
	  
	  // for node drawing:
	  double xStrt;
	  double xEnd;
	  double y;
 
	  BufAndMeta(BufferedImage bi, Rectangle2D clip, Dimension screenDim, Rectangle2D worldRec, 
	  		       int heightPad, double lpp, ImgAndBufPool bis) {
  	
	  	imgHeight = bi.getHeight();
	    imgWidth = bi.getWidth();
		
	    zoomH = screenDim.getWidth() / worldRec.getWidth();
	    zoomV = screenDim.getHeight() / worldRec.getHeight();
	    zoom = Math.max(zoomH, zoomV);
	    Point2D centerW = new Point2D.Double(worldRec.getX() + (worldRec.getWidth() / 2.0), worldRec.getY() + (worldRec.getHeight() / 2.0));
	    transform = new AffineTransform();
	    transform.translate(screenDim.getWidth() / 2.0, screenDim.getHeight() / 2.0);
	    transform.scale(zoom, zoom);
	    transform.translate(-centerW.getX(), -centerW.getY());
	      
	    scrnHeight = screenDim.height;
	    scrnWidth = screenDim.width;
	     
	    bufLen = scrnHeight * scrnWidth;
	    
	    newPoint = new Point2D.Double();
	   
	    mybuf = bis.fetchBuf(bufLen);
	    
	    pts = new Point();
	    pte = new Point();    
	    startPoint = new Point();
		  endPoint = new Point();
		  
		  startPoint.setLocation(worldRec.getX(), worldRec.getY());
		  ulInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint,pts); 
	  }
	  
	  boolean clipForLinks(Rectangle2D clip) {
	  	if ((x < clip.getX()) || (x > (clip.getX() + clip.getWidth()))) {
	    	return (false);
	    }
	    if (yStrt < clip.getY()) {
	    	yStrt = clip.getY();
	    }
	    if (yStrt > (clip.getY() + clip.getHeight())) {
	    	yStrt = clip.getY() + clip.getHeight();
	    }
	    if (yEnd < clip.getY()) {
	    	yEnd = clip.getY();
	    }
	    if (yEnd > (clip.getY() + clip.getHeight())) {
	    	yEnd = clip.getY() + clip.getHeight();
	    }
	
	    startPoint.setLocation(x, yStrt);
	    endPoint.setLocation(x, yEnd);
	        
	    startPtInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	    endPtInV = BasicZoomTargetSupport.pointToViewport(endPoint,transform, newPoint, pte);
	       
	    return (true);
	  }  
	  
	  boolean clipForNodes(Rectangle2D clip) {
	  	if ((y < clip.getY()) || (y > (clip.getY() + clip.getHeight()))) {
	    	return (false);
	    }
	    if (xStrt < clip.getX()) {
	    	xStrt = clip.getX();
	    }
	    if (xStrt > (clip.getX() + clip.getWidth())) {
	    	xStrt = clip.getX() + clip.getWidth();
	    }
	    if (xEnd < clip.getX()) {
	    	xEnd = clip.getX();
	    }
	    if (xEnd > (clip.getX() + clip.getWidth())) {
	    	xEnd = clip.getX() + clip.getWidth();
	    }   
	    startPoint.setLocation(xStrt, y);
	    endPoint.setLocation(xEnd, y);
	    
	    startPtInV = BasicZoomTargetSupport.pointToViewport(startPoint, transform, newPoint, pts);
	    endPtInV = BasicZoomTargetSupport.pointToViewport(endPoint,transform, newPoint, pte);
	       
	    return (true);
	  } 
	  
	  void transToBuf(int bufStart, int bufEnd) {
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
	  	return;
	  }
  }  
}
