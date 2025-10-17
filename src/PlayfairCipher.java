import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class PlayfairCipher {
    private char[][] matrix = new char[6][6];
    private Map<Character, Point> charPos = new HashMap<>();

    private static final char[] MATRIX_CHARS = {
            'A','B','C','Ç','D','E',
            'F','G','Ğ','H','I','İ',
            'J','K','L','M','N','O',
            'Ö','P','Q','R','S','Ş',
            'T','U','Ü','V','W','X',
            'Y','Z','.','!',',','?','-'
    };

    public PlayfairCipher(String key) {
        StringBuilder sb = new StringBuilder();
        key = key.toUpperCase().replaceAll("[^A-ZÇĞİÖŞÜ\\.\\!\\,\\?\\-]", "");
        for(char c : key.toCharArray()){
            if(sb.indexOf(""+c) == -1) sb.append(c);
        }
        for(char c : MATRIX_CHARS){
            if(sb.indexOf(""+c) == -1) sb.append(c);
        }

        for(int i=0;i<6;i++){
            for(int j=0;j<6;j++){
                char ch = sb.charAt(i*6+j);
                matrix[i][j] = ch;
                charPos.put(ch, new Point(i,j));
            }
        }
    }

    public String encrypt(String text){
        text = text.toUpperCase();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i < text.length()){
            char a = text.charAt(i);
            if(a == ' ' || !charPos.containsKey(a)){
                sb.append(a);
                i++;
                continue;
            }

            char b = (i+1 < text.length()) ? text.charAt(i+1) : 'X';
            if(b == a) b = 'X';

            if(!charPos.containsKey(b)){
                sb.append(a);
                i++;
                continue;
            }

            Point pa = charPos.get(a);
            Point pb = charPos.get(b);

            if(pa.x == pb.x){
                sb.append(matrix[pa.x][(pa.y+1)%6]);
                sb.append(matrix[pb.x][(pb.y+1)%6]);
            } else if(pa.y == pb.y){
                sb.append(matrix[(pa.x+1)%6][pa.y]);
                sb.append(matrix[(pb.x+1)%6][pb.y]);
            } else {
                sb.append(matrix[pa.x][pb.y]);
                sb.append(matrix[pb.x][pa.y]);
            }
            i += (a==b)?1:2;
        }
        return sb.toString();
    }

    public String decrypt(String text){
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i < text.length()){
            char a = text.charAt(i);
            char b = (i+1 < text.length()) ? text.charAt(i+1) : 'X';
            if(!charPos.containsKey(a) || !charPos.containsKey(b)){
                sb.append(a);
                i++;
                continue;
            }

            Point pa = charPos.get(a);
            Point pb = charPos.get(b);

            char first, second;
            if(pa.x == pb.x){
                first = matrix[pa.x][(pa.y+5)%6];
                second = matrix[pb.x][(pb.y+5)%6];
            } else if(pa.y == pb.y){
                first = matrix[(pa.x+5)%6][pa.y];
                second = matrix[(pb.x+5)%6][pb.y];
            } else {
                first = matrix[pa.x][pb.y];
                second = matrix[pb.x][pa.y];
            }

            sb.append(first);
            sb.append(second);
            i += 2;
        }
        return sb.toString();
    }
}
