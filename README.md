# ez-play

1、至 https://www.playframework.com 下載play framework

2、下載ez-play後，先執行以下command再匯入eclipse

$ cd /your-project-path/ez-play

[2.3.X]	ez-play $ activator clean update compile eclipse

[2.2.X]	ez-play $ play      clean update compile eclipse

3、註冊twitter帳號，於下列位置輸入相關資訊

app > util > Util.java

app > controller > IndexController.java

4、執行 $ activator run 後，於瀏覽器輸入http://localhost:9000
