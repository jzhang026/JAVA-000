package co.adrian;

public class Main {

    public static void main(String[] args) {
        MyHttpClient client = new MyHttpClient();
        String res = client.get("https://www.baidu.com");
	// write your code here
        System.out.println(res);
    }
}
