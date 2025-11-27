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
	Vector<DataOutputStream> allOs;
	DataInputStream is;
	DataOutputStream os;
	
	public ClientHdlrThread(Socket client, Vector<DataOutputStream> allOs) {
		this.client = client;
		this.allOs = allOs;
	}

	public void run() {
		try {
			is = new DataInputStream(new BufferedInputStream(client.getInputStream()));
			os = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
			
			allOs.add(os);
			
			System.out.println("Client" + client.getPort() + " is listening");
			
			while(true) {
				String msg;
				if((msg = is.readUTF()) != null && allOs.size() > 1) {
					synchronized(allOs){
						for(DataOutputStream os : allOs) {
							if(os != this.os) {
								os.writeUTF("Client" + client.getPort() + " : " + msg);
								os.flush();
							}
						}
					}
					
				}
				else if(allOs.size() == 1) {
					System.out.println("Waiting for other clients ...");
				}
			}			
		} catch (EOFException e) {
			System.out.println("Client" + client.getPort() + " disconnect");
		} catch (IOException e) {
			System.out.println("Client" + client.getPort() + " connection lost");
		} finally {
			allOs.remove(this.os);
			try {
				if(is != null) is.close();
				if(os != null) os.close();
				if(client != null) client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

public class TcpChatServer {
	public static void main(String[] args) {
		
		final Vector<DataOutputStream> osList = new Vector<>();
		ServerSocket serverSocket = null;
		
		try {
			System.out.println("<<Server>>");
			serverSocket = new ServerSocket(10000);
			
			while(true) {
				System.out.println("clients : " + Thread.activeCount() + " / " + "AllOutputStream : " + osList.size());
				System.out.println("Waiting for client ...");
				Socket socket = serverSocket.accept();
				System.out.println("Client joined! : " + socket.getInetAddress() + ":" + socket.getPort());
				Thread client = new ClientHdlrThread(socket, osList);
				client.start();
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


