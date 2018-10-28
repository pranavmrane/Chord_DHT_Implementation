import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ClientEnv extends Thread {

    private ServerSocket ss = null;

    private String anchorAddress = "0.0.0.0";
    private int anchorPort = 11000;

    private String clientLocalAddress = "";
    private int clientPort = 0;
    int ringSize = 0;
    int fingerTableSize = 0;

    public void setAddress(String address, int clientPort,  int fingerTableSize){
        this.clientLocalAddress = address;
        this.clientPort = clientPort;
        this.fingerTableSize = fingerTableSize;
        this.ringSize = (int)Math.pow(2, fingerTableSize);
    }

    public void printAddresses(){
        System.out.println("Client Address: " + clientLocalAddress);
        System.out.println("Listening Port: " + clientPort);
        System.out.println("Ring Size: " + (int)(Math.pow(2, fingerTableSize)));
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
                System.out.println("Code: " + code);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void deliverNumberToAnchor(int dataTobeBeAdded){

        dataTobeBeAdded = dataTobeBeAdded % ringSize;
        String stringdataToBeAdded = "" + dataTobeBeAdded;

        System.out.println("deliverNumberToAnchor");
        Socket clientSocket = null;

        try {

            clientSocket = new Socket("localhost",anchorPort);
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


    public static void main(String args[]){

        ClientEnv clientObject = new ClientEnv();
//
//        for(String value: args){
//            System.out.println(value);
//        }

        if ((args.length == 6) && args[0].equals("-address") &&
                args[2].equals("-port") &&
                args[4].equals("-fingertablesize")){
            System.out.println("Setting Connection Variables:");
            clientObject.setAddress(args[1], Integer.parseInt(args[3]),
                    Integer.parseInt(args[5]));
            clientObject.printAddresses();
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "ClientEnv -address localhost -port 5020 " +
                    "-fingertablesize 3");
            System.out.println("Using Default Values");
            clientObject.setAddress("localhost", 5020,
                    3);
            clientObject.printAddresses();
        }
        clientObject.startService();

        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;
        while(loopStatus){
            try{

                System.out.println("Press 1 to Save Data");
                System.out.println("Press 2 to Quit");
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
