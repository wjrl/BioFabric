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

package org.systemsbiology.biofabric.analysis;

import org.systemsbiology.biofabric.util.ExceptionHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

public class NetworkAlignment {
  
  private GraphNA small_, large_;
  private AlignmentNA align_;
  
  public NetworkAlignment(NetworkAlignInfo nai) {
    
    this.small_ = new GraphNA(nai.small);
    this.large_ = new GraphNA(nai.large);
    this.align_ = new AlignmentNA(nai.align);
    
    synthesize();
  }
  
  private void synthesize() {
    
    Map<String, String> smallNames = new TreeMap<String, String>();
    Map<String, String> largeNames = new TreeMap<String, String>();
    
    for (Map.Entry<String, String> entry : align_.nodeToNode_.entrySet()) {
      
      String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
      
      smallNames.put(x, xy);
      largeNames.put(y, xy);

//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
    }
    
    this.small_.updateNames(smallNames);
    this.large_.updateNames(largeNames);
  }
  
  @Override
  public String toString() {
    String ret;
    ret = "G1: " + small_ + '\n' + "G2: " + large_;
    
    return ret;
  }
  
  public GraphNA getSmall() {
    return new GraphNA(small_);
  }
  
  public GraphNA getLarge() {
    return new GraphNA(large_);
  }
  
  
  public static class GraphNA { // needs to implement cloneable in future
    
    private Map<String, Set<String>> edges_;
    private Map<Integer, String> names_;
    private int size_;
    
    private GraphNA() {
    }
    
    public GraphNA(GraphNA graphNA) {
      this.size_ = graphNA.getSize();
      this.names_ = graphNA.getNames();
      this.edges_ = graphNA.getEdges();
    }
    
    public static GraphNA readGraphGWFile(File file) {
      GraphNA ret = new GraphNA();
      ret.edges_ = new TreeMap<String, Set<String>>();
      ret.names_ = new TreeMap<Integer, String>();
      
      try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        
        for (int i = 0; i < 4; i++) {
          br.readLine();
        }
        
        ret.size_ = Integer.parseInt(br.readLine());
        
        for (int i = 1; i <= ret.size_; i++) {
          String name = br.readLine().trim();
          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
          
          ret.addNode(i, name);
        }
        
        final int edges = Integer.parseInt(br.readLine());
        
        for (int i = 1; i <= edges; i++) {
          StringTokenizer st = new StringTokenizer(br.readLine());
          int node1 = Integer.parseInt(st.nextToken());
          int node2 = Integer.parseInt(st.nextToken());
          
          ret.addEdge(node1, node2);
        }
        
        br.close();
      } catch (IOException ioe) {
        ExceptionHandler.getHandler().displayException(ioe);
      }
      
      return ret;
    }
    
    private void addNode(int nodeNum, String nodeName) {
      if (edges_.get(nodeName) == null) {
        edges_.put(nodeName, new TreeSet<String>());
      }
      if (names_.get(nodeNum) == null) {
        names_.put(nodeNum, nodeName);
      }
    }
    
    private void addEdge(int node1, int node2) {
      if (node1 == node2) {
        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
      }
      
      edges_.get(names_.get(node1)).add(names_.get(node2));
      edges_.get(names_.get(node2)).add(names_.get(node1));
    }
    
    void updateNames(Map<String, String> newNames) {
      
      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
      
      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
        String key = entry.getKey();
        String x = (newNames.get(key) == null) ? key : newNames.get(key);
        
        Set<String> newSet = new TreeSet<String>();
        for (String str : entry.getValue()) {
          String y = (newNames.get(str) == null) ? str : newNames.get(str);
          newSet.add(y);
        }
        
        newEdges.put(x, newSet);
      }
      
      edges_ = newEdges;
    }
    
    @Override
    public String toString() {
      
      int linkNum = 0;
      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
        linkNum += entry.getValue().size();
      }
      linkNum /= 2;
      
      return String.format("(V, E) = (%d, %d)", size_, linkNum);
    }
    
    public int getSize() {
      return size_;
    }
    
    public Map<Integer, String> getNames() {
      return new TreeMap<Integer, String>(names_);
    }
    
    public Map<String, Set<String>> getEdges() {
      return new TreeMap<String, Set<String>>(edges_);
    }
    
  }
  
  public static class AlignmentNA {
    
    private Map<String, String> nodeToNode_;
    
    private AlignmentNA() {
    }
    
    public AlignmentNA(AlignmentNA alignmentNA) {
      this.nodeToNode_ = alignmentNA.getNodeToNodeMapping();
    }
    
    public static AlignmentNA readAlignFile(File file) {
      AlignmentNA ret = new AlignmentNA();
      ret.nodeToNode_ = new TreeMap<String, String>();
      
      try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        
        String line;
        while ((line = br.readLine()) != null) {
          StringTokenizer st = new StringTokenizer(line);
          
          ret.nodeToNode_.put(st.nextToken(), st.nextToken());
        }
        br.close();
      } catch (IOException ioe) {
        ExceptionHandler.getHandler().displayException(ioe);
      }
      
      return ret;
    }
    
    public Map<String, String> getNodeToNodeMapping() {
      return new TreeMap<String, String>(nodeToNode_);
    }
    
    public int getSize() {
      return nodeToNode_.size();
    }
    
  }
  
  public static class NetworkAlignInfo {
    
    final GraphNA small, large;
    final AlignmentNA align;
    
    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
      this.small = small;
      this.large = large;
      this.align = align;
    }
    
  }
  
}


//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small_, large_;
//  private AlignmentNA align_;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
//    this.small_ = nai.small;
//    this.large_ = nai.large;
//    this.align_ = nai.align;
//
//    synthesize();
//
//
//  }
//
//  private void synthesize() {
//
//    Map<String, String> smallNames = new TreeMap<String, String>();
//    Map<String, String> largeNames = new TreeMap<String, String>();
//
//    for (Map.Entry<String, String> entry : align_.nodeToNode_.entrySet()) {
//
//      String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//      smallNames.put(x, xy);
//      largeNames.put(y, xy);
//
////        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//    }
//
//    this.small_.updateNames(smallNames);
//    this.large_.updateNames(largeNames);
//  }
//
//  @Override
//  public String toString() {
//    String ret;
//    ret = "G1: " + small + '\n' + "G2: " + large;
//
//    return ret;
//  }
//
//  public GraphNA getSmall() {
//    return new GraphNA(small_);
//  }
//
//  public GraphNA getLarge() {
//    return new GraphNA(large_);
//  }
//
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    private Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
//    private GraphNA() {
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    public static GraphNA readGraphGWFile(File file) {
//      GraphNA ret = new GraphNA();
//      ret.edges_ = new TreeMap<String, Set<String>>();
//      ret.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        ret.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= ret.size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          ret.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          ret.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    private void addNode(int nodeNum, String nodeName) {
//      if (edges_.get(nodeName) == null) {
//        edges_.put(nodeName, new TreeSet<String>());
//      }
//      if (names_.get(nodeNum) == null) {
//        names_.put(nodeNum, nodeName);
//      }
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> newNames) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (newNames.get(key) == null) ? key : newNames.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (newNames.get(str) == null) ? str : newNames.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//  public static class AlignmentNA {
//
//    private Map<String, String> nodeToNode_;
//
//    private AlignmentNA() {
//    }
//
//    public static AlignmentNA readAlignFile(File file) {
//      AlignmentNA ret = new AlignmentNA();
//      ret.nodeToNode_ = new TreeMap<String, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          ret.nodeToNode_.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    //    private void synthesize(GraphNA small, GraphNA large) {
////
////      this.small = new GraphNA(small);
////      this.large = new GraphNA(large);
////
////      Map<String, String> smallNames = new TreeMap<String, String>();
////      Map<String, String> largeNames = new TreeMap<String, String>();
////
////      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
////
////        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
////
////        smallNames.put(x, xy);
////        largeNames.put(y, xy);
////
//////        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
////      }
////
////      this.small.updateNames(smallNames);
////      this.large.updateNames(largeNames);
////    }
////
////    @Override
////    public String toString() {
////      String ret;
////      ret = "G1: " + small + '\n' + "G2: " + large;
////
////      return ret;
////    }
//    public Map<String, String> getNodeToNodeMapping() {
//      return new TreeMap<String, String>(nodeToNode_);
//    }
//
//    public int getSize() {
//      return nodeToNode_.size();
//    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    final GraphNA small, large;
//    final AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//}

///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
////    public GraphNA(File dir) {
////
////      this.edges_ = new TreeMap<String, Set<String>>();
////      this.names_ = new TreeMap<Integer, String>();
////
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(dir));
////
////        for (int i = 0; i < 4; i++) {
////          br.readLine();
////        }
////
////        this.size_ = Integer.parseInt(br.readLine());
////
////        for (int i = 1; i <= size_; i++) {
////          String name = br.readLine().trim();
////          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
////
////          this.addNode(i, name);
////        }
////
////        final int edges = Integer.parseInt(br.readLine());
////
////        for (int i = 1; i <= edges; i++) {
////          StringTokenizer st = new StringTokenizer(br.readLine());
////          int node1 = Integer.parseInt(st.nextToken());
////          int node2 = Integer.parseInt(st.nextToken());
////
////          this.addEdge(node1, node2);
////        }
////
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////    }
//
//    private GraphNA() {
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    public static GraphNA readGraphGWFile(File file) {
//      GraphNA ret = new GraphNA();
//      ret.edges_ = new TreeMap<String, Set<String>>();
//      ret.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        ret.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= ret.size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          ret.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          ret.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
////    public AlignmentNA(File file) {
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(file));
////
////        String line;
////        while ((line = br.readLine()) != null) {
////          StringTokenizer st = new StringTokenizer(line);
////
////          nodeToNode.put(st.nextToken(), st.nextToken());
////        }
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////    }
//
//    private AlignmentNA() {
//    }
//
////    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
////      this.small_A = small_A;
////      this.large_A = large_A;
////      this.nodes = new TreeSet<String>();
////
////      nodeToNode = new TreeMap<String, String>();
////
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(dir));
////
////        String line;
////        while ((line = br.readLine()) != null) {
////          StringTokenizer st = new StringTokenizer(line);
////
////          nodeToNode.put(st.nextToken(), st.nextToken());
////        }
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////
////      Map<String, String> smallNames = new TreeMap<String, String>();
////      Map<String, String> largeNames = new TreeMap<String, String>();
////
////      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
////
////        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
////
////        smallNames.put(x, xy);
////        largeNames.put(y, xy);
////
////        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
////      }
////
////      this.small_A.updateNames(smallNames);
////      this.large_A.updateNames(largeNames);
////    }
//
//    public static AlignmentNA readAlignFile(File file) {
//      AlignmentNA ret = new AlignmentNA();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          ret.nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    private void provide(GraphNA small_A, GraphNA large_A) {
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}

///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
////    GraphNA A = null, B = null;
////    try {
////      A = new GraphNA(nai.small);
////      B = new GraphNA(nai.large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
////
////    if (A == null || B == null) {
////
////    }
////
////    if (A.edges.size() >= B.edges.size()) {
////      large = A;
////      small = B;
////    } else {
////      large = B;
////      small = A;
////    }
////
////    try {
////      align = new AlignmentNA(alignment, small, large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
//    public GraphNA(File dir) {
//      this.edges_ = new TreeMap<String, Set<String>>();
//      this.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        this.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          this.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          this.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
//    public AlignmentNA(File file) {
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//    }
//
//    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
//      this.small_A = small_A;
//      this.large_A = large_A;
//      this.nodes = new TreeSet<String>();
//
//      nodeToNode = new TreeMap<String, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}


///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
////    GraphNA A = null, B = null;
////    try {
////      A = new GraphNA(nai.small);
////      B = new GraphNA(nai.large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
////
////    if (A == null || B == null) {
////
////    }
////
////    if (A.edges.size() >= B.edges.size()) {
////      large = A;
////      small = B;
////    } else {
////      large = B;
////      small = A;
////    }
////
////    try {
////      align = new AlignmentNA(alignment, small, large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
//    public GraphNA(File dir) {
//      this.edges_ = new TreeMap<String, Set<String>>();
//      this.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        this.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          this.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          this.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
//    public AlignmentNA(File file) {
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//    }
//
//    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
//      this.small_A = small_A;
//      this.large_A = large_A;
//      this.nodes = new TreeSet<String>();
//
//      nodeToNode = new TreeMap<String, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}





///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
////    public GraphNA(File dir) {
////
////      this.edges_ = new TreeMap<String, Set<String>>();
////      this.names_ = new TreeMap<Integer, String>();
////
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(dir));
////
////        for (int i = 0; i < 4; i++) {
////          br.readLine();
////        }
////
////        this.size_ = Integer.parseInt(br.readLine());
////
////        for (int i = 1; i <= size_; i++) {
////          String name = br.readLine().trim();
////          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
////
////          this.addNode(i, name);
////        }
////
////        final int edges = Integer.parseInt(br.readLine());
////
////        for (int i = 1; i <= edges; i++) {
////          StringTokenizer st = new StringTokenizer(br.readLine());
////          int node1 = Integer.parseInt(st.nextToken());
////          int node2 = Integer.parseInt(st.nextToken());
////
////          this.addEdge(node1, node2);
////        }
////
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////    }
//
//    private GraphNA() {
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    public static GraphNA readGraphGWFile(File file) {
//      GraphNA ret = new GraphNA();
//      ret.edges_ = new TreeMap<String, Set<String>>();
//      ret.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        ret.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= ret.size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          ret.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          ret.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
////    public AlignmentNA(File file) {
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(file));
////
////        String line;
////        while ((line = br.readLine()) != null) {
////          StringTokenizer st = new StringTokenizer(line);
////
////          nodeToNode.put(st.nextToken(), st.nextToken());
////        }
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////    }
//
//    private AlignmentNA() {
//    }
//
////    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
////      this.small_A = small_A;
////      this.large_A = large_A;
////      this.nodes = new TreeSet<String>();
////
////      nodeToNode = new TreeMap<String, String>();
////
////      try {
////        BufferedReader br = new BufferedReader(new FileReader(dir));
////
////        String line;
////        while ((line = br.readLine()) != null) {
////          StringTokenizer st = new StringTokenizer(line);
////
////          nodeToNode.put(st.nextToken(), st.nextToken());
////        }
////        br.close();
////      } catch (IOException ioe) {
////        ExceptionHandler.getHandler().displayException(ioe);
////      }
////
////      Map<String, String> smallNames = new TreeMap<String, String>();
////      Map<String, String> largeNames = new TreeMap<String, String>();
////
////      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
////
////        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
////
////        smallNames.put(x, xy);
////        largeNames.put(y, xy);
////
////        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
////      }
////
////      this.small_A.updateNames(smallNames);
////      this.large_A.updateNames(largeNames);
////    }
//
//    public static AlignmentNA readAlignFile(File file) {
//      AlignmentNA ret = new AlignmentNA();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          ret.nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      return ret;
//    }
//
//    private void provide(GraphNA small_A, GraphNA large_A) {
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}

///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
////    GraphNA A = null, B = null;
////    try {
////      A = new GraphNA(nai.small);
////      B = new GraphNA(nai.large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
////
////    if (A == null || B == null) {
////
////    }
////
////    if (A.edges.size() >= B.edges.size()) {
////      large = A;
////      small = B;
////    } else {
////      large = B;
////      small = A;
////    }
////
////    try {
////      align = new AlignmentNA(alignment, small, large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
//    public GraphNA(File dir) {
//      this.edges_ = new TreeMap<String, Set<String>>();
//      this.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        this.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          this.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          this.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
//    public AlignmentNA(File file) {
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//    }
//
//    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
//      this.small_A = small_A;
//      this.large_A = large_A;
//      this.nodes = new TreeSet<String>();
//
//      nodeToNode = new TreeMap<String, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}


///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.analysis;
//
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//
//        import java.io.BufferedReader;
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.IOException;
//        import java.util.*;
//
//public class NetworkAlignment {
//
//  private GraphNA small, large;
//  private AlignmentNA align;
//
//  public NetworkAlignment(NetworkAlignInfo nai) {
//
////    GraphNA A = null, B = null;
////    try {
////      A = new GraphNA(nai.small);
////      B = new GraphNA(nai.large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
////
////    if (A == null || B == null) {
////
////    }
////
////    if (A.edges.size() >= B.edges.size()) {
////      large = A;
////      small = B;
////    } else {
////      large = B;
////      small = A;
////    }
////
////    try {
////      align = new AlignmentNA(alignment, small, large);
////    } catch (IOException ioe) {
////      ExceptionHandler.getHandler().displayException(ioe);
////    }
//
//    this.small = nai.small;
//    this.large = nai.large;
//    this.align = nai.align;
//
//  }
//
//  public static class GraphNA { // needs to implement cloneable in future
//
//    Map<String, Set<String>> edges_;
//    private Map<Integer, String> names_;
//    private int size_;
//
//    public GraphNA(File dir) {
//      this.edges_ = new TreeMap<String, Set<String>>();
//      this.names_ = new TreeMap<Integer, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        for (int i = 0; i < 4; i++) {
//          br.readLine();
//        }
//
//        this.size_ = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= size_; i++) {
//          String name = br.readLine().trim();
//          name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
//
//          this.addNode(i, name);
//        }
//
//        final int edges = Integer.parseInt(br.readLine());
//
//        for (int i = 1; i <= edges; i++) {
//          StringTokenizer st = new StringTokenizer(br.readLine());
//          int node1 = Integer.parseInt(st.nextToken());
//          int node2 = Integer.parseInt(st.nextToken());
//
//          this.addEdge(node1, node2);
//        }
//
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//    }
//
//    public GraphNA(GraphNA graphNA) {
//      this.size_ = graphNA.getSize();
//      this.names_ = graphNA.getNames();
//      this.edges_ = graphNA.getEdges();
//    }
//
//    private void addNode(int node, String name) {
//      if (edges_.get(name) == null) {
//        edges_.put(name, new TreeSet<String>());
//      }
//      if (names_.get(node) == null) {
//        names_.put(node, name);
//      }
//      //      edges.putIfAbsent(name, new TreeSet<String>());
//      //      names.putIfAbsent(node, name);
//    }
//
//    private void addEdge(int node1, int node2) {
//      if (node1 == node2) {
//        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
//      }
//
//      edges_.get(names_.get(node1)).add(names_.get(node2));
//      edges_.get(names_.get(node2)).add(names_.get(node1));
//    }
//
//    void updateNames(Map<String, String> names) {
//
//      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
//
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        String key = entry.getKey();
//        String x = (names.get(key) == null) ? key : names.get(key);
//
//        Set<String> newSet = new TreeSet<String>();
//        for (String str : entry.getValue()) {
//          String y = (names.get(str) == null) ? str : names.get(str);
//          newSet.add(y);
//        }
//
//        newEdges.put(x, newSet);
//      }
//
//      edges_ = newEdges;
//    }
//
////    public void extractNode(Set<String> set, String seed) throws IOException {
////
//////        Queue<String> queue = new LinkedList<>();
//////
//////        int level = 0, count = 0;
//////        queue.add(seed);
//////        while (! queue.isEmpty()) {
//////            String node = queue.poll();
//////            count = edges.get(node).size();
//////
//////            level++;
//////
//////            if (level > maxLevel) break;
//////
//////            for (String s : edges.get(node)) {
//////                queue.add(s);
//////                String[] arr = Parser.sort(node, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////
//////        }
//////
//////        for (String s : set) {
//////            set.add(s);
//////        }
////
////      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
////
////      for (String n : lvl_1) {
////        for (String s : edges.get(n)) {
////          lvl_2.add(s);
////          String[] arr = Parser.sort(n, s);
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
//////        for (String n : lvl_2) {
//////            for (String s : edges.get(n)) {
//////                String[] arr = Parser.sort(n, s);
//////                set.add(arr[0] + " " + arr[1]);
//////            }
//////        }
////
////
////    }
//
////    public void toSIF(String dir, String tag) throws IOException {
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
////
////        String x = entry.getKey();
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          String link = arr[0] + " " + tag + " " + arr[1];
////
////          set.add(link);
////        }
////      }
////
////      PrintWriter pw = new PrintWriter(new File(dir));
////      for (String s : set) {
////        pw.println(s);
////      }
////
////      pw.close();
////    }
//
//    @Override
//    public String toString() {
//
//      int linkNum = 0;
//      for (Map.Entry<String, Set<String>> entry : edges_.entrySet()) {
//        linkNum += entry.getValue().size();
//      }
//      linkNum /= 2;
//
//      return String.format("(V, E) = (%d, %d)", size_, linkNum);
//    }
//
//    public int getSize() {
//      return size_;
//    }
//
//    public Map<Integer, String> getNames() {
//      return new TreeMap<Integer, String>(names_);
//    }
//
//    public Map<String, Set<String>> getEdges() {
//      return new TreeMap<String, Set<String>>(edges_);
//    }
//
//  }
//
//
//  public static class AlignmentNA {
//
//    Map<String, String> nodeToNode;
//    Set<String> nodes;
//    GraphNA small_A, large_A;
//
//    public AlignmentNA(File file) {
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(file));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//    }
//
//    public AlignmentNA(File dir, GraphNA small_A, GraphNA large_A) {
//      this.small_A = small_A;
//      this.large_A = large_A;
//      this.nodes = new TreeSet<String>();
//
//      nodeToNode = new TreeMap<String, String>();
//
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(dir));
//
//        String line;
//        while ((line = br.readLine()) != null) {
//          StringTokenizer st = new StringTokenizer(line);
//
//          nodeToNode.put(st.nextToken(), st.nextToken());
//        }
//        br.close();
//      } catch (IOException ioe) {
//        ExceptionHandler.getHandler().displayException(ioe);
//      }
//
//      Map<String, String> smallNames = new TreeMap<String, String>();
//      Map<String, String> largeNames = new TreeMap<String, String>();
//
//      for (Map.Entry<String, String> entry : nodeToNode.entrySet()) {
//
//        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
//
//        smallNames.put(x, xy);
//        largeNames.put(y, xy);
//
//        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
//      }
//
//      this.small_A.updateNames(smallNames);
//      this.large_A.updateNames(largeNames);
//    }
//
////    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
////      large_A.toSIF(ordDir, "xx");
////      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
////    }
//
//    @Override
//    public String toString() {
//      String ret;
//      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
//
//      return ret;
//    }
//
////    public static List<String> writeEdges(Graph G) {
////
////      Set<String> set = new TreeSet<String>();
////
////      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
////
////        String x = entry.getKey();
////
////        for (String y : entry.getValue()) {
////
////          String[] arr = Parser.sort(x, y);
////
////          set.add(arr[0] + " " + arr[1]);
////        }
////      }
////
////      List<String> ret = new ArrayList<String>();
////      for (String s : set) {
////        ret.add(s);
////      }
////
////      return ret;
////    }
//
//  }
//
//  public static class NetworkAlignInfo {
//
//    GraphNA small, large;
//    AlignmentNA align;
//
//    public NetworkAlignInfo(GraphNA small, GraphNA large, AlignmentNA align) {
//      this.small = small;
//      this.large = large;
//      this.align = align;
//    }
//
//  }
//
//  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
//    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
//
////    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
////            throws IOException {
////      PrintWriter pw = new PrintWriter(new File(dir));
////
////      List<String> Sall = Alignment.writeEdges(align.small_A);
////      List<String> Lall = Alignment.writeEdges(align.large_A);
////
////      Collections.sort(Sall);
////      Collections.sort(Lall);
////
////      List<String> CC = new ArrayList<String>();
////      for (String x : Sall) {
////        if (Collections.binarySearch(Lall, x) >= 0) {
////          CC.add(x);
////        }
////      }
////
////      for (String s : Sall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagS + " " + arr[1]);
////      }
////
////      for (String s : Lall) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + tagL + " " + arr[1]);
////      }
////
////      for (String s : CC) {
////        String[] arr = split(s);
////
////        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//    /**
//     * splits parameter by space
//     */
//    public static String[] split(String twoNodes) {
//      StringTokenizer st = new StringTokenizer(twoNodes);
//      return new String[]{st.nextToken(), st.nextToken()};
//    }
//
//    /**
//     * splits parameter by dash
//     */
//    public static String[] splice(String edge) {
//      return edge.split("-");
//    }
//
//    /**
//     * sorts two strings in array
//     */
//    public static String[] sort(String a, String b) {
//      String[] ret = {a, b};
//      Arrays.sort(ret);
//
//      return ret;
//    }
//
//    /**
//     * true if two parts are equal; split by dash
//     */
//    public static boolean sameNode(String node) {
//      String[] arr = node.split("-");
//      return arr[0].equals(arr[1]);
//    }
//
////    public static void write(String filename, Set<String> set) throws IOException {
////      PrintWriter pw = new PrintWriter(new File(filename));
////      for (String s : set) {
////        String[] arr = Parser.split(s);
////        pw.println(arr[0] + " pp " + arr[1]);
////      }
////
////      pw.close();
////    }
//
//  }
//
//}

