# /bin/sh

EXEHOME=$1
RESHOME=$2
LAUNCH4J_HOME=$3
L4J_WORKING=$EXEHOME/bioFabl4jWorking.xml
VERCOMP=$4
CURRYEAR=$5

echo $EXEHOME
echo $L4J_WORKING
cat $L4J_WORKING

java -jar $LAUNCH4J_HOME/launch4j.jar $L4J_WORKING

#
# This requires that the Mono project signcode tool has been installed:
#


#signcode -spc authenticode.spc -v authenticode.pvk -a sha1 -$ commercial -n BioFabric \
#          -i http://www.BioFabric.org/ -t http://timestamp.verisign.com/scripts/timstamp.dll -tr 10 \ 
#          $EXEHOME/BioFabric.exe

cd $EXEHOME
rm -rf $EXEHOME/Licenses
mkdir $EXEHOME/Licenses
cat $RESHOME/Licenses/LICENSE-README-TEMPLATE.txt | sed "s#__WJRL_CURRYEAR__#$CURRYEAR#" > Licenses/LICENSE-README.txt
cp $RESHOME/Licenses/LICENSE.txt $RESHOME/Licenses/LICENSE-SUN.txt \
$RESHOME/Licenses/launch4j-head-LICENSE.txt $EXEHOME/Licenses
cp $RESHOME/README-INSTALL.txt $EXEHOME

zip BioFabric$VERCOMP.zip Licenses Licenses/LICENSE.txt Licenses/LICENSE-README.txt \
  Licenses/LICENSE-SUN.txt Licenses/launch4j-head-LICENSE.txt BioFabric.exe README-INSTALL.txt







