; This is a script that is run by the NSIS installer builder
; On Mac, it can be installed via MacPorts using "sudo port install nsis"
; Then run using "makensis BioFabInstaller.nsi"

; Name of our application
Name "BioFabric"

; The file to write
OutFile "BioFabricInstaller.exe"

; Set the default Installation Directory (this is the 64 bit directory)
InstallDir "$PROGRAMFILES64\BioFabric"

; Tested (Windows 10): If user elects to run the installer as Admin, they *can* write to the Program Files directory
; If we do not set this level, Windows will *insist* on an admin PIN.
RequestExecutionLevel user

; Set the text which prompts the user to enter the installation directory
DirText "Please choose the directory where you want to install BioFabric."

; ----------------------------------------------------------------------------------
; *************************** SECTION FOR INSTALLING *******************************
; ----------------------------------------------------------------------------------

Section "" 

SetOutPath "$INSTDIR\lib\__WJRL_JDK_VER__"
File /r "lib\__WJRL_JDK_VER__\*"

SetOutPath "$INSTDIR\LICENSES"
File /r "LICENSES\*"

SetOutPath "$INSTDIR\"
File "BioFabric.exe"

MessageBox MB_OK "Installation was successful."

SectionEnd
