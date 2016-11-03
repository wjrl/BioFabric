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

import java.io.*;
import java.util.*;

public class NetworkAlignment {
  
  private Graph small, large;
  private Alignment align;
  
  public NetworkAlignment(File graph1, File graph2, File alignment) throws IOException {
    
    Graph A = new Graph(graph1), B = new Graph(graph2);
    if (A.edges.size() >= B.edges.size()) {
      large = A;
      small = B;
    } else {
      large = B;
      small = A;
    }
    
    Alignment align = new Alignment(alignment, small, large);
    
  }
  
  private static class Graph {
    
    Map<String, Set<String>> edges;
    private final Map<Integer, String> names;
    private final int size;
    
    Graph(File dir) throws IOException {
      this.edges = new TreeMap<String, Set<String>>();
      this.names = new TreeMap<Integer, String>();
      
      BufferedReader br = new BufferedReader(new FileReader(dir));
      
      for (int i = 0; i < 4; i++) {
        br.readLine();
      }
      
      this.size = Integer.parseInt(br.readLine());
      
      for (int i = 1; i <= size; i++) {
        String name = br.readLine().trim();
        name = name.substring(2, name.length() - 2); // exclude "|{" and "}|"
        
        this.addNode(i, name);
      }
      
      final int edges = Integer.parseInt(br.readLine());
      
      for (int i = 1; i <= edges; i++) {
        StringTokenizer st = new StringTokenizer(br.readLine());
        int node1 = Integer.parseInt(st.nextToken());
        int node2 = Integer.parseInt(st.nextToken());
        
        this.addEdge(node1, node2);
      }
      
      br.close();
    }
    
    void addNode(int node, String name) {
      if (edges.get(name) == null) {
        edges.put(name, new TreeSet<String>());
      }
      if (names.get(node) == null) {
        names.put(node, name);
      }
      //      edges.putIfAbsent(name, new TreeSet<String>());
      //      names.putIfAbsent(node, name);
    }
    
    void addEdge(int node1, int node2) {
      if (node1 == node2) {
        ExceptionHandler.getHandler().displayException(new IllegalArgumentException("node1 == node2"));
      }
      
      edges.get(names.get(node1)).add(names.get(node2));
      edges.get(names.get(node2)).add(names.get(node1));
    }
    
    void updateNames(Map<String, String> names) {
      
      Map<String, Set<String>> newEdges = new TreeMap<String, Set<String>>();
      
      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
        String key = entry.getKey();
        String x = (names.get(key) == null) ? key : names.get(key);
        
        Set<String> newSet = new TreeSet<String>();
        for (String str : entry.getValue()) {
          String y = (names.get(str) == null) ? str : names.get(str);
          newSet.add(y);
        }
        
        newEdges.put(x, newSet);
      }
      
      edges = newEdges;
    }
    
//    public void extractNode(Set<String> set, String seed) throws IOException {
//
////        Queue<String> queue = new LinkedList<>();
////
////        int level = 0, count = 0;
////        queue.add(seed);
////        while (! queue.isEmpty()) {
////            String node = queue.poll();
////            count = edges.get(node).size();
////
////            level++;
////
////            if (level > maxLevel) break;
////
////            for (String s : edges.get(node)) {
////                queue.add(s);
////                String[] arr = Parser.sort(node, s);
////                set.add(arr[0] + " " + arr[1]);
////            }
////
////        }
////
////        for (String s : set) {
////            set.add(s);
////        }
//
//      Set<String> lvl_1 = edges.get(seed), lvl_2 = new TreeSet<String>();
//
//      for (String n : lvl_1) {
//        for (String s : edges.get(n)) {
//          lvl_2.add(s);
//          String[] arr = Parser.sort(n, s);
//          set.add(arr[0] + " " + arr[1]);
//        }
//      }
//
////        for (String n : lvl_2) {
////            for (String s : edges.get(n)) {
////                String[] arr = Parser.sort(n, s);
////                set.add(arr[0] + " " + arr[1]);
////            }
////        }
//
//
//    }
    
//    public void toSIF(String dir, String tag) throws IOException {
//      Set<String> set = new TreeSet<String>();
//
//      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
//
//        String x = entry.getKey();
//        for (String y : entry.getValue()) {
//
//          String[] arr = Parser.sort(x, y);
//
//          String link = arr[0] + " " + tag + " " + arr[1];
//
//          set.add(link);
//        }
//      }
//
//      PrintWriter pw = new PrintWriter(new File(dir));
//      for (String s : set) {
//        pw.println(s);
//      }
//
//      pw.close();
//    }
    
    @Override
    public String toString() {
      
      int linkNum = 0;
      for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
        linkNum += entry.getValue().size();
      }
      linkNum /= 2;
      
      return String.format("(V, E) = (%d, %d)", size, linkNum);
    }
    
  }
  
  private static class Alignment {
    
    final Set<String> nodes;
    final Graph small_A, large_A;
    
    Alignment(File dir, Graph small_A, Graph large_A) throws IOException {
      this.small_A = small_A;
      this.large_A = large_A;
      this.nodes = new TreeSet<String>();
      
      BufferedReader br = new BufferedReader(new FileReader(dir));
      
      Map<String, String> org_NodeToNode = new TreeMap<String, String>();
      String line;
      while ((line = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line);
        
        org_NodeToNode.put(st.nextToken(), st.nextToken());
      }
      br.close();
      
      Map<String, String> smallNames = new TreeMap<String, String>();
      Map<String, String> largeNames = new TreeMap<String, String>();
      
      for (Map.Entry<String, String> entry : org_NodeToNode.entrySet()) {
        
        String x = entry.getKey(), y = entry.getValue(), xy = x + '-' + y;
        
        smallNames.put(x, xy);
        largeNames.put(y, xy);
        
        nodes.add(xy); // FIX ME: DOESN'T INCLUDE UNALIGNED NODES FROM LARGE GRAPH
      }
      
      this.small_A.updateNames(smallNames);
      this.large_A.updateNames(largeNames);
    }
    
//    public void toFiles(String alignDir, String ordDir, String tagS, String tagL) throws IOException {
//      large_A.toSIF(ordDir, "xx");
//      Parser.toAlignmentSIF(alignDir, this, tagS, tagL);
//    }
    
    @Override
    public String toString() {
      String ret;
      ret = "G1: " + small_A + '\n' + "G2: " + large_A;
      
      return ret;
    }
    
//    public static List<String> writeEdges(Graph G) {
//
//      Set<String> set = new TreeSet<String>();
//
//      for (Map.Entry<String, Set<String>> entry : G.edges.entrySet()) {
//
//        String x = entry.getKey();
//
//        for (String y : entry.getValue()) {
//
//          String[] arr = Parser.sort(x, y);
//
//          set.add(arr[0] + " " + arr[1]);
//        }
//      }
//
//      List<String> ret = new ArrayList<String>();
//      for (String s : set) {
//        ret.add(s);
//      }
//
//      return ret;
//    }
    
  }
  
  private static class Parser { // FIX ME: THIS CLASS NEEDS TO REMOVED AFTER THE METHODS
    // ARE IMPLEMENTED INSIDE GRAPH, ALIGNMENT, OR NETWORKALIGNMENT, OR NOT AT ALL
    
//    public static void toAlignmentSIF(String dir, Alignment align, String tagS, String tagL)
//            throws IOException {
//      PrintWriter pw = new PrintWriter(new File(dir));
//
//      List<String> Sall = Alignment.writeEdges(align.small_A);
//      List<String> Lall = Alignment.writeEdges(align.large_A);
//
//      Collections.sort(Sall);
//      Collections.sort(Lall);
//
//      List<String> CC = new ArrayList<String>();
//      for (String x : Sall) {
//        if (Collections.binarySearch(Lall, x) >= 0) {
//          CC.add(x);
//        }
//      }
//
//      for (String s : Sall) {
//        String[] arr = split(s);
//
//        pw.println(arr[0] + " " + tagS + " " + arr[1]);
//      }
//
//      for (String s : Lall) {
//        String[] arr = split(s);
//
//        pw.println(arr[0] + " " + tagL + " " + arr[1]);
//      }
//
//      for (String s : CC) {
//        String[] arr = split(s);
//
//        pw.println(arr[0] + " " + "CC" + " " + arr[1]);
//      }
//
//      pw.close();
//    }
  
    /**
     * splits parameter by space
     */
    public static String[] split(String twoNodes) {
      StringTokenizer st = new StringTokenizer(twoNodes);
      return new String[]{st.nextToken(), st.nextToken()};
    }
  
    /**
     * splits parameter by dash
     */
    public static String[] splice(String edge) {
      return edge.split("-");
    }
  
    /**
     * sorts two strings in array
     */
    public static String[] sort(String a, String b) {
      String[] ret = {a, b};
      Arrays.sort(ret);
      
      return ret;
    }
  
    /**
     * true if two parts are equal; split by dash
     */
    public static boolean sameNode(String node) {
      String[] arr = node.split("-");
      return arr[0].equals(arr[1]);
    }
    
//    public static void write(String filename, Set<String> set) throws IOException {
//      PrintWriter pw = new PrintWriter(new File(filename));
//      for (String s : set) {
//        String[] arr = Parser.split(s);
//        pw.println(arr[0] + " pp " + arr[1]);
//      }
//
//      pw.close();
//    }
    
  }
  
}
