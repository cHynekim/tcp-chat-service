import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class ClientHdlrThread extends Thread{
	Socket client;
	LinkedList<Socket> allClients;
	
	public ClientHdlrThread(Socket client, LinkedList<Socket> allClients) {
		this.client = client;
		this.allClients = allClients;
	}

	public void run() {
		try {
			DataInputStream is = new DataInputStream(new BufferedInputStream(client.getInputStream()));
			System.out.println("Client" + client.getPort() + " is listening");
			while(true) {
				String msg;
				if((msg = is.readUTF()) != null && allClients.size() > 1) {
					System.out.println("triggered");
					for(Socket socket : allClients) {
						System.out.println("for");
						if(socket != this.client) {
							DataOutputStream os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
							os.writeUTF("Clinet" + socket.getPort() + " : " + msg);
							os.flush();
							System.out.println("flushed");
						}
					}
				}
				else if(allClients.size() == 1) {
					System.out.println("Waiting for other clients ...");
				}
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

public class TcpChatServer {
	public static void main(String[] args) {
		
		final LinkedList<Socket> clientList = new LinkedList<>();
		ServerSocket serverSocket = null;
		
		try {
			System.out.println("<<Server>>");
			serverSocket = new ServerSocket(10000);
			
			while(true) {
				System.out.println("Waiting for client ...");
				Socket socket = serverSocket.accept();
				System.out.println("Client joined! : " + socket.getInetAddress() + ":" + socket.getPort());
				clientList.add(socket);
				Thread client = new ClientHdlrThread(socket, clientList);
				client.start();
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


