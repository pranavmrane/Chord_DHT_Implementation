# Instructions to Setup Chord DHT

## PreRequisites
* Java JDK
* Docker

## Clone Repo
```
git clone https://github.com/pranavmrane/Chord_DHT_Implementation.git
cd Chord_DHT_Implementation/
```

## Docker Setup Instructions

Jump into superuser
	
	sudo su

Create and install docker image from docker file. Dockerfile should be in the same folder.
	
	docker build -f Dockerfile -t chord-dht .

Check if the image is installed. Should be among the listed images. The name of the image should be demo/oracle-java
	
	docker images

Check docker containers available. None should be available now.
	
	docker ps -a

Incase containers need to be removed, following command removes all containers
	
	docker rm $(docker ps -aq)

Create Containers. The count of nodes is 4. It can be more than that is required. The command will take create a container take the user inside the container. So it best to run this command on different terminal windows. The shortcut to open multiple terminal windows in Linux is Ctrl+Alt+T
	
	docker run -it --name=chordnode1 chord-dht
	docker run -it --name=chordnode2 chord-dht
	docker run -it --name=chordnode3 chord-dht
	docker run -it --name=chordnode4 chord-dht
	docker run -it --name=chordanchor1 chord-dht
	docker run -it --name=chordclient1 chord-dht

Find address of Anchor Node. This needs to be mentioned during node and client creation. This can only be peformed on a new terminal window.
	
	docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' chordanchor1

Compile code inside every container.
	
	cd /Chord
	javac *.java

Run the following commands on specified containers to start the program. Anchor Node address needs to be specified:
	
	On chordanchor1, run: java AnchorNode -limit 3 -fingertablesize 4
	On chordnode1, run: java Node -port 4001 -ID 1 -fingertablesize 4 -anchoraddress <Anchor_IP_Address>
	On chordnode2, run: java Node -port 4004 -ID 4 -fingertablesize 4 -anchoraddress <Anchor_IP_Address>
	On chordnode3, run: java Node -port 4007 -ID 7 -fingertablesize 4 -anchoraddress <Anchor_IP_Address>
	On chordnode4, run: java Node -port 4011 -ID 11 -fingertablesize 4 -anchoraddress <Anchor_IP_Address>
	On chordclient1, run: java ClientEnv -port 4020 -fingertablesize 4 -anchoraddress <Anchor_IP_Address>

**Options to add Data will be available at client Terminal**

To get out of of containers and come to back to terminal type inside container:
	
	exit

To exit superuser use:
	
	exit
