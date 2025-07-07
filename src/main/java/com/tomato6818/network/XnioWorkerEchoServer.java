package com.tomato6818.network;

import org.xnio.*;
import org.xnio.channels.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class XnioWorkerEchoServer {
    public static void main(String[] args) throws IOException {
        // 1. XNIO 인스턴스 획득
        Xnio xnio = Xnio.getInstance("nio", XnioWorkerEchoServer.class.getClassLoader());

        // 2. OptionMap을 이용해 Worker 설정 (스레드 수, KeepAlive 등)
        OptionMap workerOptions = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 2)               // IO 스레드 수
                .set(Options.WORKER_TASK_CORE_THREADS, 2)        // 기본 task pool 사이즈
                .set(Options.WORKER_TASK_MAX_THREADS, 4)         // 최대 task pool 사이즈
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.READ_TIMEOUT, 30000)
                .set(Options.WRITE_TIMEOUT, 30000)
                .getMap();

        // 3. Worker 생성
        XnioWorker worker = xnio.createWorker(workerOptions);

        // 4. 서버 소켓 옵션 설정
        OptionMap serverOptions = OptionMap.builder()
                .set(Options.BACKLOG, 100)
                .getMap();

        // 5. 서버 소켓 생성
        AcceptingChannel<StreamConnection> server = worker.createStreamConnectionServer(
                new InetSocketAddress("0.0.0.0", 9999),
                new ChannelListener<AcceptingChannel<StreamConnection>>() {
                    public void handleEvent(AcceptingChannel<StreamConnection> channel) {
                        try {
                            StreamConnection conn = channel.accept();
                            if (conn != null) {
                                handleClientConnection(conn);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                },
                serverOptions
        );

        // 6. 클라이언트 연결 수락 시작
        server.resumeAccepts();

        System.out.println("XNIO Echo Server running on port 9999");
    }

    private static void handleClientConnection(StreamConnection conn) throws IOException {
        ConnectedStreamChannel channel = new AssembledConnectedStreamChannel(
                conn, conn.getSourceChannel(), conn.getSinkChannel());

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 읽기 이벤트 핸들러
        channel.getReadSetter().set(source -> {
            try {
                int bytesRead = source.read(buffer);
                System.out.println("📥 읽은 바이트 수: " + bytesRead);
                if (bytesRead == -1) {
                    channel.close();
                    return;
                }

                buffer.flip(); 
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                String msg = new String(data, StandardCharsets.UTF_8);
                System.out.println("📥 받은 데이터: " + msg);

                // 다시 클라이언트에게 전송
                buffer.rewind(); // rewind 해서 다시 보내기

                conn.getSinkChannel().write(buffer);
                buffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    channel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // 읽기 시작
        channel.resumeReads();
    }

}
