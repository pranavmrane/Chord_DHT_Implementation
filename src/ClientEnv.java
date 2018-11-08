import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Scanner;

public class ClientEnv extends Thread {

    private ServerSocket ss = null;

    private String anchorAddress = "0.0.0.0";
    private int anchorPort = 11000;

    private String clientLocalAddress = "";
    private int clientPort = 0;
    int ringSize = 0;
    int fingerTableSize = 0;

    public void setAddress(int clientPort,
                           int fingerTableSize, String anchorAddress){
        try {
            this.clientLocalAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e){
            e.printStackTrace();
        }
//        this.clientLocalAddress = address;
        this.clientPort = clientPort;
        this.fingerTableSize = fingerTableSize;
        this.anchorAddress = anchorAddress;
        this.ringSize = (int)Math.pow(2, fingerTableSize);
    }

    public void printAddresses(){
        System.out.println("Anchor Address: " + anchorAddress);
        System.out.println("Anchor Port: " + anchorPort);
        System.out.println("Listening Address: " + clientLocalAddress);
        System.out.println("Listening Port: " + clientPort);
        System.out.println("Ring Size: " + (int)(Math.pow(2,
                fingerTableSize)));
    }

    public void startService(){
        System.out.println("Starting Client Service");
        try{
            ss = new ServerSocket(clientPort, 100,
                    Inet4Address.getByName(clientLocalAddress));
            this.start();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void run(){
        while(true){
            try{
                Socket s = ss.accept();
                ObjectInputStream in =
                        new ObjectInputStream(s.getInputStream());
                int code = in.readInt();
                if (code == 19){
                    String location = (String)in.readObject();
                    System.out.println("Data was present at: " + location);
                }
            }
            catch (ClassNotFoundException | IOException e){
                e.printStackTrace();
            }
//            catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void deliverNumberToAnchor(int dataTobeBeAdded){

        dataTobeBeAdded = dataTobeBeAdded % ringSize;
        String stringdataToBeAdded = "" + dataTobeBeAdded;

        Socket clientSocket = null;

        try {

            clientSocket = new Socket(anchorAddress,anchorPort);
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(10);
            out.writeObject(stringdataToBeAdded);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void requestData(int dataRequest){
        dataRequest = dataRequest % ringSize;
        String dataRequestString = dataRequest + ";" + clientLocalAddress + ";" +
                clientPort;
        Socket clientSocket = null;

        try {
            clientSocket = new Socket(anchorAddress,anchorPort);
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            out.writeInt(17);
            out.writeObject(dataRequestString);
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String args[]){

        ClientEnv clientObject = new ClientEnv();

        if ((args.length == 6) &&
                args[0].equals("-port") &&
                args[2].equals("-fingertablesize") &&
                args[4].equals("-anchoraddress")){
            System.out.println("Setting Connection Variables:");
            clientObject.setAddress(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]), args[5]);
            clientObject.printAddresses();
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "java ClientEnv -port 5020 -fingertablesize 4 -anchoraddress 0.0.0.0");
            System.out.println("Using Default Values");
            clientObject.setAddress(5020, 4, "0.0.0.0");
            clientObject.printAddresses();
        }
        clientObject.startService();

        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;
        while(loopStatus){
            try{

                System.out.println("Press 1 to Save Data");
                System.out.println("Press 2 to Request Data");
                System.out.println("Press 3 to Quit");
                System.out.println("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput){
                    case 1:
                        sc.nextLine();
                        System.out.println("Enter Data To Be Saved: ");
                        int dataToBeSaved = sc.nextInt();
                        clientObject.deliverNumberToAnchor(dataToBeSaved);
                        break;
                    case 2:
                        sc.nextLine();
                        System.out.println("Enter Data To Be Retrieved: ");
                        int dataRequest = sc.nextInt();
                        clientObject.requestData(dataRequest);
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
        System.out.println("Client Terminated");
        System.exit(0);
    }
}
