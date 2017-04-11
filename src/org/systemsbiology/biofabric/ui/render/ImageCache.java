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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This is a cache to hold tiling images
*/

public class ImageCache {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
      
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
   
  private String cachePref_;  
  private HashMap<String, BufferedImage> imgCache_;
  private HashMap<String, String> handleToFile_;
  private ArrayList<String> queue_;
  private int nextHandle_;
  private int maxMeg_;
  private int currSize_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ImageCache(String cachePref, int maxMeg) {
    cachePref_ = cachePref;
    imgCache_ = new HashMap<String, BufferedImage>();
    handleToFile_ = new HashMap<String, String>();
    queue_ = new ArrayList<String>();
    nextHandle_ = 0;
    if (maxMeg == 0) {
      throw new IllegalArgumentException();
    }
    maxMeg_ = maxMeg * 1000000;
    currSize_ = 0;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get an image from the cache; returns null if no image
  */
  
  public BufferedImage getAnImage(String handle, BufImgStack bis) throws IOException {
    BufferedImage retval = imgCache_.get(handle);
    if (retval != null) {
      queue_.remove(handle);
      queue_.add(0, handle);
      return (retval);
    }
    
    //
    // Not in memory cache, look for file:
    //
   
    String fileName = handleToFile_.get(handle);
   
    if (fileName == null) {
      return (null);
    }
    
    //
    // File is gone (should not happen...handleToFile_ should be authoritative):
    //
    
    File holdFile = new File(fileName);
    if (!holdFile.exists()) {
      return (null);
    }
    
    //
    // Get the image in from file:
    //
    
    retval = readImageFromFile(holdFile);

    //
    // Manage in-memory cache, toss least recently used:
    //

    int sizeEst = retval.getHeight() * retval.getWidth() * 3;
    maintainSize(sizeEst, bis);
 
    //
    // Install it:
    //

    imgCache_.put(handle, retval);
    queue_.add(0, handle);
    currSize_ += sizeEst;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Cache an image, return a handle.
  */
  
  public String cacheAnImage(BufferedImage bi, BufImgStack bis) throws IOException {

    int sizeEst = bi.getHeight() * bi.getWidth() * 3;
    maintainSize(sizeEst, bis);
    
    String handle = Integer.toString(nextHandle_++);
    imgCache_.put(handle, bi); 
    queue_.add(0, handle);
    currSize_ += sizeEst;
   
    return (handle);
  }
  
  /***************************************************************************
  **
  ** Drop the image associated with the handle
  */
  
  public BufferedImage dropAnImage(String handle, BufImgStack bis) throws IOException {

    BufferedImage bye = getAnImage(handle, bis);
    if (bye == null) {
      return (null);
    }
    int byeEst = bye.getHeight() * bye.getWidth() * 3;
    currSize_ -= byeEst;
    if (currSize_ < 0) {
      currSize_ = 0;
    }
    
    queue_.remove(handle);
    imgCache_.remove(handle);
    bis.returnImage(bye);

    String fileName = handleToFile_.get(handle);
    if (fileName != null) {
      File dropFile = new File(fileName);
      if (dropFile.exists()) {
        dropFile.delete();
      }
      handleToFile_.remove(handle);
    }
    return (bye);
  }
  
  
  /***************************************************************************
  **
  ** Replace the image with the given handle.
  */
  
  public String replaceAnImage(String handle, BufferedImage bi, BufImgStack bis) throws IOException {

    BufferedImage bye = getAnImage(handle, bis);
    int byeEst = bye.getHeight() * bye.getWidth() * 3;
    currSize_ -= byeEst;
    if (currSize_ < 0) {
      currSize_ = 0;
    }
    bis.returnImage(bye);
    
    // Note above getAnImage put us at the front of the queue
        
    int sizeEst = bi.getHeight() * bi.getWidth() * 3;
    maintainSize(sizeEst, bis);    
    imgCache_.put(handle, bi); 
    currSize_ += sizeEst;
    
    String fileName = handleToFile_.get(handle);
    if (fileName != null) {
      File holdFile = new File(fileName);
      writePNGImage(bi, holdFile);
    }
    return (handle);
  }
   
  /***************************************************************************
  **
  ** Manage in-memory cache, toss least recently used
  */
  
  private void maintainSize(int sizeEst, BufImgStack bis) throws IOException {
    while (((sizeEst + currSize_) > maxMeg_) && (queue_.size() > 0)) {
      String goodBye = queue_.remove(queue_.size() - 1);
      BufferedImage bye = imgCache_.remove(goodBye);
      String fileName = handleToFile_.get(goodBye);
      if (fileName == null) {
      	System.out.println("Flushing to file cache to maintain size");
        File holdFile = getAFile();
        writePNGImage(bye, holdFile);
        handleToFile_.put(goodBye, holdFile.getAbsolutePath());
        fileCacheReport();
      }
      int byeEst = bye.getHeight() * bye.getWidth() * 3;
      currSize_ -= byeEst;
      if (currSize_ < 0) {
        currSize_ = 0;
      }
      bis.returnImage(bye);
    }
    System.out.println("Curr mem cache: " + currSize_ + " with size est " + sizeEst);
    return;
  }
  
  /***************************************************************************
  **
  ** Get temp file:
  ** On Mac 10.5.8, JDK 1.6, this seems to go into:
  ** /private/var/folders/[2 characters]/[Random string of characters]/-Tmp-
  ** Note: To cd into -Tmp-, use "cd -- -Tmp-"
  */
  
  private File getAFile() throws IOException {
    File file = null;
    File dir = null;
    if (cachePref_ != null) {
      dir = new File(cachePref_);
      if (!dir.exists() || !dir.isDirectory()) {
        throw new IOException();
      }
      file = File.createTempFile("BioFabric", ".png", dir);
    } else {
      file = File.createTempFile("BioFabric", ".png");
    }
    file.deleteOnExit();
    return (file);
  }

  /***************************************************************************
  ** 
  ** Stats on image file cache:
  */
  
  public long fileCacheReport() {
     
  	long length = 0;
  	int numFile = 0;
   
    for (String holdFilePath : handleToFile_.values()) {    
      File holdFile = new File(holdFilePath);
      if (!holdFile.exists()) {
        continue;
      }
      if (numFile == 0) {
        System.out.println("Directory: " + holdFile.getParentFile().getAbsolutePath());
      }
  	  length += holdFile.length();
  	  numFile++;
    }
    System.out.println("Total bytes used: " + length);
    System.out.println("Total file count: " + numFile);
    return (length);
  }

  /***************************************************************************
  ** 
  ** Write out an PNG image
  */

  public static void writePNGImage(BufferedImage bi, File file) throws IOException {
  	long prewrite = Runtime.getRuntime().freeMemory();
    Iterator writers = ImageIO.getImageWritersByFormatName("png");
    ImageWriter writer = (ImageWriter)writers.next();
    ImageOutputStream ios = ImageIO.createImageOutputStream(file);
    writer.setOutput(ios);
    ImageWriteParam param = writer.getDefaultWriteParam();
    ImageTypeSpecifier its = new ImageTypeSpecifier(bi);
    IIOMetadata meta = writer.getDefaultImageMetadata(its, param);
    meta.getAsTree(meta.getNativeMetadataFormatName());
    IIOImage img = new IIOImage(bi, null, meta);
    writer.write(writer.getDefaultStreamMetadata(param), img, param);
    ios.close();
    writer.dispose();
    System.out.println("done writePNGImage " + file.getName() + " used " + (prewrite - Runtime.getRuntime().freeMemory()));
    return;
  }
  
 /***************************************************************************
  ** 
  ** Read in an image.
  */

  private BufferedImage readImageFromFile(File readFile) throws IOException { 
  	UiUtil.fixMePrintout("Have intermediate step of writing to bytes!");
    FileInputStream fis = new FileInputStream(readFile);
    ImageInputStream iis = ImageIO.createImageInputStream(fis);
    Iterator readers = ImageIO.getImageReaders(iis);
    if (!readers.hasNext()) {     
      throw new IOException();
    }
    BufferedImage retval = ImageIO.read(iis);
    // At first blush, it would seem that an iis.close() call here would
    // be the answer to issue #13, but that throws an IOException that the
    // stream is already closed! Reading the javadocs, it turns out that the
    // stream is closed UNLESS it returns null.
    if (retval == null) {
      iis.close();
    }
    return (retval);
  } 
}

