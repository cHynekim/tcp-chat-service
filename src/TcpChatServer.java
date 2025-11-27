import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class ClientHdlrThread extends Thread{
	Socket client;
	LinkedList<DataOutputStream> allOs;
	LinkedList<Socket> allClients;
	DataInputStream is;
	DataOutputStream os;
	
	public ClientHdlrThread(Socket client, LinkedList<DataOutputStream> allOs, LinkedList<Socket> allClients) {
		this.client = client;
		this.allOs = allOs;
		this.allClients = allClients;
	}

	public void run() {
		try {
			is = new DataInputStream(new BufferedInputStream(client.getInputStream()));
			os = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
			
			synchronized(allOs) {
				allOs.add(os);
			}
			
			System.out.println("Client" + client.getPort() + " is listening");
			
			while(true) {
				String msg;
				if((msg = is.readUTF()) != null && !(msg.startsWith("filemsg")) && allOs.size() > 1) {
					synchronized(allOs){
						for(DataOutputStream os : allOs) {
							if(os != this.os) {
								os.writeUTF(msg);
								os.flush();
							}
						}
					}
				}
				else if(msg.startsWith("filemsg") && allOs.size() > 1) {
					System.out.println("filemsg detected");
					File file = new File("src/server/" + msg);
					byte[] data = new byte[2048];
					int len;
					try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file))){
						while((len = is.readInt()) != -1) {
							is.read(data, 0, len);
							fos.write(data, 0, len);
							fos.flush();
						}
						fos.flush();
					}catch (EOFException e) { e.printStackTrace(); }
					
					synchronized(allOs) {
						for(DataOutputStream os : allOs) {
							if(os != this.os) {
								os.writeUTF(msg);
								try(BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))){
									while((len = fis.read(data)) != -1) {
										os.writeInt(len);
										os.write(data, 0, len);
										os.flush();
									}
									os.writeInt(-1);
									os.flush();
								}catch (EOFException e) { e.printStackTrace(); }
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
			synchronized(allOs){
				for(DataOutputStream os : allOs) {
					if(os != this.os) {
						try {
							os.writeUTF("<<Client" + client.getPort() + " disconnected>>");
							os.flush();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
			System.out.println("Client" + client.getPort() + " connection lost");
		} finally {
			synchronized(allOs) {
				allOs.remove(this.os);
			}
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
		
		final LinkedList<DataOutputStream> osList = new LinkedList<>();
		final LinkedList<Socket> clientList = new LinkedList<>();
		ServerSocket serverSocket = null;
		
		try {
			System.out.println("<<Server>>");
			serverSocket = new ServerSocket(10000);
			
			while(true) {
				System.out.println("clients : " + Thread.activeCount() + " / " + "AllOutputStream : " + osList.size());
				System.out.println("Waiting for client ...");
				Socket socket = serverSocket.accept();
				clientList.add(socket);
				System.out.println("Client joined! : " + socket.getInetAddress() + ":" + socket.getPort());
				Thread client = new ClientHdlrThread(socket, osList, clientList);
				client.start();
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


