package datasender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DataSenderService {

    public static final class DataSenderException extends Exception {

        public final Exception source;

        private DataSenderException(Exception source, String message) {
            super(message);
            this.source = source;
        }
    }

    public static final int BUFFER_SIZE = 1024;
    private final ExecutorService executorService;
    private final int servicePort;
    private final SocketAddress destinationAddress;
    private final Object channelLock = new Object();
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private DatagramChannel channel = null;

    public DataSenderService(ExecutorService executorService, int servicePort, SocketAddress destinationAddress) {
        this.executorService = executorService;
        this.servicePort = servicePort;
        this.destinationAddress = destinationAddress;
    }

    public void startService() throws DataSenderException {
        if (isStarted()) {
            System.out.println("すでに送信サービスは開始しています。");
            return;
        }

        synchronized (channelLock) {
            try {
                channel = DatagramChannel.open();
                channel.socket().bind(new InetSocketAddress(servicePort));

                System.out.println();
                System.out.println("次のポートで送信サービスを開始しました: " + servicePort);
            } catch (SocketException e) {
                throw new DataSenderException(e, "送信ソケットをバインドできませんでした: " + e.getMessage());
            } catch (SecurityException e) {
                throw new DataSenderException(e, "送信チャンネルオープン中にセキュリティ例外が発生しました: " + e.getMessage());
            } catch (IOException e) {
                throw new DataSenderException(e, "送信チャンネルをオープンできませんでした: " + e.getMessage());
            }
        }
    }

    public boolean isStarted() {
        return channel != null;
    }

    public void sendMessage(String message, Consumer<Exception> completionListener) {
        if (!isStarted()) {
            completionListener.accept(new IllegalStateException("送信サービスが開始していません。sendMessageはstartService呼び出し後に呼び出してください。"));
            return;
        }

        BiConsumer<Exception, String> onError = (error, errorMessage) -> {
            closeChannel();

            if (completionListener != null) {
                DataSenderException dataSenderException = new DataSenderException(error, errorMessage);
                completionListener.accept(dataSenderException);
            }
        };

        executorService.submit(() -> {
            synchronized (channelLock) {
                try {
                    buffer.clear();
                    buffer.put(message.getBytes(StandardCharsets.UTF_8));
                    buffer.flip();

                    channel.send(buffer, destinationAddress);
                } catch (ClosedByInterruptException e) {
                    onError.accept(e, "割り込みにより送信チャンネルがクローズしています: " + e.getMessage());
                } catch (AsynchronousCloseException e) {
                    onError.accept(e, "別のスレッドによって送信チャンネルがクローズしています: " + e.getMessage());
                } catch (ClosedChannelException e) {
                    onError.accept(e, "送信チャンネルがクローズしています: " + e.getMessage());
                } catch (SecurityException e) {
                    onError.accept(e, "送信中にセキュリティ例外が発生しました: " + e.getMessage());
                } catch (IOException e) {
                    onError.accept(e, "送信中に入出力例外が発生しました: " + e.getMessage());
                }
            }
        });
    }

    public void endServiceImmediately() {
        if (!isStarted()) {
            System.out.println("すでに送信サービスは終了しています。");
            return;
        }

        // channelの状態によらず強制クローズする
        closeChannel();
    }

    private void closeChannel() {
        try {
            channel.close();
            System.out.println("送信チャンネルをクローズしました。");
        } catch (IOException e) {
            System.out.println("送信チャンネルのクローズに失敗しました: " + e.getMessage());
        } finally {
            channel = null;
        }
    }
}
