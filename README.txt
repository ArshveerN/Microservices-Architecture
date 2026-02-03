****************
*HOW TO COMPILE*
****************

Make sure you are at the correct dir, if needed
cd into ./A1
    >>> cd ./A1

Give executing permission to runme:
    >>> chmod +x ./runme.sh

Inorder to compile all Java services:
	>>> ./runme.sh -c
All the .class files should be placed in compiled/<Service Name>


*****************
*Running Service*
*****************
All 4 of the servers need be up and running, each of the 4 servers
need to be run in a separate terminal window.

Start Order Service:
	>>> ./runme.sh -o
Start Inter-Service Communication Service (ISCS):
	>>> ./runme.sh -i
Start Product Service:
	>>> ./runme.sh -p
Start the User Service:
	>>> ./runme.sh -u


*************************
*Running Workload Parser*
*************************
In order to run the workload file
	>>> ./runme.sh -w <workload_file>