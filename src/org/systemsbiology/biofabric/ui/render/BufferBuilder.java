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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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
  // Don't build slices smaller than this dimension:
  //
  
  private static final int MIN_DIM_ = 100;  
  
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
  private HashMap<Integer, Map<Rectangle, WorldPieceOffering>> worldToImageName_;
  private BufBuildDrawer drawer_;
  private int[] bbZooms_;
  private Dimension screenDim_;
  private Dimension worldDim_;
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

  public BufferBuilder(String cachePref, int maxMeg, BufBuildDrawer drawer) {
    cache_ = new ImageCache(cachePref, maxMeg);
    worldToImageName_ = new HashMap<Integer, Map<Rectangle, WorldPieceOffering>>();
    drawer_ = drawer;
    bbc_ = null;
    timeToExit_ = false;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public BufferBuilder(BufBuildDrawer drawer) {
    drawer_ = drawer;
    worldToImageName_ = new HashMap<Integer, Map<Rectangle, WorldPieceOffering>>();
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
    bbZooms_ = new int[zooms.length];
    System.arraycopy(zooms, 0, bbZooms_, 0, zooms.length);
    screenDim_ = new Dimension();
    worldDim_ = new Dimension();
    drawer_.dimsForBuf(screenDim_, worldDim_);
    HashMap<Rectangle, WorldPieceOffering> worldForSize = new HashMap<Rectangle, WorldPieceOffering>();
    Integer key = Integer.valueOf(0);
    worldToImageName_.put(key, worldForSize);
    buildPieceMap(zooms[0], worldDim_, true, worldForSize);
    Map<Rectangle, WorldPieceOffering> worldForSizex = worldToImageName_.get(key);
    Rectangle worldPiece = worldForSizex.keySet().iterator().next(); 
    BufferedImage bi = new BufferedImage(screenDim_.width, screenDim_.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = bi.createGraphics();
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, screenDim_.width, screenDim_.height);
    drawer_.drawForBuffer(g2, worldPiece, screenDim_, worldPiece);  
    g2.dispose();
    return (bi); 
  }
 
  /***************************************************************************
  **
  **  Build all the bufs
  */
  
  public BufferedImage buildBufs(int[] zooms, BufferBuilderClient bbc, int maxSize) throws IOException {
    timeToExit_ = false;
    bbZooms_ = new int[zooms.length];
    System.arraycopy(zooms, 0, bbZooms_, 0, zooms.length);
    screenDim_ = new Dimension();
    worldDim_ = new Dimension();
    drawer_.dimsForBuf(screenDim_, worldDim_);
    HashMap<Rectangle, WorldPieceOffering> limitOffering = null;
    for (int i = 0; i < zooms.length; i++) {
      //
      // We do not allow the tesselation size to drop below a minimum image dimension. If we actually hit that
      // limit for BOTH dimensions (should not happen, but...) just use the last minimum:
      if (limitOffering != null) {
        Integer key = Integer.valueOf(i);
        HashMap<Rectangle, WorldPieceOffering> worldForSize = new HashMap<Rectangle, WorldPieceOffering>();
        Iterator<Rectangle> wpoKit = limitOffering.keySet().iterator();
        while (wpoKit.hasNext()) {
          Rectangle rkey = wpoKit.next();
          WorldPieceOffering wpo = limitOffering.get(rkey);
          worldForSize.put((Rectangle)rkey.clone(), wpo.clone()); 
        }   
        worldToImageName_.put(key, worldForSize);
      } else {
        HashMap<Rectangle, WorldPieceOffering> worldForSize = new HashMap<Rectangle, WorldPieceOffering>();
        Integer key = Integer.valueOf(i);
        worldToImageName_.put(key, worldForSize);
        boolean gottaStop = buildPieceMap(zooms[i], worldDim_, false, worldForSize);
        if (gottaStop) {
          limitOffering = worldForSize;
        }
      }
    }
    
    //
    // Build the first two zoom levels before we even get started:
    //
    
    List<QueueRequest> requestQueuePre = buildQueue(0, 1, 10);   
    while (!requestQueuePre.isEmpty()) {
      QueueRequest qr = requestQueuePre.remove(0);
      buildBuffer(screenDim_, qr);
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
  ** Get all worlds for given size
  */
  
  public Iterator<Rectangle> getWorldsForSize(Integer size) {
    Map<Rectangle, WorldPieceOffering> worldForSize = worldToImageName_.get(size);
    HashSet<Rectangle> setOfKeys = new HashSet<Rectangle>(worldForSize.keySet()); 
    return (setOfKeys.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the top buffered image
  */
  
  public BufferedImage getTopImage() throws IOException {
    Map<Rectangle, WorldPieceOffering> worldForSize = worldToImageName_.get(Integer.valueOf(0));
    WorldPieceOffering wpo = worldForSize.values().iterator().next();
    BufferedImage retval = null;
    synchronized (this) {
      retval = cache_.getAnImage(wpo.cacheHandle);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Drawing routine
  */
  
  public BufferedImage getImageForSizeAndPiece(Integer size, Rectangle worldPiece) throws IOException {
    Map<Rectangle, WorldPieceOffering> worldForSize = worldToImageName_.get(size);
    WorldPieceOffering wpo = worldForSize.get(worldPiece);
    // FIX METhis is assuming that write to handle is atomic???
    if (wpo.cacheHandle == null) {
    	System.out.println("Build lo res " + size + " " + worldPiece);
      buildLoResSlice(bbZooms_[size.intValue()], screenDim_, worldDim_, worldPiece, worldForSize);
      if (biw_ != null) {
        biw_.bumpRequest(new QueueRequest(size, worldPiece));
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
  */
  
  private boolean buildPieceMap(int zoomNum, Dimension worldDim, boolean force, Map<Rectangle, WorldPieceOffering> worldForSize) {
    //
    // On very large, very high aspect ratio networks, we can get cases where the height of the piece is zero
    // pixels. Don't let this happen:
    //
    boolean wIsMin = false;
    boolean hIsMin = false;
 
    System.out.println("These resets create the tearing on the Issue17BIFasTXT.txt test file");

    int worldWInc = worldDim.width / zoomNum;
    if ((worldWInc < MIN_DIM_) && !force) {
      worldWInc = MIN_DIM_;
      wIsMin = true;     
    }
    
    int worldHInc = worldDim.height / zoomNum;
    if ((worldHInc < MIN_DIM_) && !force) {
      worldHInc = MIN_DIM_;
      hIsMin = true;
    }
     
    System.out.println("No! When floored, This creates rectangles far beyond the right/bottom end of the network. Bigger size means we don't need them all");
    System.out.println("Plus system makes images too small, since it expects increments to match zoomNum value.");

    for (int x = 0; x < zoomNum; x++) {
      for (int y = 0; y < zoomNum; y++) {
        Rectangle worldPiece = new Rectangle(-200 + (x * worldWInc), -200 + (y * worldHInc), worldWInc, worldHInc);
        worldForSize.put(worldPiece, new WorldPieceOffering(null, zoomNum, false));        
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
    for (int i = startIndex; i <= endIndex; i++) {
      Integer key = Integer.valueOf(i);       
      Map<Rectangle, WorldPieceOffering> worldForSize = worldToImageName_.get(key);
      if (worldForSize != null) {
        Iterator<Rectangle> w4sit = worldForSize.keySet().iterator();
        while (w4sit.hasNext()) {     
          Rectangle worldPiece = w4sit.next();
          retval.add(new QueueRequest(key, worldPiece));
          if (retval.size() >= maxCount) {
            return (retval);
          }
        }
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Build the buffers
  */
  
  private boolean buildBuffer(Dimension screenDim, QueueRequest qr) throws IOException {     
    //
    // To chunk the image, we parcel out pieces of world to pieces of screen:
    //
    synchronized (this) {
      if (timeToExit_) {
        return (false);
      }
    }
    buildHiResSlice(screenDim, qr.key, qr.worldPiece);     
    return (true);
  }  
   
  /***************************************************************************
  **
  ** If we lack an image slice, we first create a lo-res version from existing images.
  ** 
  */
  
  private String buildLoResSlice(int num, Dimension screenDim, Dimension worldDim, 
                                 Rectangle worldPiece, Map<Rectangle, WorldPieceOffering> worldForSize) throws IOException {
     
    int worldHInc = worldDim.height / num;
    int worldWInc = worldDim.width / num;
    int useNum = bbZooms_[1];
    int useIndex = 1; 
    int scale = num / useNum;
    int screenHInc = screenDim.height / scale;
    int screenWInc = screenDim.width / scale;
           
    BufferedImage bi1 = null;

    Rectangle usePiece = null;
    // NOTE THE USE INDEX OF 1 FOR THE IMAGE BASE!!!!  NO!!!! FIX ME!!!!
    Map<Rectangle, WorldPieceOffering> worldForUse = worldToImageName_.get(Integer.valueOf(useIndex));
    Iterator<Rectangle> wf1Kit = worldForUse.keySet().iterator(); 
    while (wf1Kit.hasNext()) {
      usePiece = wf1Kit.next();
      Rectangle2D wpit = usePiece.createIntersection(worldPiece);
      if ((wpit.getWidth() > 10.0) && (wpit.getHeight() > 10.0)) {
        synchronized (this) {
          WorldPieceOffering wpou = worldForUse.get(usePiece);
          if ((wpou.cacheHandle != null) && !wpou.cacheHandle.equals("")) {
            bi1 = cache_.getAnImage(wpou.cacheHandle);
          }
        }
        break;
      }
    }

    if (bi1 == null) {  // blank!
      return (null);
    }

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
  
    System.out.println("Crappy lo-res segmentation occurs here!");
    BufferedImage chunk = bi1.getSubimage((xInc * screenWInc), (yInc * screenHInc), useWidth, useHeight);
    BufferedImage scaled = new BufferedImage(screenDim.width, screenDim.height + SLICE_HEIGHT_HACK_, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = scaled.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2.drawImage(chunk, 0, 0, screenDim.width, screenDim.height + SLICE_HEIGHT_HACK_, null);
    g2.dispose();
    String handle = null;
    synchronized (this) {
      WorldPieceOffering wpo = worldForSize.get(worldPiece);
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
  ** While the lo-res slice is being displayed, we go out and render to a high-res
  ** slice that is cached:
  */
  
  private void buildHiResSlice(Dimension screenDim, Integer key, Rectangle worldPiece) throws IOException {
  	System.out.println("Here is the problem. If the world piece is bigger than the zoom level expects, the fixed screenDim is wrong");
  	
  	
    System.out.println("BHRS " + screenDim + " " + key + " for " + worldPiece); 
    BufferedImage bi = new BufferedImage(screenDim.width, screenDim.height + SLICE_HEIGHT_HACK_, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = bi.createGraphics();
    g2.setColor(Color.WHITE);
    g2.setColor(Color.RED); //Debug sizing problems
    g2.fillRect(0, 0, screenDim.width, screenDim.height + SLICE_HEIGHT_HACK_);
    System.out.println(screenDim);
    boolean didDraw = drawer_.drawForBuffer(g2, worldPiece, screenDim, worldPiece);
    
    // Debug sizing problems:
    AffineTransform transform = new AffineTransform();
    g2.setTransform(transform);
    BasicStroke selectedStroke = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);    
    g2.setStroke(selectedStroke);  
    g2.setColor(Color.GREEN);
    g2.drawRect(0, 0, screenDim.width - 1, screenDim.height + SLICE_HEIGHT_HACK_ - 1);
    
    
    g2.dispose();
 
    BufferBuilderClient tellHim = null;
    synchronized (this) {
      Map<Rectangle, WorldPieceOffering> worldForSize = worldToImageName_.get(key);
      WorldPieceOffering wpo = worldForSize.get(worldPiece);
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
      final Integer noteKey = key;
      final BufferBuilderClient fth = tellHim;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fth.yourOrderIsReady(noteKey.intValue());
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
      int numPre = Math.max(linkLog - firstDrawLog, 4);
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
          if (!buildBuffer(screenDim_, qr)) {
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
    @SuppressWarnings("unused")
    int zoomNum;
    
    WorldPieceOffering(String cacheHandle, int zoomNum, boolean isDrawn) {
      this.cacheHandle = cacheHandle;
      this.zoomNum = zoomNum;
      this.isDrawn = isDrawn;
    }
    
    @Override
    public WorldPieceOffering clone() {
      try {
        return ((WorldPieceOffering)super.clone());
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
    Integer key;
    Rectangle worldPiece;
    
    QueueRequest(Integer key, Rectangle worldPiece) {
      this.key = key;
      this.worldPiece = worldPiece;
    }
    
    public int hashCode() {
      return (key.hashCode() + worldPiece.hashCode());
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

      if (!this.key.equals(otherReq.key)) {
        return (false);
      }
      return (this.worldPiece.equals(otherReq.worldPiece));
    }  
    
  }
  
  /***************************************************************************
  **
  ** Draw Object
  */  
  
  public interface BufferBuilderClient { 
    public void yourOrderIsReady(int sizeNum);
  }
}
