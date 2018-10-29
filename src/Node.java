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

    private HashSet<String> otherNodes = new HashSet<>();
    private ArrayList<Integer> calculatedFingerTable = new ArrayList<Integer>();
    private ArrayList<Integer> actualFingerTable = new ArrayList<Integer>();
    private ArrayList<Integer> dataAtNode = new ArrayList<>();
    private String predecessor = "";
    private String successor = "";

    public Node() {

    }

    public void setAddress(String nodeLocalAddress,
                           int nodePortNumber, int nodeId,
                           int fingerTableSize) {
        this.nodeLocalAddress = nodeLocalAddress;
        this.nodePortNumber = nodePortNumber;
        this.nodeId = nodeId;
        this.fingerTableSize = fingerTableSize;
        this.ringSize = (int) Math.pow(2, fingerTableSize);
        this.thisNodeIdentifier = nodeId + ";" + nodeLocalAddress + ";" +
                nodePortNumber;
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
            this.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void buildFingerTable() {
        int thisNodeId = Integer.parseInt(thisNodeIdentifier.split(";")
                [0]);
        ArrayList<Integer> knownNodeIds = getUpdatedNodeIds();
        int elementsCount = (int) (Math.log(ringSize) / Math.log(2));
        calculatedFingerTable = new ArrayList<Integer>();
        for (int i = 0; i < elementsCount; i++) {
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i)) % ringSize);
        }
        actualFingerTable =
                reconfigureFingerTable(calculatedFingerTable, knownNodeIds);
    }

    public ArrayList<Integer> getUpdatedNodeIds() {
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (String nodeDetails : otherNodes) {
            nodeIds.add(Integer.parseInt(nodeDetails.split(";")[0]));
        }
        Collections.sort(nodeIds);
        return nodeIds;
    }

    public ArrayList<Integer>
    reconfigureFingerTable(ArrayList<Integer> fingerTable,
                           ArrayList<Integer> knownNodeIds) {
        ArrayList<Integer> returnList = new ArrayList<>(fingerTable.size());

        for (int element : fingerTable) {
            if (knownNodeIds.contains(element)) {
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
        otherNodes.add(thisNodeIdentifier);
        int elementsCount = (int) (Math.log(ringSize) / Math.log(2));
        int thisNodeId =
                Integer.parseInt(thisNodeIdentifier.split(";")[0]);
        actualFingerTable = new ArrayList<>();
        for (int i = 0; i < elementsCount; i++) {
            calculatedFingerTable.add((thisNodeId +
                    (int) Math.pow(2, i)) % ringSize);
        }
        for (int i = 0; i < elementsCount; i++) {
            actualFingerTable.add(i,
                    Integer.parseInt(thisNodeIdentifier.split(";")[0]));
        }
        successor = thisNodeIdentifier;
        predecessor = thisNodeIdentifier;
    }

    public void pingAnchorNode(int message) {
        Socket clientSocket = null;

        try {
            clientSocket = new Socket("localhost", anchorPort);
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(message);
            out.writeObject(thisNodeIdentifier);
            out.flush();
        } catch (ConnectException e) {
            System.out.println("Anchor Node is Offline");
            System.out.println("Terminate and Start Again");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendToClient(String locationFound, String clientRequest){
        Socket clientSocket = null;
        String dataArray[] = clientRequest.split(";");
        try {
            clientSocket = new Socket(dataArray[1],Integer.parseInt(dataArray[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(19);
            out.writeObject(locationFound);
            out.flush();
        } catch (ConnectException e) {
            System.out.println("Client is Offline");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void universalGlobalConnector(String contactNode, String message,
                                         int functionID) {
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
            out.writeObject(travelledNodes);
            out.writeObject(createdNode);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDeleteMessage(ArrayList dataStored,
                                  String[] nodeDetails, HashSet travelledSet) {
        Socket clientSocket = null;
        String contactDetails[] = getSuccessor().split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(14);
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
                                                     dataToBeReturned) {
        Socket clientSocket = null;
        String contactDetails[] = contactNode.split(";");

        try {
            clientSocket = new Socket(contactDetails[1],
                    Integer.parseInt(contactDetails[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(16);
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
        int dataToBeAdded = Integer.parseInt(dataToBeAddedString);

        if (thisNodeIdentifier.equals(getSuccessor())) {
            if(!(dataAtNode.contains(dataToBeAdded))){
                dataAtNode.add(dataToBeAdded);
            }
            Collections.sort(dataAtNode);
        } else {
            boolean specialCondition = true;
            for (int i = fingerTableSize - 1; i >= 0; i--) {
                if (numericIn(actualFingerTable.get(i), nodeId,
                        dataToBeAdded)) {
                    specialCondition = false;
                    universalGlobalConnector(getDetailsByID(
                            actualFingerTable.get(i)), dataToBeAddedString,
                            12);
                    break;
                }
            }
            if (specialCondition) {
                universalGlobalConnector(thisNodeIdentifier,
                        dataToBeAddedString, 12);
            }
        }
    }


    public void findSuccessor(String newDiscoveredNode) {

        if (thisNodeIdentifier.equals(getSuccessor())) {
            universalGlobalConnector(newDiscoveredNode, thisNodeIdentifier,
                    4);
        } else {
            boolean specialCondition = true;
            for (int i = fingerTableSize - 1; i >= 0; i--) {
                String relevantNodeDetails =
                        getDetailsByID(actualFingerTable.get(i));
                if (In(relevantNodeDetails, thisNodeIdentifier,
                        newDiscoveredNode)) {
                    specialCondition = false;
                    universalGlobalConnector(relevantNodeDetails,
                            newDiscoveredNode, 9);
                    break;
                }
            }
            if (specialCondition) {
                universalGlobalConnector(newDiscoveredNode, getSuccessor(),
                        4);
            }
        }
    }

    public String getDetailsByID(int findNodeId) {
        String returnString = "PROBLEM";
        for (String value : otherNodes) {
            if (findNodeId == Integer.parseInt(value.split(";")[0])) {
                returnString = value;
                break;
            }
        }
        return returnString;
    }

    public boolean In(String valueToBeChecked, String node1, String node2) {
        boolean status = false;
        int valueToBeCheckedID =
                Integer.parseInt(valueToBeChecked.split(";")[0]);
        int node1ID = Integer.parseInt(node1.split(";")[0]);
        node1ID = (node1ID + 1) % ringSize;
        int node2ID = Integer.parseInt(node2.split(";")[0]);

        if (node1.equals(node2)) {
            status = false;
        } else {
            while (node1ID != node2ID) {
                if (valueToBeCheckedID == node1ID) {
                    status = true;
                    break;
                }
                node1ID = (node1ID + 1) % ringSize;
            }
        }
        return status;

    }

    public boolean numericIn(int valueToBeChecked, int node1, int node2) {
        boolean status = false;
        node1 = (node1 + 1) % ringSize;

        if (node1 == node2) {
            status = false;
        } else {
            while (node1 != node2) {
                if (valueToBeChecked == node1) {
                    status = true;
                    break;
                }
                node1 = (node1 + 1) % ringSize;
            }
        }

        return status;
    }

    public String getSuccessor() {
        return this.successor;
    }

    public String getPredecessor() {
        // System.out.println("getPredecessor: " + this.predecessor);
        return this.predecessor;
    }

    public boolean specialConditionMet(int dataToBeChecked, int localLimit,
                                       int predLimit) {

        int backup1 = dataToBeChecked;
        int count1 = 0;
        int backup2 = dataToBeChecked;
        int count2 = 0;

        while (backup1 != localLimit) {
            backup1 = (backup1 + 1) % ringSize;
            count1++;
        }

        while (backup2 != predLimit) {
            backup2 = (backup2 + 1) % ringSize;
            count2++;
        }

        if (count2 < count1) {
            return true;
        } else {
            return false;
        }
    }

    public void displayDetailsAboutNode() {
        System.out.println("Node ID: " + nodeId);
        System.out.println("Address: " + nodeLocalAddress);
        System.out.println("Port: " + nodePortNumber);
        System.out.println("Ring Size: " + ringSize);
        System.out.println("Predecessor: " + getPredecessor());
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
                    String nodeToBeReRouted = (String) in.readObject();
                    if (nodeToBeReRouted.equals(thisNodeIdentifier)) {
                        buildFingerTableSolo();
                    } else {
                        findSuccessor(nodeToBeReRouted);
                    }
                } else if (code == 4) {
                    String newSuccessor = (String) in.readObject();
                    this.successor = newSuccessor;
                    universalGlobalConnector(newSuccessor,
                            this.thisNodeIdentifier, 5);

                } else if (code == 5) {
                    String newPredecessor = (String) in.readObject();
                    universalGlobalConnector(newPredecessor, this.predecessor,
                            6);
                    this.predecessor = newPredecessor;
                } else if (code == 6) {
                    String newPredecessor = (String) in.readObject();
                    this.predecessor = newPredecessor;
                    universalGlobalConnector(this.predecessor,
                            thisNodeIdentifier, 7);
                    sleep(1000);
                    // Time to tell successor to update finger table
                    HashSet<String> travelledNodes = new HashSet<>();
                    travelledNodes.add(thisNodeIdentifier);
                    tellSuccessorToUpdateFingerTable(getSuccessor(),
                            travelledNodes, thisNodeIdentifier);
                } else if (code == 7) {
                    System.out.println("Code 7");
                    String newSuccessor = (String) in.readObject();
                    this.successor = newSuccessor;
                } else if (code == 8) {
                    @SuppressWarnings("unchecked")
                    HashSet<String> travelledNodes =
                            (HashSet<String>) in.readObject();
                    String newAddedNode = (String) in.readObject();
                    if (thisNodeIdentifier.equals(newAddedNode)) {
                        System.out.println("All Finger Tables Updated After" +
                                " Join");
                        otherNodes.addAll(travelledNodes);
                        buildFingerTable();
                        universalGlobalConnector(getSuccessor(),
                                thisNodeIdentifier, 15);
                    } else {
                        // Perform Set Union
                        otherNodes.addAll(travelledNodes);
                        travelledNodes.add(thisNodeIdentifier);
                        buildFingerTable();
                        tellSuccessorToUpdateFingerTable(successor,
                                travelledNodes, newAddedNode);
                    }
                } else if (code == 9) {
                    String newSuccessor = (String) in.readObject();
                    universalGlobalConnector(newSuccessor,
                            this.getSuccessor(), 4);
                } else if (code == 11) {
                    String dataToBeAdded = (String) in.readObject();
                    findSuccessorForData(dataToBeAdded);
                } else if (code == 12) {
                    String dataToBeAdded = (String) in.readObject();
                    universalGlobalConnector(this.getSuccessor(),
                            dataToBeAdded, 13);
                } else if (code == 13) {
                    String dataToBeAdded = (String) in.readObject();
                    int dataToBeAddedNumeric = Integer.parseInt(dataToBeAdded);
                    if(!(dataAtNode.contains(dataToBeAddedNumeric))){
                        dataAtNode.add(dataToBeAddedNumeric);
                    }
                    Collections.sort(dataAtNode);
                } else if (code == 14) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Integer> dataStored
                            = (ArrayList<Integer>) in.readObject();
                    String[] nodeDetails = (String[]) in.readObject();
                    @SuppressWarnings("unchecked")
                    HashSet<String> travelledSet
                            = (HashSet<String>) in.readObject();

                    if (!(travelledSet.contains(thisNodeIdentifier))) {
                        if (dataStored.size() > 0) {
                            dataAtNode.addAll(dataStored);
                            dataStored.clear();
                        }
                        String removedNode = nodeDetails[0];
                        String removedNodePredecessor = nodeDetails[1];
                        String removedNodeSuccessor = nodeDetails[2];
                        otherNodes.remove(removedNode);
                        if (getPredecessor().equals(removedNode)) {
                            predecessor = removedNodePredecessor;
                        }
                        if (getSuccessor().equals(removedNode)) {
                            successor = removedNodeSuccessor;
                        }
                        travelledSet.add(thisNodeIdentifier);
                        buildFingerTable();
                        sendDeleteMessage(dataStored, nodeDetails,
                                travelledSet);
                    } else {
                        System.out.println("All Finger Tables Updated" +
                                " After Drop");
                    }
                } else if (code == 15) {
                    String newlyAddedNode = (String) in.readObject();
                    int newlyAddedNodeNumber =
                            Integer.parseInt(newlyAddedNode.split(";")
                                    [0]);
                    ArrayList<Integer> returnDataList = new ArrayList<>();
                    for (Integer dataUnit : dataAtNode) {
                        if (specialConditionMet(dataUnit, nodeId,
                                newlyAddedNodeNumber)) {
                            returnDataList.add(dataUnit);
                        }
                    }
                    for (Integer dataUnit : returnDataList) {
                        dataAtNode.remove(dataUnit);
                    }
                    sendCorrectDataToPredecessor(getPredecessor(),
                            returnDataList);

                } else if (code == 16) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Integer> newData
                            = (ArrayList<Integer>) in.readObject();
                    dataAtNode.addAll(newData);
                }

                else if (code == 18) {
                    String clientRequest = (String) in.readObject();
                    int dataRequest =
                            Integer.parseInt(clientRequest.split(";")[0]);
                    if (dataAtNode.contains(dataRequest)){
                        System.out.println("if");
                        sendToClient(thisNodeIdentifier, clientRequest);
                    }
                    else{
                        System.out.println("esle");
                        universalGlobalConnector(getSuccessor(),
                                clientRequest, 18);
                    }
                }


            } catch (IOException | InterruptedException |
                    ClassNotFoundException e) {
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
                n1.onDelete();
            }
        });

    }
}