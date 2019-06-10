# /bin/bash

INSTALLER_HOME=$1
RESHOME=$2
CURRYEAR=$3

cd $INSTALLER_HOME
rm -rf $INSTALLER_HOME/Licenses
mkdir $INSTALLER_HOME/Licenses
cat $RESHOME/Licenses/LICENSE-README-TEMPLATE.txt | sed "s#__WJRL_CURRYEAR__#$CURRYEAR#" > Licenses/LICENSE-README.txt
cp $RESHOME/Licenses/LICENSE.txt $RESHOME/Licenses/LICENSE-SUN.txt \
$RESHOME/Licenses/launch4j-head-LICENSE.txt $RESHOME/Licenses/NSIS-COPYING.txt $INSTALLER_HOME/Licenses
find $INSTALLER_HOME/Licenses -name .DS_Store -delete








