import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Node extends Thread{


    private ServerSocket ss = null;
    private String nodeLocalAddress = "0.0.0.0";
    private int nodePortNumber;
    private int nodeId;
    private String thisNodeIdentifier = "";
    private int fingerTableSize = 0;
    private int ringSize = 0;

    private String anchorAddress = "0.0.0.0";
    private int anchorPort = 11000;

//    private int ringSize = 8;
//    private int fingerTableSize = 3;

    private HashSet<String> otherNodes = new HashSet<>();
    private ArrayList<Integer> calculatedFingerTable = new ArrayList<Integer>();
    private ArrayList<Integer> actualFingerTable = new ArrayList<Integer>();
    private String predecessor = "";
    private String successor = "";

    public Node(){

    }

//    public Node(ServerSocket ss, String thisNodeIdentifier) {
//        this.ss = ss;
//        this.thisNodeIdentifier = thisNodeIdentifier;
//        this.predecessor = thisNodeIdentifier;
//        this.successor = thisNodeIdentifier;
//
//    }

    public void setAddress(String nodeLocalAddress,
                           int nodePortNumber, int nodeId,
                           int fingerTableSize){
        this.nodeLocalAddress = nodeLocalAddress;
        this.nodePortNumber = nodePortNumber;
        this.nodeId = nodeId;
        this.fingerTableSize = fingerTableSize;
        this.ringSize = (int)Math.pow(2, fingerTableSize);
        this.thisNodeIdentifier = nodeId + ";" + nodeLocalAddress + ";" + nodePortNumber;
        this.predecessor = thisNodeIdentifier;
        this.successor = thisNodeIdentifier;
    }

    public void printAddresses(){
        System.out.println("Sending Address: " + nodeLocalAddress);
        System.out.println("Listening Port: " + nodePortNumber);
        System.out.println("Node ID: " + nodeId);
        System.out.println("Ring Size: " + ringSize);
        System.out.println("Predecessor: "+ predecessor);
        System.out.println("Successor: " + successor);
    }

    public void startService(){
        System.out.println("startService" + nodeId);
        try{
            ss = new ServerSocket(nodePortNumber, 100,
                    Inet4Address.getByName(nodeLocalAddress));
//            Node n2 = new Node(ss, thisNodeIdentifier);
            this.start();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void buildFingerTable(){
        System.out.println("buildFingerTable");
        int thisNodeId = Integer.parseInt(thisNodeIdentifier.substring(0, 1));
        ArrayList<Integer> knownNodeIds = getUpdatedNodeIds();
//        System.out.println("nodeIds" + knownNodeIds);
        int elementsCount = (int)(Math.log(ringSize)/Math.log(2));
        calculatedFingerTable = new ArrayList<Integer>();
        for(int i=0; i<elementsCount; i++){
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i))%ringSize);
        }
//        System.out.println("Calculated FingerTable: " + calculatedFingerTable);
        actualFingerTable = reconfigureFingerTable(calculatedFingerTable, knownNodeIds);
        System.out.println("New FingerTable: " + actualFingerTable);
        System.out.println("Pre:" + getPredecessor());
        System.out.println("Succ:" + getSuccessor());
    }

    public ArrayList<Integer> getUpdatedNodeIds(){
        System.out.println("getUpdatedNodeIds");
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (String nodeDetails: otherNodes){
            nodeIds.add(Integer.parseInt(nodeDetails.substring(0, 1)));
        }
        Collections.sort(nodeIds);
        return nodeIds;
    }

    public ArrayList<Integer> reconfigureFingerTable(ArrayList<Integer> fingerTable,
                                       ArrayList<Integer> knownNodeIds){
        System.out.println("reconfigureFingerTable");
        ArrayList<Integer> returnList = new ArrayList<>(fingerTable.size());

        for(int element: fingerTable){
            if(knownNodeIds.contains(element)){
//                System.out.println("If");
                returnList.add(element);
            }
            else {
                int alternateElement = element;
                while (!(knownNodeIds.contains(alternateElement %
                                (int)(Math.pow(2, fingerTable.size()))))){

                    alternateElement = alternateElement + 1;
                }
                returnList.add(alternateElement %
                        (int)(Math.pow(2, fingerTable.size())));
            }
        }

        return returnList;

    }

    public void buildFingerTableSolo(){
        System.out.println("buildFingerTableSolo");
        otherNodes.add(thisNodeIdentifier);

        int elementsCount = (int)(Math.log(ringSize)/Math.log(2));

        int thisNodeId = Integer.parseInt(thisNodeIdentifier.substring(0, 1));

        actualFingerTable = new ArrayList<>();
        for(int i=0; i<elementsCount; i++){
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i))%ringSize);
        }

//        System.out.println("calculated: " + calculatedFingerTable);

        for (int i = 0; i<elementsCount; i++){
            actualFingerTable.add(i,
                    Integer.parseInt(thisNodeIdentifier.substring(0, 1)));
        }

//        System.out.println("actualFingerTable: " + actualFingerTable);
        successor = thisNodeIdentifier;
        predecessor = thisNodeIdentifier;
    }

    public void run(){
        System.out.println("Node" + nodeId + " Listening");
        pingAnchorNode();
        while(true){
            try{
                Socket s = ss.accept();
                ObjectInputStream in =
                        new ObjectInputStream(s.getInputStream());
                int code = in.readInt();

                if (code == 3){
                    // Received new node to be redirected
                    // Anchor node sent this new node
                    System.out.println("Code 3");
                    String nodeToBeReRouted = (String)in.readObject();
                    if(nodeToBeReRouted.equals(thisNodeIdentifier)){
//                        System.out.println("This node is the only one in " +
//                                "the ring");
                        buildFingerTableSolo();
                    }
                    else {
//                        System.out.println("Node Request received from anchor");
                        int newDiscoveredNode =
                                Integer.parseInt(nodeToBeReRouted.substring(0, 1));
//                        System.out.println("newDiscoveredNode" + newDiscoveredNode);

//                        System.out.println("Node needs to be routed");
//                        System.out.println("Finding successor for new node");
                        findSuccessor(nodeToBeReRouted);
                    }
                }

                else if(code == 4){
                    System.out.println("Code 4");
                    String newSuccessor = (String)in.readObject();
//                    System.out.println("New Successor Received:" + newSuccessor);
                    this.successor = newSuccessor;
                    System.out.println("Succ" + getSuccessor());
                    System.out.println("Pre" + getPredecessor());
//                    System.out.println("New Successor's Predecessor Needs " +
//                            "to be updated");
                    updatePredecessorForSuccessor(newSuccessor,
                            this.thisNodeIdentifier);

                }
                else if(code == 5) {
                    System.out.println("Code 5");
                    String newPredecessor = (String)in.readObject();
//                    System.out.println("New Predecessor Received:" + newPredecessor);
                    updatePredecessorForPredecessor(newPredecessor, this.predecessor);
                    this.predecessor = newPredecessor;
                    System.out.println("Pre" + getPredecessor());
                    System.out.println("Succ" + getSuccessor());
                }
                else if(code == 6){
                    System.out.println("Code 6");
                    String newPredecessor = (String)in.readObject();
//                    System.out.println("New Predecessor Received:" + newPredecessor);
                    this.predecessor = newPredecessor;
                    System.out.println("Pre" + getPredecessor());
                    System.out.println("Succ" + getSuccessor());
//                    System.out.println("Predecessor's Successor needs to be updated");
                    updateSuccessorForPredecessor(this.predecessor, thisNodeIdentifier);
                    sleep(1000);
                    // Time to tell successor to update finger table
                    HashSet<String> travelledNodes = new HashSet<>();
                    travelledNodes.add(thisNodeIdentifier);
                    tellSuccessorToUpdateFingerTable(getSuccessor(), travelledNodes, thisNodeIdentifier);
                }
                else if(code == 7){
                    System.out.println("Code 7");
                    String newSuccessor = (String)in.readObject();
//                    System.out.println("New Successor Received:" + newSuccessor);
                    this.successor = newSuccessor;
                    System.out.println("Pre" + getPredecessor());
                    System.out.println("Succ" + getSuccessor());
                }
                else if(code == 8){
                    System.out.println("Code 8");
                    HashSet<String> travelledNodes = (HashSet<String>)in.readObject();
                    String newAddedNode = (String)in.readObject();

//                    System.out.println("Recieved Travelled Nodes:" + travelledNodes);
//                    System.out.println("new Added Node: " + newAddedNode);

                    if (thisNodeIdentifier.equals(newAddedNode)){
                        System.out.println("We have completed the circle");
                        otherNodes.addAll(travelledNodes);
                        buildFingerTable();
                    }

                    else{
                        // Perform Set Union
                        otherNodes.addAll(travelledNodes);
                        travelledNodes.add(thisNodeIdentifier);
                        buildFingerTable();
                        tellSuccessorToUpdateFingerTable(successor,
                                travelledNodes, newAddedNode);
                    }
                }
                else if(code == 9){
                    System.out.println("Code 9");
                    String newSuccessor = (String)in.readObject();
                    updateSuccessorForSomeNode(newSuccessor, this.getSuccessor());
                }
            }
            catch (IOException | InterruptedException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    public void tellSuccessorToUpdateFingerTable(String contactNode,
                                                 HashSet<String> travelledNodes,
                                                 String createdNode){
//        System.out.println("tellSuccessorToUpdateFingerTable");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(8);
//            System.out.println("This Node Identifier" + contactNode);
//            System.out.println("Travelled Node:"  + travelledNodes);
//            System.out.println("Created Node:" + createdNode);
            out.writeObject(travelledNodes);
            out.writeObject(createdNode);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void updateSuccessorForSomeNode(String contactNode, String successor){
        System.out.println("updateSuccessorForSomeNode");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(4);
            System.out.println("This Node Identifier" + contactNode);
            out.writeObject(successor);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updatePredecessorForSuccessor(String contactNode,
                                              String predecessor){
        System.out.println("updatePredecessorForSuccessor");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(5);
//            System.out.println("This Node Identifier" + contactNode);
            out.writeObject(predecessor);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updatePredecessorForPredecessor(String contactNode,
                                              String predecessor){
//        System.out.println("updatePredecessorForPredecessor");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(6);
//            System.out.println("This Node Identifier" + contactNode);
            out.writeObject(predecessor);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void updateSuccessorForPredecessor(String contactNode,
                                              String successor){
        System.out.println("updateSuccessorForPredecessor");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(7);
//            System.out.println("This Node Identifier" + contactNode);
            out.writeObject(successor);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void findSuccessor(String newDiscoveredNode) {

        System.out.println("findSuccessor");

        if (thisNodeIdentifier.equals(getSuccessor())) {
            System.out.println("This is only one node in System");
            updateSuccessorForSomeNode(newDiscoveredNode, thisNodeIdentifier);
        } else {
            boolean specialCondition = true;
            System.out.println("findSuccessor: ELSE");
            for (int i = fingerTableSize - 1; i >= 0; i--) {
                String relevantNodeDetails = getDetailsByID(actualFingerTable.get(i));
                if (In(relevantNodeDetails, thisNodeIdentifier, newDiscoveredNode)) {
                    specialCondition = false;
                    sendYourSuccessor(relevantNodeDetails, newDiscoveredNode);
                    break;
                }
            }
            if(specialCondition){
                System.out.println("Magic Else");
                updateSuccessorForSomeNode(newDiscoveredNode, getSuccessor());
            }
        }
    }

    public void sendYourSuccessor(String contactNode,
                                              String successor){
        System.out.println("sendYourSuccessor");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(9);
            System.out.println("This Node Identifier" + contactNode);
            System.out.println("Succ:" + successor);
            out.writeObject(successor);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


//    public String findPredecessor(String newDiscoveredNode){
//        String nodeDash = thisNodeIdentifier;
//        System.out.println(newDiscoveredNode);
//        System.out.println(thisNodeIdentifier);
//        System.out.println(this.getSuccessor());
//        while (!(In(newDiscoveredNode, thisNodeIdentifier, this.getSuccessor()))){
//            nodeDash = closestPrecedingFinger(newDiscoveredNode);
//        }
//
//        return nodeDash;
//
//    }

//    public String closestPrecedingFinger(String newDiscoveredNode){
//        for (int i=fingerTableSize-1; i<=0; i++){
//            String location = getDetailsByID(actualFingerTable.get(i));
//            if(!(location).equals("PROBLEM")){
//                if(In(location, thisNodeIdentifier, newDiscoveredNode)){
//                    return location;
//                }
//            }
//            else {
//                System.out.println("PROBLEM");
//            }
//        }
//    }

    public String getDetailsByID(int findNodeId){
        System.out.println("getDetailsByID");
        System.out.println("findNodeId" + findNodeId);
        System.out.println("otherNodes" + otherNodes);

        String returnString = "PROBLEM";
        for (String value : otherNodes){
            if(findNodeId == Integer.parseInt(value.substring(0, 1))){
                returnString =  value;
                break;
            }
        }
        System.out.println("Details:" + returnString);
        return returnString;
    }

    public boolean In(String valueToBeChecked, String node1, String node2){
        System.out.println("In");

        boolean status = false;

        int valueToBeCheckedID =
                Integer.parseInt(valueToBeChecked.substring(0, 1));
        int node1ID = Integer.parseInt(node1.substring(0, 1));
        int node2ID = Integer.parseInt(node2.substring(0, 1));
        int bigger = 0;
        int smaller = 0;

        if (node1.equals(node2)){
            System.out.println("In:IF");
            status = false;
        }
        else {
            System.out.println("In:ELSE");
            while (node1ID != node2ID){
                if(valueToBeCheckedID == node1ID){
                    status = true;
                    break;
                }
                node1ID = (node1ID + 1)%ringSize;
            }
        }

        System.out.println("Boolean Status:" + status);

        return status;

    }

    public String getSuccessor() {
        // System.out.println("getSuccessor");
        return this.successor;
    }

    public String getPredecessor() {
        // System.out.println("getPredecessor");
        return this.predecessor;
    }

    public void pingAnchorNode(){
        System.out.println("pingAnchorNode");
        Socket clientSocket = null;

        try {
//            System.out.println("Sending Information to Anchor Node");
            System.out.println();
            clientSocket = new Socket("localhost", anchorPort);
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(1);
//            System.out.println("This Node Identifier" + thisNodeIdentifier);
            out.writeObject(thisNodeIdentifier);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws InterruptedException{

        Node n1 = new Node();

        if ((args.length == 8) && args[0].equals("-address") &&
                args[2].equals("-port") &&
                args[4].equals("-ID") &&
                args[6].equals("-fingertablesize")){
            System.out.println("Setting Connection Variables:");
            n1.setAddress(args[1], Integer.parseInt(args[3]),
                    Integer.parseInt(args[5]), Integer.parseInt(args[7]));
            n1.printAddresses();
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "Node -address localhost -port 5001 -ID 1 " +
                            "-fingertablesize 3");
            System.out.println("Using Default Values");
            n1.setAddress("localhost", 5001,
                    1, 2);
            n1.printAddresses();
        }

        n1.startService();

    }
}
