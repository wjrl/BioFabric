/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.model;

import java.util.Comparator;
import java.util.Map;

import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** A Class
*/

public class FabricLink implements Cloneable, AttributeLoader.AttributeKey {
  private NID.WithName srcID_;
  private NID.WithName trgID_;
  private String relation_;
  private Boolean directed_;
  private boolean isShadow_;

  public FabricLink(NID.WithName srcID, NID.WithName trgID, String relation, boolean isShadow, Boolean directed) {
    if ((srcID == null) || (trgID == null) || (relation == null)) {
      throw new IllegalArgumentException();
    }
    srcID_ = srcID;
    trgID_ = trgID;
    relation_ = relation;
    isShadow_ = isShadow;
    directed_ = directed;
  }
  
  public FabricLink(NID.WithName srcID, NID.WithName trgID, String relation, boolean isShadow) {
    this(srcID, trgID, relation, isShadow, null);
  }
    
  public FabricLink flipped() {
    if (isFeedback()) {
      throw new IllegalStateException();
    }
    return (new FabricLink(trgID_, srcID_, relation_, isShadow_, directed_)); 
  }
  
  public boolean directionFrozen() {
    return (directed_ != null); 
  }
  
  public void installDirection(Boolean isDirected) {
    if (directed_ != null) {
      throw new IllegalStateException();
    }
    directed_ = isDirected;
    return;
  }
  
  @Override
  public FabricLink clone() {
    try {
      return ((FabricLink)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
   
  public boolean isShadow() {
    return (isShadow_);
  }
  
  public void dropShadowStatus() {
    isShadow_ = false;
    return;
  }
  
  public NID.WithName getTrgID() {
    return (trgID_);
  }

  public NID.WithName getSrcID() {
    return (srcID_);
  } 
  
  public String getRelation() {
    return (relation_);
  }
  
  public AugRelation getAugRelation() {
    return (new AugRelation(relation_, isShadow_));
  } 
  
  public boolean isDirected() {
    if (directed_ == null) {
      throw new IllegalStateException(); 
    }  
    return (directed_.booleanValue());
  } 
    
  public boolean isFeedback() {
    return (srcID_.equals(trgID_));
  } 
  
  @Override
  public int hashCode() {
    return (srcID_.hashCode() + trgID_.hashCode() + DataUtil.normKey(relation_).hashCode() + ((isShadow_) ? 17 : 31) + 
            ((directed_ == null) ? 0 : directed_.hashCode()));
  }

  @Override
  public String toString() {
    return ("srcID = " + srcID_ + " trgID = " + trgID_ + "rel = " + relation_ + " directed_ = " + directed_ + " isShadow_ = " + isShadow_);
  }
 
  public String toDisplayString() {
    StringBuffer buf = new StringBuffer();
    buf.append(srcID_.getName());
    buf.append((isDirected()) ? '-' : '\u2190');
    if (isShadow_) {
      buf.append("shdw");
    }
    buf.append("(");
    buf.append(relation_);
    buf.append(")");  
    buf.append('\u2192');  // For bidirectional '\u2194'
    buf.append(trgID_.getName());
    return (buf.toString());
  }
  
  public String toEOAString(Map<NID.WithName, BioFabricNetwork.NodeInfo> nodeInfo) {
    StringBuffer buf = new StringBuffer();
    buf.append(nodeInfo.get(srcID_).getNodeName());
    if (isShadow_) {
      buf.append(" shdw");
    } else {
      buf.append(" ");
    }
    buf.append("(");
    buf.append(relation_);
    buf.append(") ");
    buf.append(nodeInfo.get(trgID_).getNodeName());
    return (buf.toString());
  }
   
  @Override
  public boolean equals(Object other) {    
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof FabricLink)) {
      return (false);
    }
    FabricLink otherLink = (FabricLink)other;
  
    if (!this.srcID_.equals(otherLink.srcID_)) {
      return (false);
    }
    if (!this.trgID_.equals(otherLink.trgID_)) {
      return (false);
    }
    
    if (this.isShadow_ != otherLink.isShadow_) {
      return (false);
    }
    
    if (!DataUtil.normKey(this.relation_).equals(DataUtil.normKey(otherLink.relation_))) {
      return (false);
    }
    
    if (this.directed_ == null) {
      return (otherLink.directed_ == null);
    }   
       
    return (this.directed_.equals(otherLink.directed_));
  }  
 
  public boolean synonymous(FabricLink other) {
    if (this.equals(other)) {
      return (true);
    }
    if (this.isDirected() || other.isDirected()) {
      return (false);
    }
    if (!DataUtil.normKey(this.relation_).equals(DataUtil.normKey(other.relation_))) {
      return (false);
    }
    if (this.isShadow_ != other.isShadow_) {
      return (false);
    }    
    if (!this.srcID_.equals(other.trgID_)) {
      return (false);
    }
    return (this.trgID_.equals(other.srcID_));
  }
   
   public boolean shadowPair(FabricLink other) {
    if (this.equals(other)) {
      return (false);
    }
    if (!this.srcID_.equals(other.srcID_)) {
      return (false);
    }
    if (!this.trgID_.equals(other.trgID_)) {
      return (false);
    }   
    if (!DataUtil.normKey(this.relation_).equals(DataUtil.normKey(other.relation_))) {
      return (false);
    }
    
    if (this.directed_ == null) {
      if (other.directed_ != null) {
        return (false); 
      }
    } else if (!this.directed_.equals(other.directed_)) {
      return (false);
    }
    if (this.isShadow_ == other.isShadow_) {
      return (false);
    }
    return (true);   
  }

  /***************************************************************************
  **
  ** Comparator for Fabric Links
  */  
  
  public static class FabLinkComparator implements Comparator<FabricLink> {
    
    public int compare(FabricLink one, FabricLink otherLink) {
	    if (one.equals(otherLink)) {
	      return (0);
	    }
	    
	    String srcOne = DataUtil.normKey(one.srcID_.getName());
	    String trgOne = DataUtil.normKey(one.trgID_.getName());
	    String srcTwo = DataUtil.normKey(otherLink.srcID_.getName());
	    String trgTwo = DataUtil.normKey(otherLink.trgID_.getName());
	    
	    if (!srcOne.equals(srcTwo)) {
	      return (srcOne.compareTo(srcTwo));
	    }    
	    if (!trgOne.equals(trgTwo)) {
	      return (trgOne.compareTo(trgTwo));
	    } 
	    if (one.isShadow_ != otherLink.isShadow_) {
	      return ((one.isShadow_) ? -1 : 1);
	    }  
	    if (!DataUtil.normKey(one.relation_).equals(DataUtil.normKey(otherLink.relation_))) {
	      return (one.relation_.compareToIgnoreCase(otherLink.relation_));
	    }
	   
	    //
	    // Gatta catch the far-out case where two links with same source and target node names but different
	    // internal IDs get to here. Can't let it fall through
	    //
	    
	    if (!one.srcID_.equals(otherLink.srcID_)) {
         return (one.srcID_.compareTo(otherLink.srcID_));
      }
      if (!one.trgID_.equals(otherLink.trgID_)) {
        return (one.trgID_.compareTo(otherLink.trgID_));
      }

	    throw new IllegalStateException();
	  }
    	
  }
  
  /***************************************************************************
  **
  ** Augmented relation
  */  
  
  public static class AugRelation implements Cloneable, Comparable<AugRelation> {
    public String relation;
    public boolean isShadow;
    
    public AugRelation(String relation, boolean isShadow) {
      this.relation = relation;
      this.isShadow = isShadow;
    }
    
    @Override
    public AugRelation clone() {
      try {
        return ((AugRelation)super.clone());
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }   
   
    public int compareTo(AugRelation otherAug) {
      if (this.equals(otherAug)) {
        return (0);
      }

      if (this.isShadow != otherAug.isShadow) {
        return ((this.isShadow) ? -1 : 1);
      }    
      if (!this.relation.toUpperCase().equals(otherAug.relation.toUpperCase())) {
        return (this.relation.compareToIgnoreCase(otherAug.relation));
      }
      throw new IllegalStateException();
    }
      
    @Override  
    public boolean equals(Object other) {    
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof AugRelation)) {
        return (false);
      }
      AugRelation otherAug = (AugRelation)other;

      if (this.isShadow != otherAug.isShadow) {
        return (false);
      }
    
      return (this.relation.toUpperCase().equals(otherAug.relation.toUpperCase()));
    }
     
    @Override
    public int hashCode() {
      return (relation.toUpperCase().hashCode() + ((isShadow) ? 17 : 31));
    }

    @Override
    public String toString() {
      return ("rel = " + relation + " isShadow = " + isShadow);
    }    
  }
 
}
