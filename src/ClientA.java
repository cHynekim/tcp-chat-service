import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientA {
	public static void main(String[] args) {
		System.out.println("<<Client A>>");
		try {
			//Socket 설정, 서버와 연결되어 있음
			Socket socket = new Socket(InetAddress.getByName("localhost"), 10000);
			System.out.println("Connected! : " + socket.getInetAddress() + ":" + socket.getPort());
			
			//Data I/O Stream 설정, 서버와 연결되어 있음
			DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			//InputStream으로 들어오는 데이터 핸들러 역할을 하는 스레드의 객체 생성,
			//main thread가 죽으면 같이 죽어야 하므로 daemon으로 설정.
			//멀티스레딩 사용하여 입력과 출력을 나눔. 하여 입력 도중 또는 전송 시에도 나에게 보내는 메세지 수신이 가능하게 함.
			//수신 메세지 받는 스레드도 동일한 자원을 공유할 수 있게 생성자로 socket과 Input Stream 을 넘김
			Thread inputHdlr = new InputHdlr(socket, is);
			inputHdlr.setDaemon(true);
			inputHdlr.start();
			
			Scanner scanner = new Scanner(System.in);
			
			//위 scanner 객체에 저장된 console 입력을 Enter 기준으로 다른 사용자들에게 전송함
			while(true) {
				String input = scanner.nextLine();
				//파일 전송
				if(input.equalsIgnoreCase("file")) {
					//보내려는 파일 정보 입력
					System.out.println("File name? >> ");
					String fileName = scanner.nextLine();
					
					System.out.println("File extension? (remove .)");
					System.out.println("(e.g. aaa.jpg -> type jpg) >> ");
					String fileExt = scanner.nextLine();
					
					//보내려는 파일이 위치한 경로 생성
					File file = new File("src/client_a/" + fileName + "." + fileExt);
					try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))){
						//파일명 전송
						os.writeUTF("filemsg" + socket.getLocalPort() + "." + fileExt);
						os.flush();
						
						//파일 데이터 전송
						byte[] data = new byte[2048];
						int len;
						while((len = fis.read(data)) != -1) {
							os.writeInt(len);
							os.write(data, 0, len);
							os.flush();
						}
						os.writeInt(-1);
						os.flush();
						
					}catch (FileNotFoundException e) {
						System.out.println("File Not Found : " + e);
					}
					
				//사용자가 exit 입력 시, 채팅방 퇴장
				} else if(input.equalsIgnoreCase("exit")) {
					//해당 사용자가 채팅방을 나갔다는 것을 다른 사용자들에게 알리기 위해 퇴장 메세지 전송
					os.writeUTF("<<Client" + socket.getLocalPort() + " left this chat>>");
					os.flush();
					
					//해당 사용자의 자원 반납하여 메모리 누수 방지
					scanner.close();
					is.close();
					os.close();
					socket.close();
					break;
				
				//그 외 메세지들은 해당 사용자의 정보를 담아 다른 사용자들에게 전송
				} else {
					os.writeUTF("Client" + socket.getLocalPort() + " : " + input);
					os.flush();
				}
				
			}
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
