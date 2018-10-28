import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Node extends Thread {


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
    private ArrayList<Integer> dataAtNode = new ArrayList<>();
    private String predecessor = "";
    private String successor = "";

    public Node() {

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
                           int fingerTableSize) {
        this.nodeLocalAddress = nodeLocalAddress;
        this.nodePortNumber = nodePortNumber;
        this.nodeId = nodeId;
        this.fingerTableSize = fingerTableSize;
        this.ringSize = (int) Math.pow(2, fingerTableSize);
        this.thisNodeIdentifier = nodeId + ";" + nodeLocalAddress + ";" + nodePortNumber;
        this.predecessor = thisNodeIdentifier;
        this.successor = thisNodeIdentifier;
    }

    public void printAddresses() {
        System.out.println("Sending Address: " + nodeLocalAddress);
        System.out.println("Listening Port: " + nodePortNumber);
        System.out.println("Node ID: " + nodeId);
        System.out.println("Ring Size: " + ringSize);
        System.out.println("Predecessor: " + predecessor);
        System.out.println("Successor: " + successor);
        System.out.println();
    }

    public void startService() {
        System.out.println("startService" + nodeId);
        try {
            ss = new ServerSocket(nodePortNumber, 100,
                    Inet4Address.getByName(nodeLocalAddress));
//            Node n2 = new Node(ss, thisNodeIdentifier);
            this.start();
        } catch (IOException ex) {
//            System.out.println("Check if Port already in use");
            ex.printStackTrace();
        }
    }

    public void buildFingerTable() {
//        System.out.println("buildFingerTable");
        int thisNodeId = Integer.parseInt(thisNodeIdentifier.split(";")[0]);
        ArrayList<Integer> knownNodeIds = getUpdatedNodeIds();
//        System.out.println("nodeIds" + knownNodeIds);
        int elementsCount = (int) (Math.log(ringSize) / Math.log(2));
        calculatedFingerTable = new ArrayList<Integer>();
        for (int i = 0; i < elementsCount; i++) {
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i)) % ringSize);
        }
//        System.out.println("Calculated FingerTable: " + calculatedFingerTable);
        actualFingerTable = reconfigureFingerTable(calculatedFingerTable, knownNodeIds);
//        System.out.println("New FingerTable: " + actualFingerTable);
//        System.out.println("Pre:" + getPredecessor());
//        System.out.println("Succ:" + getSuccessor());
    }

    public ArrayList<Integer> getUpdatedNodeIds() {
//        System.out.println("getUpdatedNodeIds");
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (String nodeDetails : otherNodes) {
            nodeIds.add(Integer.parseInt(nodeDetails.split(";")[0]));
        }
        Collections.sort(nodeIds);
        return nodeIds;
    }

    public ArrayList<Integer> reconfigureFingerTable(ArrayList<Integer> fingerTable,
                                                     ArrayList<Integer> knownNodeIds) {
//        System.out.println("reconfigureFingerTable");
        ArrayList<Integer> returnList = new ArrayList<>(fingerTable.size());

        for (int element : fingerTable) {
            if (knownNodeIds.contains(element)) {
//                System.out.println("If");
                returnList.add(element);
            } else {
                int alternateElement = element;
                while (!(knownNodeIds.contains(alternateElement %
                        (int) (Math.pow(2, fingerTable.size()))))) {

                    alternateElement = alternateElement + 1;
                }
                returnList.add(alternateElement %
                        (int) (Math.pow(2, fingerTable.size())));
            }
        }

        return returnList;

    }

    public void buildFingerTableSolo() {
//        System.out.println("buildFingerTableSolo");
        otherNodes.add(thisNodeIdentifier);

        int elementsCount = (int) (Math.log(ringSize) / Math.log(2));

        int thisNodeId = Integer.parseInt(thisNodeIdentifier.split(";")[0]);

        actualFingerTable = new ArrayList<>();
        for (int i = 0; i < elementsCount; i++) {
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i)) % ringSize);
        }

//        System.out.println("calculated: " + calculatedFingerTable);

        for (int i = 0; i < elementsCount; i++) {
            actualFingerTable.add(i,
                    Integer.parseInt(thisNodeIdentifier.split(";")[0]));
        }

//        System.out.println("actualFingerTable: " + actualFingerTable);
        successor = thisNodeIdentifier;
        predecessor = thisNodeIdentifier;
    }

    public void pingAnchorNode(int message) {
//        System.out.println("pingAnchorNode");
        Socket clientSocket = null;

        try {
//            System.out.println("Sending Information to Anchor Node");
            clientSocket = new Socket("localhost", anchorPort);
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(message);
//            System.out.println("This Node Identifier" + thisNodeIdentifier);
            out.writeObject(thisNodeIdentifier);
            out.flush();
        } catch (ConnectException e) {
//            System.out.println("Anchor Node is Offline");
//            System.out.println("Terminate and Start Again");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void universalGlobalConnector(String contactNode, String message,
                                         int functionID) {
//        System.out.println("ContactNode: " + contactNode);
//        System.out.println("Message: " + message);
//        System.out.println("Function ID: " + functionID);

        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(functionID);
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tellSuccessorToUpdateFingerTable(String contactNode,
                                                 HashSet<String> travelledNodes,
                                                 String createdNode) {
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
//            System.out.println("Travelled Node:" + travelledNodes);
//            System.out.println("Created Node:" + createdNode);
            out.writeObject(travelledNodes);
            out.writeObject(createdNode);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDeleteMessage(ArrayList dataStored,
                                  String[] nodeDetails, HashSet travelledSet) {
//        System.out.println("sendDeleteMessage");
        Socket clientSocket = null;
        String contactDetails[] = getSuccessor().split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(14);
//            System.out.println("dataStored: " + dataStored);
//            System.out.println("nodeDetails: " + nodeDetails);
//            System.out.println("Travelled Nodes:" + travelledSet);
            out.writeObject(dataStored);
            out.writeObject(nodeDetails);
            out.writeObject(travelledSet);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCorrectDataToPredecessor(String contactNode,
                                             ArrayList<Integer>
                                                     dataToBeReturned){
//        System.out.println("sendCorrectDataToPredecessor");
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(16);
//            System.out.println("contactNode: " + contactNode);
//            System.out.println("dataToBeReturned: " + dataToBeReturned);
            out.writeObject(dataToBeReturned);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onDelete() {
        pingAnchorNode(2);

        String[] nodeDetails = new String[3];
        nodeDetails[0] = thisNodeIdentifier;
        nodeDetails[1] = getPredecessor();
        nodeDetails[2] = getSuccessor();
        HashSet<String> travelledList = new HashSet<>();

        sendDeleteMessage(dataAtNode, nodeDetails, travelledList);

    }

    public void findSuccessorForData(String dataToBeAddedString) {

//        System.out.println("findSuccessorForData");
        int dataToBeAdded = Integer.parseInt(dataToBeAddedString);

        if (thisNodeIdentifier.equals(getSuccessor())) {
//            System.out.println("This is only one node in System");
            //
            dataAtNode.add(dataToBeAdded);
            Collections.sort(dataAtNode);
//            System.out.println("dataAtNode: " + dataAtNode);
        } else {
            boolean specialCondition = true;
//            System.out.println("findSuccessor: ELSE");
            for (int i = fingerTableSize - 1; i >= 0; i--) {
                if (numericIn(actualFingerTable.get(i), nodeId, dataToBeAdded)) {
                    specialCondition = false;
                    universalGlobalConnector(getDetailsByID(actualFingerTable.get(i)), dataToBeAddedString, 12);
//                    sendDataToYourSuccessor(getDetailsByID(actualFingerTable.get(i)), dataToBeAddedString);
                    break;
                }
            }
            if (specialCondition) {
//                System.out.println("Magic Else");
                universalGlobalConnector(thisNodeIdentifier, dataToBeAddedString, 12);
//                sendDataToYourSuccessor(thisNodeIdentifier, dataToBeAddedString);
            }
        }
    }


    public void findSuccessor(String newDiscoveredNode) {

//        System.out.println("findSuccessor");

        if (thisNodeIdentifier.equals(getSuccessor())) {
//            System.out.println("This is only one node in System");
//            updateSuccessorForSomeNode(newDiscoveredNode, thisNodeIdentifier);
            universalGlobalConnector(newDiscoveredNode, thisNodeIdentifier, 4);
        } else {
            boolean specialCondition = true;
//            System.out.println("findSuccessor: ELSE");
            for (int i = fingerTableSize - 1; i >= 0; i--) {
                String relevantNodeDetails = getDetailsByID(actualFingerTable.get(i));
                if (In(relevantNodeDetails, thisNodeIdentifier, newDiscoveredNode)) {
                    specialCondition = false;
//                    sendYourSuccessor(relevantNodeDetails, newDiscoveredNode);
                    universalGlobalConnector(relevantNodeDetails, newDiscoveredNode, 9);
                    break;
                }
            }
            if (specialCondition) {
//                System.out.println("Magic Else");
//                updateSuccessorForSomeNode(newDiscoveredNode, getSuccessor());
                universalGlobalConnector(newDiscoveredNode, getSuccessor(), 4);
            }
        }
    }

    public String getDetailsByID(int findNodeId) {
//        System.out.println("getDetailsByID");
//        System.out.println("findNodeId" + findNodeId);
//        System.out.println("otherNodes" + otherNodes);

        String returnString = "PROBLEM";
        for (String value : otherNodes) {
            if (findNodeId == Integer.parseInt(value.split(";")[0])) {
                returnString = value;
                break;
            }
        }
//        System.out.println("Details:" + returnString);
        return returnString;
    }

    public boolean In(String valueToBeChecked, String node1, String node2) {
//        System.out.println("In");

        boolean status = false;

        int valueToBeCheckedID =
                Integer.parseInt(valueToBeChecked.split(";")[0]);
        int node1ID = Integer.parseInt(node1.split(";")[0]);
        node1ID = (node1ID + 1) % ringSize;
        int node2ID = Integer.parseInt(node2.split(";")[0]);

        if (node1.equals(node2)) {
//            System.out.println("In:IF");
            status = false;
        } else {
//            System.out.println("In:ELSE");
            while (node1ID != node2ID) {
                if (valueToBeCheckedID == node1ID) {
                    status = true;
                    break;
                }
                node1ID = (node1ID + 1) % ringSize;
            }
        }

//        System.out.println("Boolean Status:" + status);

        return status;

    }

    public boolean numericIn(int valueToBeChecked, int node1, int node2) {
//        System.out.println("Numeric In");

        boolean status = false;
        node1 = (node1 + 1) % ringSize;

        if (node1 == node2) {
//            System.out.println("In:IF");
            status = false;
        } else {
//            System.out.println("In:ELSE");
            while (node1 != node2) {
                if (valueToBeChecked == node1) {
                    status = true;
                    break;
                }
                node1 = (node1 + 1) % ringSize;
            }
        }

//        System.out.println("Boolean Status:" + status);

        return status;
    }

    public String getSuccessor() {
        // System.out.println("getSuccessor: " + this.successor);
        return this.successor;
    }

    public String getPredecessor() {
        // System.out.println("getPredecessor: " + this.predecessor);
        return this.predecessor;
    }


    public void displayDetailsAboutNode(){
        System.out.println("Node ID: " + nodeId);
        System.out.println("Address: " + nodeLocalAddress);
        System.out.println("Port: " + nodePortNumber);
        System.out.println("Ring Size: " + ringSize);
        System.out.println("Predecessor: "+ getPredecessor());
        System.out.println("Successor: " + getSuccessor());
        System.out.println("Calculated FingerTable: " + calculatedFingerTable);
        System.out.println("Actual FingerTable: " + actualFingerTable);
        System.out.println("Data Stored: " + dataAtNode);
        System.out.println();

    }

    public void run() {
        System.out.println("Node" + nodeId + " Listening");
        pingAnchorNode(1);
        while (true) {
            try {
                Socket s = ss.accept();
                ObjectInputStream in =
                        new ObjectInputStream(s.getInputStream());
                int code = in.readInt();

                if (code == 3) {
                    // Received new node to be redirected
                    // Anchor node sent this new node
//                    System.out.println("Code 3");
                    String nodeToBeReRouted = (String) in.readObject();
                    if (nodeToBeReRouted.equals(thisNodeIdentifier)) {
//                        System.out.println("This node is the only one in " +
//                                "the ring");
                        buildFingerTableSolo();
                    } else {
//                        System.out.println("Node Request received from anchor");
                        int newDiscoveredNode =
                                Integer.parseInt(nodeToBeReRouted.split(";")[0]);
//                        System.out.println("newDiscoveredNode" + newDiscoveredNode);
//
//                        System.out.println("Node needs to be routed");
//                        System.out.println("Finding successor for new node");
                        findSuccessor(nodeToBeReRouted);
                    }
                } else if (code == 4) {
//                    System.out.println("Code 4");
                    String newSuccessor = (String) in.readObject();
//                    System.out.println("New Successor Received:" + newSuccessor);
                    this.successor = newSuccessor;
//                    System.out.println("Succ" + getSuccessor());
//                    System.out.println("Pre" + getPredecessor());
//                    System.out.println("New Successor's Predecessor Needs " +
//                            "to be updated");
//                    updatePredecessorForSuccessor(newSuccessor,
//                            this.thisNodeIdentifier);
                    universalGlobalConnector(newSuccessor,
                            this.thisNodeIdentifier, 5);

                } else if (code == 5) {
//                    System.out.println("Code 5");
                    String newPredecessor = (String) in.readObject();
//                    System.out.println("New Predecessor Received:" + newPredecessor);
                    // Save old Predecessor
//                    System.out.println("this.predecessor" + this.predecessor);
                    // updatePredecessorForPredecessor(newPredecessor, this.predecessor);
                    universalGlobalConnector(newPredecessor, this.predecessor, 6);
                    this.predecessor = newPredecessor;
//                    System.out.println("Pre" + getPredecessor());
//                    System.out.println("Succ" + getSuccessor());
                } else if (code == 6) {
                    System.out.println("Code 6");
                    String newPredecessor = (String) in.readObject();
//                    System.out.println("New Predecessor Received:" + newPredecessor);
                    this.predecessor = newPredecessor;
//                    System.out.println("Pre" + getPredecessor());
//                    System.out.println("Succ" + getSuccessor());
//                    System.out.println("Predecessor's Successor needs to be updated");
                    // updateSuccessorForPredecessor(this.predecessor, thisNodeIdentifier);
                    universalGlobalConnector(this.predecessor, thisNodeIdentifier, 7);
                    sleep(1000);
                    // Time to tell successor to update finger table
                    HashSet<String> travelledNodes = new HashSet<>();
                    travelledNodes.add(thisNodeIdentifier);
                    tellSuccessorToUpdateFingerTable(getSuccessor(), travelledNodes, thisNodeIdentifier);
                } else if (code == 7) {
                    System.out.println("Code 7");
                    String newSuccessor = (String) in.readObject();
//                    System.out.println("New Successor Received:" + newSuccessor);
                    this.successor = newSuccessor;
//                    System.out.println("Pre" + getPredecessor());
//                    System.out.println("Succ" + getSuccessor());
                } else if (code == 8) {
//                    System.out.println("Code 8");
                    @SuppressWarnings("unchecked")
                    HashSet<String> travelledNodes = (HashSet<String>) in.readObject();
                    String newAddedNode = (String) in.readObject();

//                    System.out.println("Recieved Travelled Nodes:" + travelledNodes);
//                    System.out.println("new Added Node: " + newAddedNode);

                    if (thisNodeIdentifier.equals(newAddedNode)) {
                        System.out.println("We have completed the circle");
                        otherNodes.addAll(travelledNodes);
                        buildFingerTable();
                        universalGlobalConnector(getSuccessor(), thisNodeIdentifier, 15);
                    } else {
                        // Perform Set Union
                        otherNodes.addAll(travelledNodes);
                        travelledNodes.add(thisNodeIdentifier);
                        buildFingerTable();
                        tellSuccessorToUpdateFingerTable(successor,
                                travelledNodes, newAddedNode);
                    }
                } else if (code == 9) {
//                    System.out.println("Code 9");
                    String newSuccessor = (String) in.readObject();
//                    updateSuccessorForSomeNode(newSuccessor, this.getSuccessor());
                    universalGlobalConnector(newSuccessor, this.getSuccessor(), 4);
                } else if (code == 11) {
//                    System.out.println("Code 11");
                    String dataToBeAdded = (String) in.readObject();
                    findSuccessorForData(dataToBeAdded);
                } else if (code == 12) {
//                    System.out.println("Code 12");
                    String dataToBeAdded = (String) in.readObject();
                    universalGlobalConnector(this.getSuccessor(), dataToBeAdded, 13);
                    // saveDataLocally(this.getSuccessor(), dataToBeAdded);
                } else if (code == 13) {
//                    System.out.println("Code 13");
                    String dataToBeAdded = (String) in.readObject();
                    dataAtNode.add(Integer.parseInt(dataToBeAdded));
                    Collections.sort(dataAtNode);
//                    System.out.println("DataAtNode:" + dataAtNode);
                } else if (code == 14) {
//                    System.out.println("Code 14");
                    @SuppressWarnings("unchecked")
                    ArrayList<Integer> dataStored = (ArrayList<Integer>) in.readObject();
                    String[] nodeDetails = (String[]) in.readObject();
                    @SuppressWarnings("unchecked")
                    HashSet<String> travelledSet = (HashSet<String>) in.readObject();

                    if (!(travelledSet.contains(thisNodeIdentifier))) {
                        if (dataStored.size() > 0) {
//                            System.out.println("BDataATNode: "+ dataAtNode);
                            dataAtNode.addAll(dataStored);
//                            System.out.println("ADataATNode: "+ dataAtNode);
                            dataStored.clear();
                        }

                        String removedNode = nodeDetails[0];
                        String removedNodePredecessor = nodeDetails[1];
                        String removedNodeSuccessor = nodeDetails[2];
                        otherNodes.remove(removedNode);
                        if (getPredecessor().equals(removedNode)) {
//                            System.out.println("14: PRE");
                            predecessor = removedNodePredecessor;
                        }
                        if (getSuccessor().equals(removedNode)) {
//                            System.out.println("14: SUCC");
                            successor = removedNodeSuccessor;
                        }
                        travelledSet.add(thisNodeIdentifier);
                        buildFingerTable();
                        sendDeleteMessage(dataStored, nodeDetails, travelledSet);
                    } else {
                        System.out.println("Updated All Nodes on Delete");
                    }
                }
                else if(code == 15){
//                    System.out.println("Code 15");
//                    System.out.println("Successor needs to send an arraylist" +
//                            " to newly added member");
                    String newlyAddedNode = (String)in.readObject();
                    int newlyAddedNodeNumber = Integer.parseInt(newlyAddedNode.split(";")[0]);
                    ArrayList<Integer> returnDataList = new ArrayList<>();
                    for (Integer dataUnit : dataAtNode){
                        if(dataUnit <= newlyAddedNodeNumber){
                            returnDataList.add(dataUnit);
                        }
                    }
                    for (Integer dataUnit : returnDataList){
                        dataAtNode.remove(dataUnit);
                    }
                    sendCorrectDataToPredecessor(getPredecessor(), returnDataList);

                }
                else if(code == 16){
//                    System.out.println("Code 15");
                    @SuppressWarnings("unchecked")
                    ArrayList<Integer> newData = (ArrayList<Integer>)in.readObject();
//                    System.out.println("Before DataNode: " + dataAtNode);
                    dataAtNode.addAll(newData);
//                    System.out.println("After DataNode: " + dataAtNode);
//                    System.out.println("DataUpdate Complete");
                }
            } catch (IOException | InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            displayDetailsAboutNode();
        }
    }
    public static void main(String args[]) throws InterruptedException {

        Node n1 = new Node();

        if ((args.length == 8) && args[0].equals("-address") &&
                args[2].equals("-port") &&
                args[4].equals("-ID") &&
                args[6].equals("-fingertablesize")) {
            System.out.println("Setting Connection Variables:");
            n1.setAddress(args[1], Integer.parseInt(args[3]),
                    Integer.parseInt(args[5]), Integer.parseInt(args[7]));
            n1.printAddresses();
        } else {
            System.err.println("Enter Command Like this: " +
                    "Node -address localhost -port 5001 -ID 1 " +
                    "-fingertablesize 3");
            System.out.println("Using Default Values");
            n1.setAddress("localhost", 5001,
                    1, 2);
            n1.printAddresses();
        }

        n1.startService();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
//                System.out.println("Exited!");
                n1.onDelete();
//                System.out.println(n1.getSuccessor());

            }
        });

    }
}
