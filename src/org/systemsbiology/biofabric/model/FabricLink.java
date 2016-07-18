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

package org.systemsbiology.biofabric.model;

import org.systemsbiology.biofabric.io.AttributeLoader;

/****************************************************************************
**
** A Class
*/

public class FabricLink implements Cloneable, Comparable<FabricLink>, AttributeLoader.AttributeKey {
  private String src_;
  private String trg_;
  private String relation_;
  private Boolean directed_;
  private boolean isShadow_;

  public FabricLink(String src, String trg, String relation, boolean isShadow, Boolean directed) {
    if ((src == null) || (trg == null) || (relation == null)) {
      throw new IllegalArgumentException();
    }
    src_ = src;
    trg_ = trg;
    relation_ = relation;
    isShadow_ = isShadow;
    directed_ = directed;
  }
  
  public FabricLink(String src, String trg, String relation, boolean isShadow) {
    this(src, trg, relation, isShadow, null);
  }
    
  public FabricLink flipped() {
    if (isFeedback()) {
      throw new IllegalStateException();
    }
    return (new FabricLink(trg_, src_, relation_, isShadow_, directed_)); 
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
  
  public String getTrg() {
    return (trg_);
  }

  public String getSrc() {
    return (src_);
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
    return (src_.equals(trg_));
  } 
  
  @Override
  public int hashCode() {
    return (src_.hashCode() + trg_.hashCode() + relation_.hashCode() + ((isShadow_) ? 17 : 31) + 
            ((directed_ == null) ? 0 : directed_.hashCode()));
  }

  @Override
  public String toString() {
    return ("src = " + src_ + " trg = " + trg_ + "rel = " + relation_ + " directed_ = " + directed_ + " isShadow_ = " + isShadow_);
  }
 
  public String toDisplayString() {
    StringBuffer buf = new StringBuffer();
    buf.append(src_);
    buf.append((isDirected()) ? '-' : '\u2190');
    if (isShadow_) {
      buf.append("shdw");
    }
    buf.append("(");
    buf.append(relation_);
    buf.append(")");  
    buf.append('\u2192');  // For bidirectional '\u2194'
    buf.append(trg_);
    return (buf.toString());
  }
  
  public String toEOAString() {
    StringBuffer buf = new StringBuffer();
    buf.append(src_);
    if (isShadow_) {
      buf.append(" shdw");
    } else {
      buf.append(" ");
    }
    buf.append("(");
    buf.append(relation_);
    buf.append(") ");
    buf.append(trg_);
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
  
    if (!this.src_.equals(otherLink.src_)) {
      return (false);
    }
    if (!this.trg_.equals(otherLink.trg_)) {
      return (false);
    }
    
    if (this.isShadow_ != otherLink.isShadow_) {
      return (false);
    }
    
    if (!this.relation_.equals(otherLink.relation_)) {
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
    if (!this.relation_.equals(other.relation_)) {
      return (false);
    }
    if (this.isShadow_ != other.isShadow_) {
      return (false);
    }    
    if (!this.src_.equals(other.trg_)) {
      return (false);
    }
    return (this.trg_.equals(other.src_));
  }
   
   public boolean shadowPair(FabricLink other) {
    if (this.equals(other)) {
      return (false);
    }
    if (!this.src_.equals(other.src_)) {
      return (false);
    }
    if (!this.trg_.equals(other.trg_)) {
      return (false);
    }   
    if (!this.relation_.equals(other.relation_)) {
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

  public int compareTo(FabricLink otherLink) {
    if (this.equals(otherLink)) {
      return (0);
    }

    if (!this.src_.equals(otherLink.src_)) {
      return (this.src_.compareToIgnoreCase(otherLink.src_));
    }    
    if (!this.trg_.equals(otherLink.trg_)) {
      return (this.trg_.compareToIgnoreCase(otherLink.trg_));
    }
    if (this.isShadow_ != otherLink.isShadow_) {
      return ((this.isShadow_) ? -1 : 1);
    }   
    if (!this.relation_.equals(otherLink.relation_)) {
      return (this.relation_.compareToIgnoreCase(otherLink.relation_));
    }
    throw new IllegalStateException();
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
      if (!this.relation.equals(otherAug.relation)) {
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
    
      return (this.relation.equals(otherAug.relation));
    }
     
    @Override
    public int hashCode() {
      return (relation.hashCode() + ((isShadow) ? 17 : 31));
    }

    @Override
    public String toString() {
      return ("rel = " + relation + " isShadow = " + isShadow);
    }    
  }
 
}
