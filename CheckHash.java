import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class CheckHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.matches("Admin@1234", "$2a$12$LJ3m4ks0Y5sQNOBMGFGWI.xJ2BGOO/5R3N3KsLRRWxYb8nnKHnT9G"));
    }
}
