import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InputHdlr extends Thread{
	Socket socket;
	DataInputStream is;
	
	public InputHdlr(Socket socket, DataInputStream is) {
		this.socket = socket;
		this.is = is;
	}

	public void run() {
		try {
			while(true) {
				//Input Stream으로 들어온 타 사용자로부터의 메세지를 읽어 저장
				String msg = is.readUTF();
				
				//filemsg로 시작함 -> 파일명 포맷, 파일 수신
				if(msg.startsWith("filemsg")) {
					System.out.println("Receiving " + msg + " ...");
					//수신 받은 파일 저장할 디렉토리의 경로 생성
					String filePath = "src/client" + this.socket.getLocalPort();
					Path dir = Paths.get(filePath);
					//해당 경로에 해당 이름의 디렉토리가 없다면 새로 생성
					if(!Files.exists(dir)) Files.createDirectories(dir);
					
					//받으려는 파일이 위치할 경로 생성
					File file = new File(filePath + "/" + msg);
					
					//파일 데이터 받기
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
					System.out.println("Received!");
				
				//파일 수신이 아닌 경우 그대로 수신 받은 메세지 console 출력
				}else System.out.println(msg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
