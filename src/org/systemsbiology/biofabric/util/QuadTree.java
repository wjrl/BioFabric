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

import java.awt.geom.Rectangle2D;
import java.util.List;

/***************************************************************************
**
** Class for fast access to tiles that display a rectangle
*/

public class QuadTree {
  
  private int maxDepth_;
  private QuadTreeNode root_;
  
  public QuadTree(Rectangle2D worldExtent, int maxDepth) {
  	maxDepth_ = maxDepth;
  	root_ = new QuadTreeNode(worldExtent);
  } 
   
  /***************************************************************************
	**
	** Given a rectangle and a depth, fill in the children at the depth that
	** contain the rectangle. Returns true if anything found. 
	*/
	  
  public boolean getNodes(Rectangle2D worldRect, int atDepth, List<QuadTreeNode> nodes) {
    if (atDepth >= maxDepth_) {
    	throw new IllegalArgumentException();
    }
  	return (root_.getNodes(worldRect, atDepth, nodes));
  }
  
  /***************************************************************************
	**
	** Given a rectangle that maps exactly to one of our nodes, fill in the path of nodes to that
	** rectangle.
	*/
	  
  public boolean getPath(Rectangle2D worldRect, List<QuadTreeNode> path) {
  	return (root_.getPath(worldRect, path));
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
  ** Nodes of the tree
  */
  
  public static class QuadTreeNode {
    	
  	enum Corner {SINGLETON, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT}
  	
  	private int depth_;

  	private Rectangle2D worldExtent_;
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

    QuadTreeNode(Rectangle2D worldExtent) {
    	depth_ = 0;
      parent_ = null;
      corner_ = Corner.SINGLETON;
      worldExtent_ = worldExtent;
      needKidInit_ = true;
    }
    
    /***************************************************************************
	  **
	  ** Build a child node
	  */  

    QuadTreeNode(QuadTreeNode parent, Corner corner, int depth) {
    	depth_ = depth;
      parent_ = parent;
      corner_ = corner;
      needKidInit_ = true;
      Rectangle2D parentWEx = parent.getWorldExtent();
      double dhalfw = parentWEx.getWidth() * 0.5;
      double dhalfh = parentWEx.getHeight() * 0.5;

      switch (corner_) {
        case UPPER_LEFT:
        	worldExtent_ = new Rectangle2D.Double(parentWEx.getX(), parentWEx.getY(), dhalfw, dhalfh);
        	break;
        case UPPER_RIGHT:
        	worldExtent_ = new Rectangle2D.Double(parentWEx.getX() + dhalfw, parentWEx.getY(), dhalfw, dhalfh);
        	break;
        case LOWER_LEFT:
        	worldExtent_ = new Rectangle2D.Double(parentWEx.getX(), parentWEx.getY() + dhalfh, dhalfw, dhalfh);
        	break;
        case LOWER_RIGHT:
        	worldExtent_ = new Rectangle2D.Double(parentWEx.getX() + dhalfw, 
        			                                  parentWEx.getY() + dhalfh, dhalfw, dhalfh);
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
	  
	  boolean getNodes(Rectangle2D worldRect, int atDepth, List<QuadTreeNode> nodes) {
	  	//
	  	// If our depth is greater than the requested depth, we have nothing to do anymore:
	  	//
	  	if (atDepth < depth_) {
	  //		System.out.println("we are deep " + depth_ + " " + atDepth);
	  		return (false); 
	  	}

  	  boolean weIntersect = worldExtent_.intersects(worldRect);
  	  if (weIntersect) {
  	  	if (atDepth == depth_) {
  	  //		System.out.println("we are at depth " + depth_ + " " + atDepth + " " + worldRect + " " + worldExtent_);
  	  		nodes.add(this);
  	  		return (true);
  	  	}
  	  	if (needKidInit_) {
  	  		needKidInit_ = false;
  	  	  split();
  	  	}
	  		ulKid_.getNodes(worldRect, atDepth, nodes);
	  		urKid_.getNodes(worldRect, atDepth, nodes);
	  		llKid_.getNodes(worldRect, atDepth, nodes);
	  		lrKid_.getNodes(worldRect, atDepth, nodes);
  	  	return (true);
  	  } else {
  	//  	System.out.println("no hit " + "  " + worldRect + " " + worldExtent_);
  	  	return (false);
  	  }
    } 
	  
	  /***************************************************************************
	  **
	  ** Given a rectangle and a depth, fill in the children at the depth that
	  ** contain the rectangle.
	  */
	  
	  boolean getPath(Rectangle2D matchRect, List<QuadTreeNode> path) {
	  
  	  boolean weMatch = worldExtent_.equals(matchRect);
  	  if (weMatch) {
  	  	path.add(this);
  	  	return (true);
  	  }
  	  boolean weIntersect = worldExtent_.intersects(matchRect);
  	  if (!weIntersect) {
  	    return (false);
  	  }
  	  path.add(this);
  	  if (needKidInit_) {
  	  	throw new IllegalStateException();
  	  }
	  	if (ulKid_.getPath(matchRect, path)) {
	  	  return (true);
	  	} else if (urKid_.getPath(matchRect, path)) {
  			return (true);
	  	} else if (llKid_.getPath(matchRect, path)) {
  			return (true);
	  	}	else if (lrKid_.getPath(matchRect, path)) {
  			return (true);
	  	}		
	  	throw new IllegalStateException();
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
		  	  split();
		  	}
		  	ulKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	urKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	llKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
		  	lrKid_.getAllNodesToDepth(minDepth, maxDepth, nodes);
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
	  ** Get the world extent
	  */	 
    
    public Rectangle2D getWorldExtent() {
      return (worldExtent_);      
    }
    
    /***************************************************************************
	  **
	  ** Get the depth
	  */	 
    
    public int getDepth() {
      return (depth_);      
    }
    
	  /***************************************************************************
	  **
	  ** Create the kids.
	  */
    
    void split() {	
      ulKid_ = new QuadTreeNode(this, Corner.UPPER_LEFT, depth_ + 1);
    	urKid_ = new QuadTreeNode(this, Corner.UPPER_RIGHT, depth_ + 1);
    	llKid_ = new QuadTreeNode(this, Corner.LOWER_LEFT,depth_ + 1);
    	lrKid_ = new QuadTreeNode(this, Corner.LOWER_RIGHT, depth_ + 1);
    	return;    
    }
  }
}
