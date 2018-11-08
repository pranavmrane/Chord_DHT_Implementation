Jump into superuser
	
	sudo su

Install Docker Image
	
	docker pull peiworld/csci652

Check if the image is installed. Should be among the listed images.
	
	docker images

Check docker containers available. None should be available now.
	
	docker ps -a

Incase containers need to be removed, following command removes all containers
	
	docker rm $(docker ps -aq)

Create Containers. The count of nodes is 4. It can be more than that is required.
The command will take create a container take the user inside the container.
So it best to run this command on different terminal windows.
The shortcut to open multiple terminal windows in Linux is Ctrl+Alt+T
	
	docker run -it --name=chordnode1 peiworld/csci652
	docker run -it --name=chordnode2 peiworld/csci652
	docker run -it --name=chordnode3 peiworld/csci652
	docker run -it --name=chordnode4 peiworld/csci652
	docker run -it --name=chordanchor1 peiworld/csci652
	docker run -it --name=chordclient1 peiworld/csci652

Find address of Anchor Node. This needs to be mentioned in code. 
	
	docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' chordanchor1

Move Project Folder into every container. This is done on a separate terminal and all changes will be reflected in the individual containers.
	
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordnode1:/Chord1
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordnode2:/Chord1
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordnode3:/Chord1
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordnode4:/Chord1
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordanchor1:/Chord1
	docker cp /home/pranavmrane/IdeaProjects/Chord1 chordclient1:/Chord1

In the terminal of every container, you can check to see if new folder(Chord1) is created using:
	
	ls

Compile code inside every container.
	
	cd Chord1/src
	javac *.java

Run the following commands on specified containers to start the program:
	
	chordanchor1:
	java AnchorNode -limit 3 -fingertablesize 4
	chordnode1:
	java Node -port 4001 -ID 1 -fingertablesize 4 -anchoraddress 172.17.0.6
	chordnode2:
	java Node -port 4004 -ID 4 -fingertablesize 4 -anchoraddress 172.17.0.6
	chordnode3:
	java Node -port 4007 -ID 7 -fingertablesize 4 -anchoraddress 172.17.0.6
	chordnode4:
	java Node -port 4011 -ID 11 -fingertablesize 4 -anchoraddress 172.17.0.6
	chordclient1:
	java ClientEnv -port 4020 -fingertablesize 4 -anchoraddress 172.17.0.6

Options to add Data are available at client Terminal

To get out of of containers and come to back to terminal type inside container:
	
	exit

To exit superuser use:
	
	exit