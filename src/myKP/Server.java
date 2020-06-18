package myKP;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Properties;

public class Server {

    public static int port;
    public static LinkedList<ServerThread> serverList = new LinkedList<>(); // список всех потоков сервера - ридеров
    public static Story story; // история переписки

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("src/myKP/myConfig.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        port = Integer.parseInt(properties.getProperty("port"));

        ServerSocket server = new ServerSocket(port);
        story = new Story();

        System.out.println("Server Started");
        try {
            while (true) {
                // Блокируется до возникновения нового соединения:
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerThread(socket)); // добавить новое соединенние в список
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}

class ServerThread extends Thread {
    private Socket socket; // сокет, через который сервер общается с клиентом,
    private BufferedReader in; // поток чтения из сокета
    private BufferedWriter out; // поток завписи в сокет

    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        Server.story.printStory(out); // поток вывода передаётся для передачи истории последних 10
        // сообщений новому поключению
        start(); // вызываем метод run()
    }
    @Override
    public void run() {
        String message;
        try {
            // первое сообщение отправленное сюда - это никнейм
            message = in.readLine();
            try {
                out.write(message + "\n");
                out.flush(); // flush() нужен для выталкивания оставшихся данных
                // если такие есть, и очистки потока для дальнейших нужд
            } catch (IOException ignored) {}
            try {
                while (true) {
                    message = in.readLine();
                    if(message.equals("stop")) {
                        this.downService(); // закрытие
                        break; // если пришла пустая строка - выходим из цикла прослушки
                    }
                    System.out.println("Echoing: " + message);
                    Server.story.addStoryEl(message);
                    for (ServerThread vr : Server.serverList) {
                        if (!vr.equals(this)) {
                            vr.send(message); // отослать принятое сообщение с привязанного клиента всем остальным
                        }
                    }
                }
            } catch (NullPointerException ignored) {}


        } catch (IOException e) {
            this.downService();
        }
    }

    private void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {}

    }

    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ServerThread vr : Server.serverList) {
                    if(vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {}
    }
}

class Story {

    private LinkedList<String> story = new LinkedList<>();

    public void addStoryEl(String el) {
        // если сообщений больше 10, удаляем первое и добавляем новое
        // иначе просто добавить
        if (story.size() >= 10) {
            story.removeFirst();
            story.add(el);
        } else {
            story.add(el);
        }
    }

    public void printStory(BufferedWriter writer) {
        if(story.size() > 0) {
            try {
                writer.write("History messages" + "\n");
                for (String vr : story) {
                    writer.write(vr + "\n");
                }
                writer.write("/...." + "\n");
                writer.flush();
            } catch (IOException ignored) {}

        }

    }
}
