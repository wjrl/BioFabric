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

import java.util.SortedSet;
import java.util.TreeSet;

public class MinMax implements Cloneable, Comparable<MinMax> {
  
  public static final int EQUALS             = 0;   
  public static final int IS_PROPER_SUPERSET = 1;
  public static final int IS_PROPER_SUBSET   = 2;
  public static final int IS_DISJOINT        = 3;
  public static final int INTERSECTS         = 4;
  
  public int min;
  public int max;
  
  public MinMax() {
  }

  public MinMax(int val) {
    this.min = val;
    this.max = val;
  }  
  
  public MinMax(int min, int max) {
    this.min = min;
    this.max = max;
  }
  
  
  public MinMax(Integer min, Integer max) {
    this.min = min.intValue();
    this.max = max.intValue();
  }
  
  
  public MinMax(SortedSet<Integer> ofIntegers) {
    if (ofIntegers.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.min = ofIntegers.first().intValue();
    this.max = ofIntegers.last().intValue();
  }

  public MinMax(MinMax other) {
    this.min = other.min;
    this.max = other.max;
  }
  
  @Override
  public MinMax clone() {
    try {
      MinMax newVal = (MinMax)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }    
  }

  public MinMax reset(int min, int max) {
    this.min = min;
    this.max = max;
    return (this);
  }
  
    @Override
  public int hashCode() {
    return (min + max);
  }
  
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof MinMax)) {
      return (false);
    }
    MinMax otherMM = (MinMax)other;
    return ((this.min == otherMM.min) && (this.max == otherMM.max));
  }  
  
    
  public SortedSet<Integer> getAsSortedSet() {
    TreeSet<Integer> retval = new TreeSet<Integer>();
    for (int i = min; i <= max; i++) {
      retval.add(new Integer(i));
    }
    return (retval);
  } 
 
  public MinMax union(MinMax other) {
    MinMax retval = new MinMax();
    retval.min = (this.min < other.min) ? this.min : other.min;
    retval.max = (this.max > other.max) ? this.max : other.max;    
    return (retval);
  } 
  
  public MinMax intersect(MinMax other) {
    MinMax retval = new MinMax();
    retval.min = (this.min < other.min) ? other.min : this.min;
    retval.max = (this.max > other.max) ? other.max : this.max;
    if (retval.min > retval.max) {
      return (null);
    }
    return (retval);
  } 
  
  public boolean contained(int val) {
    return ((val >= min) && (val <= max));
  } 
  
  public boolean outsideOrOnBoundary(int val) {
    return ((val <= min) || (val >= max));
  } 
  
  public MinMax init() {
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    return (this);
  }     
  
  public MinMax update(int newVal) {
    if (newVal < min) {
      min = newVal;
    }
    if (newVal > max) {
      max = newVal;
    }
    return (this);
  }
  
  /**
  *** Answer how the given MinMax relates to this one.
  **/
  
  public int evaluate(MinMax other) {
    if ((this.min == other.min) && (this.max == other.max)) {
      return (EQUALS);    
    } else if ((this.min >= other.min) && (this.max <= other.max)) {
      return (IS_PROPER_SUPERSET);
    } else if ((this.min <= other.min) && (this.max >= other.max)) {
      return (IS_PROPER_SUBSET);  
    } else if ((this.max < other.min) || (this.min > other.max)) {      
      return (IS_DISJOINT);
    } else {
      return (INTERSECTS);
    }
  }
 
  public String toString() {
    return ("min: " + min + " max: " + max);
  }
  
  public int compareTo(MinMax other) {
 
    //
    // Single value comes before a range:
    //

    if (this.min != other.min) {
      return (this.min - other.min);
    }
    
    return (this.max - other.max);
  } 
}
