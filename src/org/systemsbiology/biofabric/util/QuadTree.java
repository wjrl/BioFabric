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

package org.systemsbiology.biofabric.util;

import java.awt.Rectangle;
import java.util.List;

/***************************************************************************
**
** Class for fast access to tiles that display a rectangle
*/

public class QuadTree {
  
  private int maxDepth_;
  private QuadTreeNode root_;
  
  public QuadTree(int minDim, Rectangle extent, int maxDepth) {
  	maxDepth_ = maxDepth;
  	root_ = new QuadTreeNode(minDim, extent);
  } 
   
  /***************************************************************************
	**
	** Given a rectangle and a depth, fill in the children at the depth that
	** contain the rectangle. Returns true if anything found. 
	*/
	  
  public boolean getNodes(Rectangle rect, int atDepth, List<QuadTreeNode> nodes) {
    if (atDepth >= maxDepth_) {
    	throw new IllegalArgumentException();
    }
  	return (root_.getNodes(rect, atDepth, nodes));
  }
  
  /***************************************************************************
	**
	** Given bounding depths, fill in the children between those depths
	** Returns true if anything found. 
	*/
	  
  public boolean getAllNodesToDepth(int minDepth, int maxDepth, List<QuadTreeNode> nodes) {
    if (maxDepth > maxDepth_) {
    	throw new IllegalArgumentException();
    }
  	return (root_.getAllNodesToDepth(minDepth, maxDepth, nodes));
  }

  /***************************************************************************
  **
  ** Node payloads
  */
  
  public interface Payload {
  }
  
  /***************************************************************************
  **
  ** Nodes of the tree
  */
  
  public static class QuadTreeNode {
    	
  	enum Corner {SINGLETON, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT}
  	
  	private int depth_;
  	private int minDim_;
  	private boolean minWidth_;
  	private boolean minHeight_;
  	private Rectangle extent_;
  	private Payload payLoad_;
  	private Corner corner_;
    private QuadTreeNode parent_;
    private boolean needKidInit_;
    private QuadTreeNode ulKid_;
    private QuadTreeNode urKid_;
    private QuadTreeNode llKid_;
    private QuadTreeNode lrKid_;
    
    /***************************************************************************
	  **
	  ** Build the root node
	  */

    QuadTreeNode(int minDim, Rectangle extent) {
    	if ((extent.width < minDim) || (extent.width < minDim)) {
    		throw new IllegalArgumentException();
    	}
    	depth_ = 0;
      parent_ = null;
      corner_ = Corner.SINGLETON;
      minDim_ = minDim;
      extent_ = extent;
      needKidInit_ = true;
    }
    
    /***************************************************************************
	  **
	  ** Build a child node
	  */  

    QuadTreeNode(QuadTreeNode parent, Corner corner, int minDim, int depth) {
    	depth_ = depth;
      parent_ = parent;
      corner_ = corner;
      minDim_ = minDim;
      needKidInit_ = true;
      Rectangle parentEx = parent.getExtent();
      int halfw = parentEx.width / 2;
      int halfwx = parentEx.width % 2;
      int halfh = parentEx.height / 2;
      int halfhx = parentEx.height % 2;
      minWidth_ = (halfw <= minDim_);
      minHeight_ = (halfh <= minDim_);
      switch (corner_) {
        case UPPER_LEFT:
        	extent_ = new Rectangle(parentEx.x, parentEx.y, halfw, halfh);
        	break;
        case UPPER_RIGHT:
        	extent_ = new Rectangle(parentEx.x + halfw, parentEx.y, halfw + halfwx, halfh);
        	break;
        case LOWER_LEFT:
        	extent_ = new Rectangle(parentEx.x, parentEx.y + halfh, halfw, halfh + halfhx);
        	break;
        case LOWER_RIGHT:
        	extent_ = new Rectangle(parentEx.x + halfw, parentEx.y + halfh, 
        			                    halfw + halfwx, halfh + halfhx);        	
        	break;
        default:
        	throw new IllegalArgumentException();
      }          
    }

	  /***************************************************************************
	  **
	  ** Given a rectangle and a depth, fill in the children at the depth that
	  ** contain the rectangle.
	  */
	  
	  boolean getNodes(Rectangle rect, int atDepth, List<QuadTreeNode> nodes) {
	  	//
	  	// If our depth is greater than the requested depth, we have nothing to do anymore:
	  	//
	  	if (atDepth < depth_) {
	  		return (false);
	  	}

  	  boolean weIntersect = extent_.intersects(rect);
  	  if (weIntersect) {
  	  	if (atDepth == depth_) {
  	  		nodes.add(this);
  	  		return (true);
  	  	}
  	  	if (needKidInit_) {
  	  		needKidInit_ = false;
  	  		// If no split occurred, we are at the minimum dimension, even if we
  	  		// have not hit the greatest depth
  	  	  boolean didSplit = split();
  	  		if (!didSplit) {
  	  			nodes.add(this);
  	  		  return (true);		
  	  		}
  	  	}
  	  	if (ulKid_ != null) {
  	  		ulKid_.getNodes(rect, atDepth, nodes);
  	  	}
  	  	if (urKid_ != null) {
  	  		urKid_.getNodes(rect, atDepth, nodes);
  	  	}
  	  	if (llKid_ != null) {
  	  		llKid_.getNodes(rect, atDepth, nodes);
  	  	}
  	  	if (lrKid_ != null) {
  	  		lrKid_.getNodes(rect, atDepth, nodes);
  	  	}
  	  	return (true);
  	  } else {
  	  	return (false);
  	  }
    } 

	  /***************************************************************************
	  **
	  ** Given a depth, fill in all the children encountered up to and including that depth
	  */
	  
	  boolean getAllNodesToDepth(int minDepth, int maxDepth, List<QuadTreeNode> nodes) {
	  	//
	  	// Once we hit the specified depth, we do not go any deeper!
	  	//
	  	if (maxDepth < depth_) {
	  		return (false);
	  	}

	  	if ((minDepth <= depth_) && (maxDepth >= depth_)) {
	  		nodes.add(this);
	  	}
	  	
	  	if (maxDepth > depth_) {
		  	if (needKidInit_) {
		  		needKidInit_ = false;
		  		// If no split occurred, we are at the minimum dimension, even if we
		  		// have not hit the greatest depth
		  	  boolean didSplit = split();
		  		if (!didSplit) {
		  		  return (true);		
		  		}
		  	}
		  	if (ulKid_ != null) {
		  		ulKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	}
		  	if (urKid_ != null) {
		  		urKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	}
		  	if (llKid_ != null) {
		  		llKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	}
		  	if (lrKid_ != null) {
		  		lrKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	}
	  	}
	  	return (true);
	  }
	  
	  /***************************************************************************
	  **
	  ** Get the child on the given corner
	  */	  
	  
    QuadTreeNode getChild(Corner corner) {
    	switch (corner_) {
        case UPPER_LEFT:
        	return (ulKid_);
        case UPPER_RIGHT:
        	return (urKid_);
        case LOWER_LEFT:
        	return (llKid_);
        case LOWER_RIGHT:
        	return (lrKid_);      	
        default:
        	throw new IllegalStateException();
      } 	
    }
    
    /***************************************************************************
	  **
	  ** Get the parent
	  */	
    
    
    QuadTreeNode getParent() {
      return (parent_);        
    }
    
    /***************************************************************************
	  **
	  ** Get the extent
	  */	 
    
    public Rectangle getExtent() {
      return (extent_);      
    }
    
    /***************************************************************************
	  **
	  ** Set the payload
	  */	
    
    
    void setPayload(Payload payLoad) {
      payLoad_ = payLoad;
      return;
    }
    
    /***************************************************************************
	  **
	  ** Get the payload
	  */	

    public Payload getPayload() {
      return (payLoad_);
    }

	  /***************************************************************************
	  **
	  ** Create the kids. If one of our dimensions is at or below the minimum dimension,
	  ** we do not split that dimension. Returns false is no more children to be added
	  ** (both dims now at or below minimum dimension).
	  */
    
    boolean split() {	
    	if (!minWidth_) {
    		ulKid_ = new QuadTreeNode(this, Corner.UPPER_LEFT, minDim_, depth_ + 1);
    		urKid_ = new QuadTreeNode(this, Corner.UPPER_RIGHT, minDim_, depth_ + 1);
    		if (!minHeight_) { 
    			llKid_ = new QuadTreeNode(this, Corner.LOWER_LEFT, minDim_, depth_ + 1);
    		  lrKid_ = new QuadTreeNode(this, Corner.LOWER_RIGHT, minDim_, depth_ + 1);
    		} else {
    			llKid_ = null;
    			lrKid_ = null;
    		}
    		return (true);
    	} else { // Is min width
    		urKid_ = null;
    		lrKid_ = null;
    		if (!minHeight_) {
    			ulKid_ = new QuadTreeNode(this, Corner.UPPER_LEFT, minDim_, depth_ + 1);
    			llKid_ = new QuadTreeNode(this, Corner.LOWER_LEFT, minDim_, depth_ + 1);
    			return (true);
    		} else {
    			ulKid_ = null;
    			llKid_ = null;
    			return (false);
    		}
    	}       
    }
  }
}
