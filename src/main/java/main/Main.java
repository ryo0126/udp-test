package main;

import configuration.Configuration;
import datareceiver.DataReceiverService;
import datasender.DataSenderService;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static datareceiver.DataReceiverService.*;

public final class Main {

    private interface InputType {

        final class SendMessage implements InputType {

            final String message;

            SendMessage(String message) {
                this.message = message;
            }
        }

        final class Command implements InputType {

            enum Type {
                QUIT("quit"),
                START_SENDER_SERVICE("start-sender"),
                STOP_SENDER_SERVICE("stop-sender"),
                UNKNOWN("");

                String rawString;

                Type(String rawString) {
                    this.rawString = rawString;
                }
            }

            static String START_SYMBOL = "/";
            final Type type;

            Command(Type type) {
                this.type = type;
            }
        }

        enum None implements InputType {
            INSTANCE;
        }
    }

    public static void main(String[] args) {
        System.out.println("--------------------コンフィグ--------------------");
        System.out.println("receiverPort    = " + Configuration.RECEIVER_PORT);
        System.out.println("senderPort      = " + Configuration.SENDER_PORT);
        System.out.println("destinationIp   = " + Configuration.DESTINATION_IP);
        System.out.println("destinationPort = " + Configuration.DESTINATION_PORT);
        System.out.println("--------------------------------------------------");

        ExecutorService receiverExecutorService = Executors.newSingleThreadExecutor();
        DataReceiverService receiverService = new DataReceiverService(receiverExecutorService, Configuration.RECEIVER_PORT);
        receiverService.startService(result -> {
            if (result instanceof Result.Success) {
                Result.Success success = (Result.Success) result;

                String decoded = new String(success.receivedData, StandardCharsets.UTF_8);
                System.out.println(success.sourceAddress.toString() + "よりデータを受信しました:");
                System.out.println(decoded);
            } else if (result instanceof Result.Error) {
                Result.Error error = (Result.Error) result;
                System.out.println("受信タスクで例外が発生しました: " + error.error.getMessage());
            }
        });

        InetSocketAddress destinationAddress = new InetSocketAddress(Configuration.DESTINATION_IP, Configuration.DESTINATION_PORT);
        ExecutorService senderExecutorService = Executors.newSingleThreadExecutor();
        DataSenderService senderService = new DataSenderService(senderExecutorService, Configuration.SENDER_PORT, destinationAddress);

        boolean isRunning = true;
        Scanner scanner = new Scanner(System.in);

        while (isRunning) {
            String input = scanner.nextLine();
            InputType inputType = parseInputType(input);

            if (inputType instanceof InputType.SendMessage) {
                InputType.SendMessage sendMessage = (InputType.SendMessage) inputType;

                System.out.println("次のメッセージを送信します:");
                System.out.println(sendMessage.message);

                senderService.sendMessage(sendMessage.message, error -> {
                    if (error != null) {
                        System.out.println("メッセージ送信時に例外が発生しました: " + error.getMessage());
                        return;
                    }

                    System.out.println("メッセージを送信しました。");
                });
            } else if (inputType instanceof InputType.Command) {
                InputType.Command command = (InputType.Command) inputType;

                switch (command.type) {
                    case QUIT -> {
                        System.out.println("終了します。");
                        receiverService.endServiceImmediately();
                        receiverExecutorService.shutdown();
                        senderService.endServiceImmediately();
                        senderExecutorService.shutdown();
                        isRunning = false;
                    }
                    case START_SENDER_SERVICE -> {
                        try {
                            senderService.startService();
                        } catch (DataSenderService.DataSenderException e) {
                            System.out.println("送信サービスを開始できませんでした: " + e.getMessage());
                        }
                    }
                    case STOP_SENDER_SERVICE ->
                        senderService.endServiceImmediately();
                    case UNKNOWN ->
                        System.out.println("無効なコマンドです: " + input);
                }
            }
        }
    }

    private static InputType parseInputType(String input) {
        if (input.startsWith(InputType.Command.START_SYMBOL)) {
            String commandString = input.substring(InputType.Command.START_SYMBOL.length()).trim();

            for (InputType.Command.Type type: InputType.Command.Type.values()) {
                if (type.rawString.equals(commandString)) {
                    return new InputType.Command(type);
                }
            }
            return new InputType.Command(InputType.Command.Type.UNKNOWN);
        }
        if (input.trim().isEmpty()) {
            return InputType.None.INSTANCE;
        }

        return new InputType.SendMessage(input);
    }
}
