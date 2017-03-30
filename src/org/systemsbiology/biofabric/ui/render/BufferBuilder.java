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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.QuadTree;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.QuadTree.QuadTreeNode;

/****************************************************************************
**
** This builds and manages image chunks
*/

public class BufferBuilder {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  //
  // Tweak the slice height up by one, else rounding/trunc errors introduce white
  // gridding on the image mosaic!
  //
  
  private static final int SLICE_HEIGHT_HACK_ = 1;
  
  //
  // At what level of links per pixel to we transition from bin renderer to drawing renderer?
  //
  
  private static final double TRANSITION_LPP_ = 20.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  //
  // For mapping of selections:
  //
  
  private ImageCache cache_;
 // private HashMap<Integer, Map<Rectangle, WorldPieceOffering>> worldToImageName_;
  private HashMap<Rectangle2D, WorldPieceOffering> allWorldsToImageName_;
  private QuadTree findWorldsQT_;
  private BufBuildDrawer drawRender_;
  private BufBuildDrawer binRender_;
  private int[] bbZooms_;
  private Dimension screenDim_;
  private Rectangle2D worldRect_;
  private BufferBuilderClient bbc_;
  private boolean timeToExit_;
  private BuildImageWorker biw_;

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BufferBuilder(String cachePref, int maxMeg, BufBuildDrawer drawRender, BufBuildDrawer binRender) {
    cache_ = new ImageCache(cachePref, maxMeg);
    allWorldsToImageName_ = new HashMap<Rectangle2D, WorldPieceOffering>();
    findWorldsQT_ = null;
    drawRender_ = drawRender;
    binRender_ = binRender;
    bbc_ = null;
    timeToExit_ = false;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public BufferBuilder(BufBuildDrawer drawRender, BufBuildDrawer binRender) {
    drawRender_ = drawRender;
    binRender_ = binRender;
    allWorldsToImageName_ = new HashMap<Rectangle2D, WorldPieceOffering>();
    findWorldsQT_ = null;
    bbc_ = null;
    timeToExit_ = false;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Kill it off
  */
  
  public void release() {
    synchronized (this) {
      timeToExit_ = true;
      bbc_ = null;
      this.notify();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Simple overview one-shot
  */
  
  public BufferedImage buildOneBuf(int[] zooms) {
    screenDim_ = new Dimension();
    worldRect_ = new Rectangle2D.Double();
    binRender_.dimsForBuf(screenDim_, worldRect_); // Both will give same answer... 
    Rectangle worldPiece = UiUtil.rectFromRect2D(worldRect_);
    BufferedImage bi = new BufferedImage(screenDim_.width, screenDim_.height, BufferedImage.TYPE_INT_RGB);
    double lpp = linksPerPix(screenDim_, worldPiece);
    BufBuildDrawer useDrawer = (lpp < TRANSITION_LPP_) ? drawRender_ : binRender_;
    useDrawer.drawForBuffer(bi, worldPiece, screenDim_, worldPiece, 0, lpp);  
    return (bi); 
  }
 
  /***************************************************************************
  **
  ** Get our slices in world to cover the given view in the world at the desired depth
  */
  
  public void getSlicesToCover(int depth, Rectangle2D viewInWorld, List<Rectangle2D>slicesToCover) {
  
  	ArrayList<QuadTree.QuadTreeNode> qtnList = new ArrayList<QuadTree.QuadTreeNode>();
  	findWorldsQT_.getNodes(viewInWorld, depth, qtnList);
  	int lsiz = qtnList.size();
  	for (int i = 0; i < lsiz; i++) {
  		QuadTree.QuadTreeNode qtn = qtnList.get(i);
      slicesToCover.add(qtn.getWorldExtent());
    }
  	return;
  }
  
  /***************************************************************************
  **
  **  Build all the bufs
  */
  
  public BufferedImage buildBufs(int[] zooms, BufferBuilderClient bbc, int maxSize, 
  		                           BTProgressMonitor monitor, 
                                 double startFrac, 
                                 double endFrac) throws IOException, AsynchExitRequestException {
    timeToExit_ = false;
    bbZooms_ = new int[zooms.length];
    System.arraycopy(zooms, 0, bbZooms_, 0, zooms.length);
    screenDim_ = new Dimension();
    worldRect_ = new Rectangle2D.Double();
    drawRender_.dimsForBuf(screenDim_, worldRect_); // These values are now ours
    Rectangle worldPiece = UiUtil.rectFromRect2D(worldRect_);
     
    double inc = (endFrac - startFrac) / ((zooms.length == 0) ? 1 : zooms.length);
    double currProg = startFrac;
    
    findWorldsQT_ = new QuadTree(worldPiece, zooms.length);
         
    //
    // Build the first two zoom levels before we even get started:
    //
    
    List<QueueRequest> requestQueuePre = buildQueue(0, 1, 10);   
    while (!requestQueuePre.isEmpty()) {
      QueueRequest qr = requestQueuePre.remove(0);
      buildBuffer(new Dimension(qr.imageDim.width, qr.imageDim.height), qr);
    }
    
    //
    // Now build up the requests for the background thread:
    //   
    
    List<QueueRequest> requestQueue = (zooms.length > 2) ? buildQueue(2, zooms.length - 1, maxSize) : new ArrayList<QueueRequest>();
 
    bbc_ = bbc;
 
    if (!requestQueue.isEmpty()) {
      biw_ = new BuildImageWorker(screenDim_, requestQueue);
      Thread runThread = new Thread(biw_);
      runThread.setPriority(runThread.getPriority() - 2);
      runThread.start();
    }
    return (getTopImage()); 
  }

  /***************************************************************************
  **
  ** Get the top buffered image
  */
  
  public BufferedImage getTopImage() throws IOException {
  	ArrayList<QuadTree.QuadTreeNode> nodes = new ArrayList<QuadTree.QuadTreeNode>();
  	boolean found = findWorldsQT_.getAllNodesToDepth(0, 0, nodes);
  	if (!found || (nodes.size() != 1)) {
  		throw new IOException();
  	}
    WorldPieceOffering wpo = this.allWorldsToImageName_.get(nodes.get(0).getWorldExtent());
    BufferedImage retval = null;
    synchronized (this) {
      retval = cache_.getAnImage(wpo.cacheHandle);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get an image at the given depth
  */
  
  public BufferedImage getImageForPiece(int depth, Rectangle2D worldRect) throws IOException {
  	WorldPieceOffering wpo = allWorldsToImageName_.get(worldRect);
  	if (wpo == null) {
  		wpo = new WorldPieceOffering(null, screenDim_, worldRect, false);
  		allWorldsToImageName_.put(worldRect, wpo);;
  	//	System.out.println("GIFP miss " + worldRect);
  	} else {
  //		System.out.println("GIFP hit " + worldRect);
  	}
  	boolean needLoRes = false;
    synchronized (this) {  	
      needLoRes = (wpo.cacheHandle == null);
    }
    // Yeah, this could be stale. Not the end of the world though, and we are not going to
    // hold the lock until the lo-res slice is built...
    if (needLoRes) {  	
      buildLoResSlice(worldRect, wpo);
      if (biw_ != null) {
        biw_.bumpRequest(new QueueRequest(depth, screenDim_, worldRect));
      }
    }
    BufferedImage retval = null;
    // This stalls a lot on big images, perhaps while files are being written out?
    synchronized (this) {
      if ((wpo.cacheHandle != null) && !wpo.cacheHandle.equals("")) {
        retval = cache_.getAnImage(wpo.cacheHandle);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** generate piece map
  
  
  private boolean buildPieceMap(int zoomNum, Dimension baseImageDim, Dimension worldDim, boolean force, Map<Rectangle, WorldPieceOffering> worldForSize) {
    //
    // On very large, very high aspect ratio networks, we can get cases where the height of the piece is zero
    // pixels. Don't let this happen. Also seeing on tiny networks.
    //
    boolean wIsMin = false;
    boolean hIsMin = false;
 
    int useWidth = baseImageDim.width;
    int useHeight = baseImageDim.height;

    int effectiveZoomNumX = zoomNum;
    int worldWInc = worldDim.width / zoomNum;
    if ((worldWInc < MIN_DIM_) && !force) {
    	useWidth = (int)Math.ceil(baseImageDim.width * ((double)MIN_DIM_ / worldWInc));
      worldWInc = MIN_DIM_;
      effectiveZoomNumX = (int)Math.ceil((double)worldDim.width / worldWInc);
      wIsMin = true;   
    }
    
    int effectiveZoomNumY = zoomNum;
    int worldHInc = worldDim.height / zoomNum;
    if ((worldHInc < MIN_DIM_) && !force) {
    	useHeight = (int)Math.ceil(baseImageDim.height * ((double)MIN_DIM_ / worldHInc));
      worldHInc = MIN_DIM_;
      effectiveZoomNumY = (int)Math.ceil((double)worldDim.height / worldHInc);
      hIsMin = true;    
    }

    for (int x = 0; x < effectiveZoomNumX; x++) {
      for (int y = 0; y < effectiveZoomNumY; y++) {
        Rectangle worldPiece = new Rectangle(-200 + (x * worldWInc), -200 + (y * worldHInc), worldWInc, worldHInc);
        worldForSize.put(worldPiece, new WorldPieceOffering(null, node.getViewExtent(), worldExtent, false));        
      }
    }
    return (wIsMin && hIsMin);
  }
 
  /***************************************************************************
  **
  ** Build the request queue
  */
  
  private List<QueueRequest> buildQueue(int startIndex, int endIndex, int maxCount)  {
    ArrayList<QueueRequest> retval = new ArrayList<QueueRequest>();
    ArrayList<QuadTree.QuadTreeNode> nodes = new ArrayList<QuadTree.QuadTreeNode>();
    findWorldsQT_.getAllNodesToDepth(startIndex, endIndex, nodes);
    int numWorlds = nodes.size();
    for (int i = 0; i < numWorlds; i++) {
    	QuadTree.QuadTreeNode node = nodes.get(i);
    	Rectangle2D worldExtent = node.getWorldExtent();
      WorldPieceOffering wpo = allWorldsToImageName_.get(worldExtent);
      if (wpo != null) {
      	throw new IllegalStateException();
      }
      wpo = new WorldPieceOffering(null, screenDim_, worldExtent, false);
      allWorldsToImageName_.put(worldExtent, wpo);        
      retval.add(new QueueRequest(node.getDepth(), screenDim_, wpo.worldRect));
      if (retval.size() >= maxCount) {
        return (retval);
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Build the buffers
  */
  
  private boolean buildBuffer(Dimension imageDim, QueueRequest qr) throws IOException {     
    //
    // To chunk the image, we parcel out pieces of world to pieces of screen:
    //
    synchronized (this) {
      if (timeToExit_) {
        return (false);
      }
    }
    buildHiResSlice(imageDim, qr.depth, qr.worldPiece);     
    return (true);
  }  
   
  /***************************************************************************
  **
  ** If we lack an image slice, we first create a lo-res version from existing images.
  ** 
  */
  
  private String buildLoResSlice(Rectangle2D worldRect, WorldPieceOffering wpo) throws IOException {
     
  //  int worldHInc = worldDim.height / num;
 //   int worldWInc = worldDim.width / num;
  //  int useNum = bbZooms_[1];
  //  int useIndex = 1; 
  //  int scale = num / useNum;
  //  int screenHInc = screenDim.height / scale;
  //  int screenWInc = screenDim.width / scale;
  	
    ArrayList<QuadTree.QuadTreeNode> path = new  ArrayList<QuadTree.QuadTreeNode>() ;  
    BufferedImage bi1 = null;
  //  Rectangle usePidece = null;

    if (!findWorldsQT_.getPath(worldRect, path)) {
    	throw new IllegalStateException();
    }
    
    //
    // Crank backwards up the chain to find an image to use:
    //
    
    WorldPieceOffering wpou = null;
    int pathLen = path.size();
    for (int i = pathLen - 1; i >= 0; i--) {
    	QuadTree.QuadTreeNode node = path.get(i);
      synchronized (this) {
        wpou = allWorldsToImageName_.get(node.getWorldExtent());  
        if ((wpou != null) && (wpou.cacheHandle != null) && !wpou.cacheHandle.equals("")) {
          bi1 = cache_.getAnImage(wpou.cacheHandle);
          System.out.println("Found image at i " + i);
          break;
        }
      }
      
    }

    if (bi1 == null) {  // blank!
      return (null);
    }
/*
    int xInc = (worldPiece.x - usePiece.x + 10) / worldWInc;  // FIX !  Too sensitive to rounding errors: 10 is temp fix
    int yInc = ((worldPiece.y - usePiece.y) + 10) / worldHInc;

    int topWidth = bi1.getWidth() - (xInc * screenWInc);
    int useWidth = (topWidth < screenWInc) ? topWidth : screenWInc;
    int topHeight = bi1.getHeight() - (yInc * screenHInc);
    int useHeight = (topHeight < screenHInc) ? topHeight : screenHInc;
    if ((useHeight <= 0) || (useWidth <= 0)) {
      // REALLY thin slices (1.25 million links) have round/trunc errors taking height to 0!
      double screenHIncd = (double)screenDim.height / (double)scale;
      double yIncd = (double)((worldPiece.y - usePiece.y) + 10) / (double)worldHInc;
      double topHeightd = bi1.getHeight() - (yIncd * screenHInc);
      double useHeightd = (topHeightd < screenHIncd) ? topHeightd : screenHIncd;
      
      double screenWIncd = (double)screenDim.width / (double)scale;
      double xIncd = (double)((worldPiece.x - usePiece.x) + 10) / (double)worldWInc;
      double topWidthd = bi1.getWidth() - (xIncd * screenWInc);
      double useWidthd = (topWidthd < screenWIncd) ? topWidthd : screenWIncd;
             
      if ((useHeight <= 0) && (useHeightd > 0.0)) {
        useHeight = 1;
      }
      if ((useWidth <= 0) && (useWidthd > 0.0)) {
        useWidth = 1;
      }     
      if ((useHeight <= 0) || (useWidth <= 0)) {
        return (null);
      }
    }
    */
    //
    // The piece of the image we use depends on how the target world rect fits inside the
    // world rect of the image we are using:
    //
    
    double subxFrac = (worldRect.getX() - wpou.worldRect.getX()) / wpou.worldRect.getWidth();
    int subxLoc = (int)Math.round(subxFrac * screenDim_.getWidth());
    double subyFrac = (worldRect.getY() - wpou.worldRect.getY()) / wpou.worldRect.getHeight();
    int subyLoc = (int)Math.round(subyFrac * screenDim_.getHeight());
    int subW = (int)Math.round((worldRect.getWidth() / wpou.worldRect.getWidth()) * bi1.getWidth());
    int subH = (int)Math.round((worldRect.getHeight() / wpou.worldRect.getHeight()) * bi1.getHeight());
    
    
  
   // UiUtil.fixMePrintout("Crappy lo-res segmentation occurs here!");
    
    BufferedImage chunk = bi1.getSubimage(subxLoc, subyLoc, subW, subH);
    BufferedImage scaled = new BufferedImage(screenDim_.width, screenDim_.height + SLICE_HEIGHT_HACK_, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = scaled.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2.drawImage(chunk, 0, 0, screenDim_.width, screenDim_.height + SLICE_HEIGHT_HACK_, null);
    g2.dispose();
    
    String handle = null;
    synchronized (this) {
      if (!wpo.isDrawn) {
        if (!isBlankImage(scaled)) {      
          wpo.cacheHandle = cache_.cacheAnImage(scaled);
        } else {
          wpo.cacheHandle = "";
        }
      }
    }
    return (handle);
  }
 
  /***************************************************************************
  **
  ** How many links per pix for this current zoom?
  */
  
  private double linksPerPix(Dimension imageDim, Rectangle2D worldPiece) { 
    double zoom = Math.max(imageDim.getWidth() / worldPiece.getWidth(), imageDim.getHeight() / worldPiece.getHeight());
    double linksPerPix = 1.0 / (BioFabricPanel.GRID_SIZE * zoom);
    return (linksPerPix);
  }
  
  /***************************************************************************
  **
  ** While the lo-res slice is being displayed, we go out and render to a high-res
  ** slice that is cached:
  */
  
  private void buildHiResSlice(Dimension imageDim, int depth, Rectangle2D worldPiece) throws IOException {
    BufferedImage bi = new BufferedImage(imageDim.width, imageDim.height + SLICE_HEIGHT_HACK_, BufferedImage.TYPE_INT_RGB);

    double lpp = linksPerPix(imageDim, worldPiece);
    BufBuildDrawer useDrawer = (lpp < TRANSITION_LPP_) ? drawRender_ : binRender_;
   
    boolean didDraw = useDrawer.drawForBuffer(bi, worldPiece, imageDim, worldPiece, SLICE_HEIGHT_HACK_, lpp);
  
    BufferBuilderClient tellHim = null;
    synchronized (this) {
    	WorldPieceOffering wpo = allWorldsToImageName_.get(worldPiece);
      if (didDraw) {
        if ((wpo.cacheHandle == null) || wpo.cacheHandle.equals("")) {
          wpo.cacheHandle = cache_.cacheAnImage(bi);
        } else {
          cache_.replaceAnImage(wpo.cacheHandle, bi);
        }
        wpo.isDrawn = true;
      } else {  // nothing drawn
        if (wpo.cacheHandle == null) {
          wpo.cacheHandle = "";
        } else if (!wpo.cacheHandle.equals("")) {
          cache_.dropAnImage(wpo.cacheHandle);
        }
      }
      if (bbc_ != null) {
        tellHim = bbc_;
      }
    }
    
    if (tellHim != null) {
      final int noteKey = depth;
      final BufferBuilderClient fth = tellHim;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fth.yourOrderIsReady(noteKey);
        }
      });
    }
    bi = null;
    return;
  }
 
  /***************************************************************************
  **
  ** Tell us if the image is all white:
  */

  private boolean isBlankImage(BufferedImage bi) {
    int width = bi.getWidth();
    int height = bi.getHeight();
    int[] pixels = new int[width * height];
    PixelGrabber pg = new PixelGrabber(bi, 0, 0, width, height, pixels, 0, width);
    try {
      pg.grabPixels();
    } catch (InterruptedException ex) {
      throw new IllegalStateException();
    }
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        if (!parsePixel(i, j, pixels[j * width + i])) {
          return (false);
        }
      }
    }
    return (true);
  }

  private boolean parsePixel(int x, int y, int pixel) {
     //int alpha = (pixel >> 24) & 0xff;
     int red   = (pixel >> 16) & 0xff;
     int green = (pixel >>  8) & 0xff;
     int blue  = (pixel) & 0xff;
     return ((red == 255) && (green == 255) && (blue == 255));       
  }

  /***************************************************************************
  **
  ** We only do image-based zooms if we need to, and switch over to actual drawing
  ** when the link count in the frame gets small enough. Note that right now, that
  ** cross-over is set to 10,000 links:
  */ 

  public static int[] calcImageZooms(BioFabricNetwork bfn) { 

    boolean showShadows = FabricDisplayOptionsManager.getMgr().getDisplayOptions().getDisplayShadows();
    int lco = (bfn == null) ? 0 : bfn.getLinkCount(showShadows);
    int[] preZooms;
    if (lco != 0) {
      int linkLog = (int)Math.ceil(Math.log(lco) / Math.log(2.0));
      int firstDrawLog = (int)Math.ceil(Math.log(1.0E4) / Math.log(2.0));
      // For tiny networks (1 link), previous 4 levels of zoom is too much.
      int numPre = Math.max(linkLog - firstDrawLog, 2);
      preZooms = new int[numPre];
      preZooms[0] = 1;
      for (int i = 1; i < numPre; i++) {
        preZooms[i] = 2 * preZooms[i - 1];
      }
    } else {
      preZooms = new int[1];
      preZooms[0] = 1;   
    } 
    return (preZooms);
  }
  
  
  /***************************************************************************
  **
  ** Build images in the background:
  */

  public class BuildImageWorker implements Runnable {

    private Dimension screenDim_;
    @SuppressWarnings("unused")
    private String errString_;
    private ArrayList<QueueRequest> requests_;

    public BuildImageWorker(Dimension screenDim, List<QueueRequest> requests) {
      screenDim_ = (Dimension)screenDim.clone();
      requests_ = new ArrayList<QueueRequest>(requests);
    }

    public void run() {
      try {
        while (true) {
          QueueRequest qr = getNextRequest();
          if (qr == null) {
            break;
          }
          if (!buildBuffer(new Dimension(qr.imageDim.width, qr.imageDim.height), qr)) {
            return;
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
        errString_ = "IOException";
      } catch (Throwable oom) {
        String format = "Other error : {1}";
        oom.printStackTrace();
        errString_ = MessageFormat.format(format, new Object[] {oom.getMessage()}); 
      }
   //   client_.retrievedResult(theUrl_, myResult_);
      return;
    }
    
    
    private QueueRequest getNextRequest() {
      synchronized (BufferBuilder.this) {
        while (requests_.isEmpty()) {
	        try {
		        BufferBuilder.this.wait();
	        } catch (InterruptedException e) {
	        }
          if (timeToExit_) {
            return (null);
          }
	      }
        return (requests_.remove(0));     
      }
    }
       
    //
    // When a lo-res result is all we can return, push the hi-res requests to the
    // front of the queue:
    //
    
    void bumpRequest(QueueRequest qr) {
      synchronized (BufferBuilder.this) {
        requests_.remove(qr);  // May not be there!!!!
        requests_.add(0, qr);
        BufferBuilder.this.notify(); 
      }
      return;
    }
  }
    
  /***************************************************************************
  **
  ** World Piece Offering
  */  
  
  private static class WorldPieceOffering implements Cloneable {
    String cacheHandle;
    boolean isDrawn;
    Dimension imageDim;
    Rectangle2D worldRect;
    
    WorldPieceOffering(String cacheHandle, Dimension imageDim, Rectangle2D worldRect, boolean isDrawn) {
      this.cacheHandle = cacheHandle;
      this.imageDim = imageDim;
      this.worldRect = worldRect;
      this.isDrawn = isDrawn;
    }
    
    @Override
    public WorldPieceOffering clone() {
      try {
        return ((WorldPieceOffering)super.clone()); // Don't copy rects; they are not changed
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }   
  }
  
  /***************************************************************************
  **
  ** Request to draw an object
  */  
  
  private static class QueueRequest {
    int depth;
    Dimension imageDim;
    Rectangle2D worldPiece;
    
    QueueRequest(int depth, Dimension imageDim, Rectangle2D worldPiece) {
      this.depth = depth;
      this.worldPiece = worldPiece;
      this.imageDim = imageDim;
    }
    
    public int hashCode() {
      return (depth + worldPiece.hashCode() + imageDim.hashCode());
    }
      
    public boolean equals(Object other) {    
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof QueueRequest)) {
        return (false);
      }
      QueueRequest otherReq = (QueueRequest)other;

      if (this.depth != otherReq.depth) {
        return (false);
      }
      if (!this.imageDim.equals(otherReq.imageDim)) {
      	return (false);
      } 
      return (this.worldPiece.equals(otherReq.worldPiece));
    }  
    
  }
 
  /***************************************************************************
  **
  ** Interface for guys who wnat to listen for our results
  */  
  
  public interface BufferBuilderClient { 
    public void yourOrderIsReady(int sizeNum);
  } 
}
