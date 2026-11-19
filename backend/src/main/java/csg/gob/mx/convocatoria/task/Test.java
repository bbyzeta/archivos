package csg.gob.mx.convocatoria.task;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class Test {
	public static void main(String[] args) {
        String textoPlano = "DFSMP006193";
        String hashGuardado = "$2y$10$JNbmYtdDVlj2wnFjKkZkS.B32CsRTSiSZhcirzDG4DMfvtc5V2dTO";

       
        boolean coincide = BCrypt.checkpw(textoPlano, hashGuardado);

        if (coincide) {
            System.out.println("¡La contraseña es correcta!");
        } else {
            System.out.println("Contraseña incorrecta.");
        }
    }
}
