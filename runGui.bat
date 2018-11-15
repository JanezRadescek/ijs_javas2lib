set EXEC_DIR=%0\..

java -cp %EXEC_DIR%\out\production\gui\;%EXEC_DIR%\out\production\ijs_javas2lib\;%EXEC_DIR%\out\production\s2-java-lib\;%EXEC_DIR%\out\production\pcardtimesync\;%EXEC_DIR%\lib\commons-cli-1.4\commons-cli-1.4.jar;%EXEC_DIR%\lib\miglayout-core-4.2.jar;%EXEC_DIR%\lib\miglayout-4.0-swing.jar gui.BaurOkno2 $@
