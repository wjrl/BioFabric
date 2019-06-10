# /bin/bash

INSTALLER_HOME=$1
RESHOME=$2
VERCOMP=$3

cd $INSTALLER_HOME

cp $RESHOME/README-INSTALL.txt .

find $INSTALLER_HOME -name .DS_Store -delete

zip BioFabric$VERCOMP.zip BioFabricInstaller.exe README-INSTALL.txt







