import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class AnchorNode extends Thread {

    private ServerSocket ss = null;
    private String anchorLocalAddress = "0.0.0.0";
    private int anchorPortNumber;
    private int limit = 0;
    private int fingerTableSize = 0;
    private ArrayList<String> unconfirmedActiveNodes = new ArrayList<>();


    public AnchorNode(){

    }

    public void setAddressAndLimit(int limit, int fingerTableSize){
        this.anchorLocalAddress = "localhost";
        this.anchorPortNumber = 11000;
        this.limit = limit;
        this.fingerTableSize = fingerTableSize;
    }

    public void printAddressAndLimit(){
        System.out.println("Sending Address: " + anchorLocalAddress);
        System.out.println("Listening Port: " + anchorPortNumber);
        System.out.println("Limit: " + limit);
        System.out.println("Ring Size: " + (int)(Math.pow(2,
                fingerTableSize)));
    }

    public void startService(){
        try{
            ss = new ServerSocket(anchorPortNumber, 100,
                    Inet4Address.getByName(anchorLocalAddress));
            this.start();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public String addNodeToActiveList(String nodeDetails){
        String redirectionNode = "";
        if (unconfirmedActiveNodes.size() == 0){
            redirectionNode = nodeDetails;
            unconfirmedActiveNodes.add(nodeDetails);
        }
        else if(unconfirmedActiveNodes.size() < limit){
            redirectionNode = unconfirmedActiveNodes.get(0);
            unconfirmedActiveNodes.add(nodeDetails);
        }
        else {
            redirectionNode = unconfirmedActiveNodes.get(0);
        }
        return redirectionNode;
    }

    public void deleteNodeFromActiveList(String nodeDetails){
        if(unconfirmedActiveNodes.contains(nodeDetails)){
            unconfirmedActiveNodes.remove(nodeDetails);
        }
    }

    public void provideRedirectionNode(String redirectionNode,
                                      String nodeToBeAdded){
        System.out.println(redirectionNode + " will handle " + nodeToBeAdded);
        Socket clientSocket = null;

        try {
            String[] nodeInformationArray = redirectionNode.split(";");
            clientSocket = new Socket(nodeInformationArray[1],
                    Integer.parseInt(nodeInformationArray[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(3);
            out.writeObject(nodeToBeAdded);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void provideRedirectionForData(String dataToBeAdded){
        Socket clientSocket = null;

        try {
            // There needs to be atleast one node in system
            String[] nodeInformationArray =
                    unconfirmedActiveNodes.get(0).split(";");
            clientSocket = new Socket(nodeInformationArray[1],
                    Integer.parseInt(nodeInformationArray[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(11);
            out.writeObject(dataToBeAdded);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void provideRedirectionForDataRequest(String dataRequest){
        Socket clientSocket = null;

        try {
            // There needs to be atleast one node in system
            String[] nodeInformationArray =
                    unconfirmedActiveNodes.get(0).split(";");
            clientSocket = new Socket(nodeInformationArray[1],
                    Integer.parseInt(nodeInformationArray[2]));
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(18);
            out.writeObject(dataRequest);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void availableNodes() {
        if(unconfirmedActiveNodes.size() == 0){
            System.out.println("No Nodes have Connected Until Now");
        }
        else{
            for (int i=0; i<unconfirmedActiveNodes.size(); i++) {
                String detailsArray[] =
                        unconfirmedActiveNodes.get(i).split(";");
                System.out.println("NodeID: " + detailsArray[0]);
                System.out.println("Address: " + detailsArray[1]);
                System.out.println("Port: " + detailsArray[2]);
            }
        }
    }

    public void run(){
        while(true){
            try{
                Socket s = ss.accept();
                ObjectInputStream in =
                        new ObjectInputStream(s.getInputStream());
                int code = in.readInt();

                if (code == 1){
                    String nodeDetails = (String)in.readObject();
                    System.out.println("Node Added: " + nodeDetails);
                    String redirectionNode = addNodeToActiveList(nodeDetails);
                    provideRedirectionNode(redirectionNode, nodeDetails);

                }
                else if(code == 2){
                    System.out.println("Node Was Deleted");
                    String nodeDetails = (String)in.readObject();
                    deleteNodeFromActiveList(nodeDetails);
                }

                else if(code == 10){
                    System.out.println("Data Add Request Made");
                    String dataToBeAdded = (String)in.readObject();
                    if (unconfirmedActiveNodes.size() > 0){
                        provideRedirectionForData(dataToBeAdded);
                    }
                    else{
                        System.out.println("Try again when atleast one node" +
                                " is online");
                    }
                }
                else if(code == 17){
                    String nodeDetails = (String)in.readObject();
                    provideRedirectionForDataRequest(nodeDetails);
                }
            }
            catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) throws InterruptedException{
        AnchorNode anchorNodeObject1 = new AnchorNode();

        if ((args.length == 4) &&
                args[0].equals("-limit") &&
                args[2].equals("-fingertablesize")){
            System.out.println("Setting Connection Variables:");
            anchorNodeObject1.setAddressAndLimit(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]));
            anchorNodeObject1.printAddressAndLimit();
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "AnchorNode -limit 3 -fingertablesize 3");
            System.out.println("Using Default Values");
            anchorNodeObject1.setAddressAndLimit(3, 4);
            anchorNodeObject1.printAddressAndLimit();
        }


        anchorNodeObject1.startService();
        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;
        while(loopStatus){
            try{

                System.out.println("Press 1 to View Alive Nodes");
                System.out.println("Press 2 to View Address and Ring Size");
                System.out.println("Press 3 to Quit");
                System.out.println("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput){
                    case 1:
                        anchorNodeObject1.availableNodes();
                        break;
                    case 2:
                        anchorNodeObject1.printAddressAndLimit();
                        break;
                    case 3:
                        loopStatus = false;
                        break;
                    default:
                        System.out.println("Please enter a correct value.");
                        break;
                }
            } catch (Exception e){
                System.out.println("Invalid Input");
                sc.nextLine();
            }
        }
        sc.close();
        System.out.println("Anchor Node Terminated");
        System.exit(0);
    }

}

