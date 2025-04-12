### Macbook M1报错：“Library not loaded: /usr/local/opt/openssl/lib/libssl.1.0.0.dylib”

1. 创建目录：`sudo mkdir -p /usr/local/opt/openssl/lib/`
2. 解压到目录：`sudo unzip -d /usr/local/opt/openssl/lib/ openssl1.0_x86_64.zip`
3. 授权：`sudo chmod -R 777 /usr/local/opt/openssl/`
4. 注意：如果提示安全性问题，需要在Mac的`隐私与安全性`上点击允许
5. MacOS开启“任何来源”：`sudo spctl --master-disable`
