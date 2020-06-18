package myKP;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class Client {
    public static String ip;
    public static int port;

    public static void main(String[] args) {

        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("src/myKP/myConfig.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        port = Integer.parseInt(properties.getProperty("port"));
        ip = properties.getProperty("ip");

        new ClientSocket(ip, port);
    }
}

class ClientSocket {
    private Socket socket;
    private BufferedReader in; // чтение из сокета
    private BufferedWriter out; // запись в сокет
    private BufferedReader inputUser; // чтение с консоли
    private String ip; // ip адрес клиента
    private int port; // порт соединения
    private String nickname; // имя клиента

    public ClientSocket(String ip, int port) {
        this.ip = ip;
        this.port = port;
        try {
            this.socket = new Socket(ip, port);
        } catch (IOException e) {
            System.err.println("Socket failed");
        }
        try {
            // потоки чтения из сокета / записи в сокет, и чтения с консоли
            inputUser = new BufferedReader(new InputStreamReader(System.in));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.pressNickname(); // перед началом запрос имени
            new ClientReader().start(); // поток читает сообщения из сокета в бесконечном цикле
            new ClientSender().start(); // поток пишет сообщения в сокет приходящие с консоли в бесконечном цикле
        } catch (IOException e) {
            ClientSocket.this.downService(); // закрытие сокета
        }
    }

    private void pressNickname() {
        System.out.print("Ваше имя: ");
        try {
            nickname = inputUser.readLine();
            out.write("Привет " + nickname + "\n");
            out.flush();
        } catch (IOException ignored) {
        }

    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {}
    }

    private class ClientReader extends Thread {  // поток чтения сообщений с сервера

        @Override
        public void run() {    // переопределяем метод run
            String str;
            try {
                while (true) {
                    str = in.readLine(); // ждем сообщения с сервера
                    if (str.equals("stop")) {
                        ClientSocket.this.downService(); // закрытие
                        break; // выходим из цикла если пришло "stop"
                    }
                    System.out.println(str); // пишем сообщение с сервера на консоль
                }
            } catch (IOException e) {
                ClientSocket.this.downService();
            }
        }
    }

    public class ClientSender extends Thread {  // поток отправляет сообщения приходящие с консоли на сервер

        @Override
        public void run() {
            while (true) {
                String clientMessage;
                try {
                    LocalDateTime current = LocalDateTime.now();
                    clientMessage = inputUser.readLine(); // сообщения с консоли
                    if (clientMessage.equals("stop")) {
                        out.write("stop" + "\n");
                        ClientSocket.this.downService(); // закрытие
                        break; // выходим из цикла если пришло "stop"
                    } else {
                        out.write("(" + current + ") " + nickname + ": " + clientMessage + "\n"); // отправляем на сервер
                    }
                    out.flush(); // чистим
                } catch (IOException e) {
                    ClientSocket.this.downService(); // закрытие

                }

            }
        }
    }
}
