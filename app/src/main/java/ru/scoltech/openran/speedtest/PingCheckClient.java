package ru.scoltech.openran.speedtest;

import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PingCheckClient {
    volatile boolean isDone = true;

    public int doPing(InetAddress address, int port, int timeout) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            long timeOfPing = System.nanoTime();
            byte[] requestValue = ("Ping at " + timeOfPing).getBytes(StandardCharsets.UTF_8);
            DatagramPacket request = new DatagramPacket(requestValue, requestValue.length);
            request.setAddress(address);
            request.setPort(port);
            long start = System.nanoTime();
            clientSocket.send(request);
            isDone = false;
            clientSocket.setSoTimeout(timeout);
            DatagramPacket inputDatagramPacket = new DatagramPacket(new byte[1024], 1024);
            clientSocket.receive(inputDatagramPacket);
            isDone = true;
            long end = System.nanoTime();
            String result = new String(Arrays.copyOf(inputDatagramPacket.getData(), inputDatagramPacket.getLength()), StandardCharsets.UTF_8);
            if (result.equals("Ping at " + timeOfPing)) {
                return (int) (end - start);
            } else
                return -1;
        } catch (IOException e) {
            isDone = true;
            return -1;
        }
    }
}
