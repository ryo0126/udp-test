# 概要

UDPを使って相互に短いメッセージを送り合うことができるコンソールアプリです。

# 使い方

* 送信側と受信側でアプリを起動します。
* 送信側で`/start-sender`というコマンドを入力してEnterキーを押下し、送信サービスを起動します。起動に成功したら`次のポートで送信サービスを開始しました: [ポート番号]`と出力されます。
* 送信側で任意の短い文字列を入力してEnterキーを押下します。受信側で正常に受信できたら`/[送信元IPアドレス]:[送信元ポート番号]よりデータを受信しました: [送信したメッセージ]`と出力されます。
* 終了する場合は`/quit`というコマンドを入力してEnterキーを押下します。

## コマンド一覧

* `/start-sender` - 送信サービスを開始します。
* `/stop-sender` - 送信サービスを終了します。
* `/quit` - アプリを終了します。

# 設定

`src/main/resources/configuration.properties`ファイルからサービスポートなどの設定が行えます。

## プロパティ一覧

* receiverPort - 受信サービスのポート番号を指定します。
* senderPort - 送信サービスのポート番号を指定します。
* destinationIp - 受信側のIPアドレスを指定します。
* destinationPort - 受信側の受信サービスポート番号を指定します。
