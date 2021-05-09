package datareceiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public final class DataReceiverService {

    public static class Result {

        public static final class Success extends Result {

            public final SocketAddress sourceAddress;
            public final byte[] receivedData;

            private Success(SocketAddress sourceAddress, byte[] receivedData) {
                this.sourceAddress = sourceAddress;
                this.receivedData = receivedData;
            }
        }

        public static final class Error extends Result {

            public final Exception error;

            private Error(Exception error) {
                this.error = error;
            }
        }
    }

    public static final class DataReceiverException extends Exception {

        public final Exception source;

        private DataReceiverException(Exception source, String message) {
            super(message);
            this.source = source;
        }
    }

    public static final int BUFFER_SIZE = 1024;
    private final ExecutorService executorService;
    private final int port;
    private final Object channelLock = new Object();
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private DatagramChannel channel = null;
    private Runnable dataReceiverTask;
    private Consumer<Result> dataReceiverListener;

    public DataReceiverService(ExecutorService executorService, int port) {
        this.executorService = executorService;
        this.port = port;

        dataReceiverTask = () -> {
            synchronized (channelLock) {
                buffer.clear();

                try {
                    SocketAddress sourceAddress = channel.receive(buffer);

                    buffer.flip();
                    byte[] receivedData = new byte[buffer.limit()];
                    buffer.get(receivedData);
                    if (dataReceiverListener != null) {
                        dataReceiverListener.accept(new Result.Success(sourceAddress, receivedData));
                    }

                    executorService.submit(dataReceiverTask);
                } catch (ClosedByInterruptException e) {
                    onError(e, "割り込みにより受信チャンネルがクローズしています: " + e.getMessage());
                } catch (AsynchronousCloseException e) {
                    onError(e, "別のスレッドによって受信チャンネルがクローズしています: " + e.getMessage());
                } catch (ClosedChannelException e) {
                    onError(e, "受信チャンネルがクローズしています: " + e.getMessage());
                } catch (SecurityException e) {
                    onError(e, "受信中にセキュリティ例外が発生しました: " + e.getMessage());
                } catch (IOException e) {
                    onError(e, "受信中に入出力例外が発生しました: " + e.getMessage());
                }
            }
        };
    }

    public boolean isStarted() {
        return channel != null;
    }

    public void startService(Consumer<Result> dataReceiverListener) {
        if (isStarted()) {
            System.out.println("すでに受信サービスは開始しています。");
            return;
        }

        synchronized (channelLock) {
            this.dataReceiverListener = dataReceiverListener;

            try {
                channel = DatagramChannel.open();
                channel.socket().bind(new InetSocketAddress(port));

                executorService.submit(dataReceiverTask);
                System.out.println("次のポートで受信サービスを開始しました: " + port);
            } catch (SocketException e) {
                onError(e, "受信ソケットをバインドできませんでした: " + e.getMessage());
            } catch (SecurityException e) {
                onError(e, "受信チャンネルオープン中にセキュリティ例外が発生しました: " + e.getMessage());
            } catch (IOException e) {
                onError(e, "受信チャンネルをオープンできませんでした: " + e.getMessage());
            }
        }
    }

    public void endServiceImmediately() {
        if (!isStarted()) {
            System.out.println("すでに受信サービスは終了しています。");
            return;
        }

        // channelの状態によらず強制クローズする
        closeChannel();
    }

    private void onError(Exception error, String message) {
        if (isStarted()) {
            closeChannel();
        }

        if (dataReceiverListener != null) {
            DataReceiverException dataReceiverException = new DataReceiverException(error, message);
            dataReceiverListener.accept(new Result.Error(dataReceiverException));
        }
    }

    private void closeChannel() {
        try {
            channel.close();
            System.out.println("受信チャンネルをクローズしました。");
        } catch (IOException e) {
            System.out.println("受信チャンネルのクローズに失敗しました: " + e.getMessage());
        } finally {
            channel = null;
        }
        dataReceiverListener = null;
    }
}
