# /bin/sh

EXEHOME=$1
RESHOME=$2
CURRYEAR=$3


cd $EXEHOME
rm -rf $EXEHOME/Licenses
mkdir $EXEHOME/Licenses
cat $RESHOME/Licenses/LICENSE-README-TEMPLATE.txt | sed "s#__WJRL_CURRYEAR__#$CURRYEAR#" > Licenses/LICENSE-README.txt
cp $RESHOME/Licenses/LICENSE.txt $EXEHOME/Licenses
cp $RESHOME/Licenses/LICENSE-SUN.txt $EXEHOME/Licenses
cp $RESHOME/INSTALL-README.rtf $EXEHOME

