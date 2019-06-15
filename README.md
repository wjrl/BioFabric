BioFabric
=========

BioFabric is an open-source network visualization tool that uses a new approach: nodes are represented as horizontal lines instead of as points:

![Les Miserables Network](http://www.biofabric.org/gallery/images/LesMiz1024.png)

[Super-Quick Demo D3 Animation](http://www.biofabric.org/gallery/pages/SuperQuickBioFabric.html)

---


Work on Version 2.0 is continuing (June 2019). It is currently available as [Version 2.0 Beta Release 2](https://github.com/wjrl/BioFabric/releases/tag/V2.0Beta2), and bug fixing continues with frequent merges into the master branch.

Version 2 contains a number of new features, many of which are dedicated to handling *very* large networks. While there are still bugs being addressed, this new version is the only practical way to deal with large networks with over a million links.

* **Significant** performance improvements. The 280K node, 2.3M edge test network now loads in a couple minutes, in a memory footprint of under 8 GB.

* Long-running operations are tracked with frequent progress reports and cancel options.

* If a long running operation is canceled, the previous network can be restored. This feature uses a copy written to a cached file, thus reducing memory requirements.

* To handle very large networks, where e.g. there may be hundreds or thousands of node or link lines per pixel, a special *bucket renderer* has been written. This renderer does not draw lines, but simply counts the nodes and links per pixel to shade each pixel accordingly.

* Object creation has been kept to a minimum, to minimize garbage collection while processing large networks. The images, byte arrays, and integer arrays used for the tiling renderer are tightly managed and efficiently reused.   

* Intersection testing uses a new quadtree data structure, resulting in massive improvements in mouse responsiveness with huge networks. 

* The navigation pane on the bottom of the window can be resized or hidden entirely.

* There is a new plugin architecture to allow developers to write extensions to the core program.

* There is a new batch mode that allows the code to write out a PNG in headless mode, allowing renderings on e.g. cloud machines with huge amounts of physical memory.

* **Node and Link Annotations** have been implemented. This allows the user to label blocks of links and nodes with shaded rectangles, to add semantic information to networks.

* Five new layouts have been added; some of these use the new node and link annotation feature to e.g. label clusters.

* Shadow links can be toggled from a button on the toolbar.

* **NEW in Beta release 2** The mouse scroll wheel now zooms the main view, and scrolling can be accomplished by pressing the left mouse button and dragging.

* **NEW in Beta release 2** Java heap limit for all three platform executables has been increased to 8 GB. It can be tweaked most easily on the Linux version, which sets the value in a shell script. 

* Basic [GW files](http://www.algorithmic-solutions.info/leda_manual/GW.html) can be imported, link groups can be either gathered per node or per network, and multiple node zones per node can be labeled (hat tip to [Rishi Desai](https://github.com/RishiDesai) for these features).

---

This Version 2.0 Beta Release 2 replaces Version 2.0 Beta Release 1.

The current stable production release (Version 1, released 2012) is available as the V1.0 tag.

