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

/*
 * main thread : 사용자들의 접속 요청 수락, 해당 사용자를 관리하는 스레드 실행
 * 
 * client hdlr thread : 각 담당하는 사용자로부터의 데이터를 타 사용자들에게 전송하며
 * 퇴장 혹은 비정상적 종료의 경우 해당 클라이언트가 사용하던 자원을 모두 반납하여 메모리 누수를 막음.
 * 
 * 멀티스레드로 만든 이유는 기존 접속자1의 입력 내용을 기존 다른 접속자들에게 전송할 때,
 * 다른 기존 접속자2도 입력 내용을 다른 접속자들에게 전송 가능하며
 * 새로운 접속자도 접속이 가능하게 동시에 일어났으면 하여 멀티스레드로 서버를 구현함.
 * 
 * */

class ClientHdlrThread extends Thread{
	Socket client;
	LinkedList<DataOutputStream> allOs;
	//LinkedList<Socket> allClients;
	DataInputStream is;
	DataOutputStream os;
	
	public ClientHdlrThread(Socket client, LinkedList<DataOutputStream> allOs) {
		this.client = client;
		this.allOs = allOs;
		//this.allClients = allClients;
	}

	public void run() {
		try {
			//해당 스레드가 관리하는 사용자와 연결된 Input Stream과 Output Stream을 불러옴
			is = new DataInputStream(new BufferedInputStream(client.getInputStream()));
			os = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
			
			//사용자들의 Output Stream을 모아둔 객체에 추가,
			//추가가 일어날 때 connection lost 또는 for-each문이 동시에 실행되면 안되므로 allOs를 key로 가지는 동기화 블록으로 감쌈
			synchronized(allOs) {
				allOs.add(os);
			}
			
			System.out.println("Client" + client.getPort() + " is listening");
			
			while(true) {
				String msg;
				//사용자로부터의 메세지가 null이 아니고 filemsg로 시작하지 않고, (즉, 사용자 A가 일반 메세지를 보냄)
				//해당 메세지를 나 외에 다른 사용자들에게 보낼 수 있는 경우, (서버와 연결된 Output Stream의 갯수 1개 초과)
				//나 외의 다른 사용자들과 연결된 각 Output Stream으로 메세지 전송
				if((msg = is.readUTF()) != null && !(msg.startsWith("filemsg")) && allOs.size() > 1) {
					//for-each로 각 Output Stream을 통해 보내는 도중 allOs의 Output Stream 정보가 변경되면 안되므로
					//allOs를 key로 가지는 동기화 블록으로 for-each문을 감싸줌
					synchronized(allOs){
						for(DataOutputStream os : allOs) {
							if(os != this.os) {
								os.writeUTF(msg);
								os.flush();
							}
						}
					}
				}
				//사용자로부터의 메세지의 시작이 filemsg일 경우, 파일명 포맷이므로 파일 데이터를 우선 받은 후에 타 사용자들에게 송신함
				else if(msg.startsWith("filemsg") && allOs.size() > 1) {
					//System.out.println("filemsg detected");
					//파일 수신(클라 -> 서버)
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
					
					//파일 송신(서버 -> 타 클라)
					//for-each 도중 allOs가 변경되는 것을 막기 위해 동기화 블록으로 감쌈
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
				//접속자가 나 혼자일 경우,
				else if(allOs.size() == 1) {
					System.out.println("Waiting for other clients ...");
				}
			}
		
		} catch (EOFException e) {
			//사용자로부터 온 메세지가 exit일 경우, EOF예외가 발생함
			//이 경우 접속이 끊겼다는 콘솔을 서버에 출력.
			//클라이언트 측 : 퇴장했다는 문구를 flush하여 타 사용자들에게 알리고, 자원 반납(close)
			//서버 측 : 연결 끊겼다는 콘솔 출력 후 finally 실행
			System.out.println("Client" + client.getPort() + " disconnect");
			
		} catch (IOException e) {
			//IO예외의 경우, 강제 종료 시 발생 (exit 입력이 아닌 pause버튼을 누름)
			//이 경우 클라이언트는 이미 종료된 상태이니 서버 측에서 강제 종료한 사용자의 연결이 끊겼다고 flush하여 타 사용자들에게 알려줌
			//(마찬가지로 충돌을 막기 위해 동기화 블록으로 감쌈)
			//후에 서버 측 콘솔에 connection lost 출력 후 finally 실행
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
			//충돌을 막기위해 모든 스레드에서 접근하는 allOs를 키로하는 동기화 블록으로 감쌈
			//exit 또는 강제 종료한 클라이언트와 관련한 정보 삭제
			synchronized(allOs) {
				allOs.remove(this.os);
			}
			try {
				//만약 반납되지 않았다면 반납
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
		//서버에 접속한 각 사용자들의 Output Stream을 저장하는 Linked List
		//값 변경보다는 값의 삭제/추가가 빈번히 일어나기에 Linked List로 만듬
		//해당 객체를 clientHdlrThread의 생성자로 넘겨 각 사용자들에게 메세지를 전송하고 자원 관리를 가능하게 함
		final LinkedList<DataOutputStream> osList = new LinkedList<>();
		//서버 소켓과 연결된 클라이언트 소켓들(= 클라이언트들의 정보)을 담아둠
		//final LinkedList<Socket> clientList = new LinkedList<>();
		//서버 소켓
		ServerSocket serverSocket = null;
		
		try {
			System.out.println("<<Server>>");
			//서버 소켓 할당, 10000번 포트 사용
			serverSocket = new ServerSocket(10000);
			
			while(true) {
				System.out.println("clients : " + Thread.activeCount() + " / " + "AllOutputStream : " + osList.size());
				System.out.println("Waiting for client ...");
				//연결 요청하는 소켓 수락
				Socket socket = serverSocket.accept();
				//클라이언트 리스트에 추가
				//clientList.add(socket);
				System.out.println("Client joined! : " + socket.getInetAddress() + ":" + socket.getPort());
				//해당 클라이언트를 관리하는 스레드 실행
				Thread client = new ClientHdlrThread(socket, osList);
				//Thread client = new ClientHdlrThread(socket, osList, clientList);
				client.start();
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


