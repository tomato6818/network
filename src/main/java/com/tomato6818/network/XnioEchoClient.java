package com.tomato6818.network;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class XnioEchoClient {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("localhost", 9999);
             OutputStream out = socket.getOutputStream();
             Scanner in = new Scanner(socket.getInputStream())) {

            out.write("Hello XNIO!\n".getBytes());
            out.flush();

            while (in.hasNextLine()) {
                System.out.println("Received: " + in.nextLine());
            }
        }
    }
}