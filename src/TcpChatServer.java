import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

class ClientHdlrThread extends Thread{
	Socket client;
	Vector<Socket> allClients;
	
	public ClientHdlrThread(Socket client, Vector<Socket> allClients) {
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
					synchronized(allClients){
						for(Socket socket : allClients) {
							if(socket != this.client) {
								DataOutputStream os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
								os.writeUTF("Client" + client.getPort() + " : " + msg);
								os.flush();
							}
						}
					}
					
				}
				else if(allClients.size() == 1) {
					System.out.println("Waiting for other clients ...");
				}
			}			
		} catch (EOFException e) {
			System.out.println("Client" + client.getPort() + " disconnect");
		} catch (IOException e) {
			System.out.println("Client" + client.getPort() + " connection lost");
		} finally {
			allClients.remove(client);
			System.out.println("removed");
		}
	}
}

public class TcpChatServer {
	public static void main(String[] args) {
		
		final Vector<Socket> clientList = new Vector<>();
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


